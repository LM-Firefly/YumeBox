package com.github.yumelira.yumebox.clash.manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import com.github.yumelira.yumebox.clash.config.Configuration
import com.github.yumelira.yumebox.clash.config.RouteConfig
import com.github.yumelira.yumebox.clash.core.ClashCore
import com.github.yumelira.yumebox.clash.exception.toConfigImportException
import com.github.yumelira.yumebox.common.util.SystemProxyHelper
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.core.model.ProxySort
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.model.ProfileType
import com.github.yumelira.yumebox.data.repository.ProxyChainResolver
import com.github.yumelira.yumebox.data.repository.ProxyStateRepository
import com.github.yumelira.yumebox.data.repository.SelectionDao
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.domain.model.ProxyState
import com.github.yumelira.yumebox.domain.model.RunningMode
import com.github.yumelira.yumebox.domain.model.TrafficData
import dev.oom_wg.purejoy.mlang.MLang
import java.io.Closeable
import java.io.File
import java.net.InetSocketAddress
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import timber.log.Timber

class ClashManager(
    private val context: Context,
    private val workDir: File,
    private val proxyModeProvider: (() -> TunnelState.Mode)? = null
) : Closeable {
    companion object {
        private const val TAG = "ClashManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val selectionDao = SelectionDao(context)

    private val currentProfileId = java.util.concurrent.atomic.AtomicReference<String>(null)

    val proxyStateRepository = ProxyStateRepository(
        context = context,
        proxyChainResolver = ProxyChainResolver(),
        profileIdProvider = { currentProfileId.get() }
    )

    private val _proxyState = MutableStateFlow<ProxyState>(ProxyState.Idle)
    val proxyState: StateFlow<ProxyState> = _proxyState.asStateFlow()

    val isRunning: StateFlow<Boolean> = _proxyState.map { it.isRunning }.stateIn(scope, SharingStarted.Eagerly, false)

    val runningMode: StateFlow<RunningMode> = _proxyState.map { state ->
        when (state) {
            is ProxyState.Running -> state.mode
            is ProxyState.Connecting -> state.mode
            else -> RunningMode.None
        }
    }.stateIn(scope, SharingStarted.Eagerly, RunningMode.None)

    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    public val screenStateFlow = kotlinx.coroutines.flow.callbackFlow {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val isScreenOn = intent.action == Intent.ACTION_SCREEN_ON
                trySend(isScreenOn)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        trySend(powerManager.isInteractive)
        awaitClose { 
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                Timber.w(e, "Failed to unregister screen state receiver")
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, true)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val sharedTrafficFlow = screenStateFlow.transformLatest { isScreenOn ->
        if (isScreenOn) {
            while (true) {
                if (_proxyState.value is ProxyState.Running) {
                    try {
                        val now = TrafficData.from(ClashCore.queryTrafficNow())
                        val total = TrafficData.from(ClashCore.queryTrafficTotal())
                        emit(now to total)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to query traffic data")
                        emit(TrafficData.ZERO to TrafficData.ZERO)
                    }
                } else {
                    emit(TrafficData.ZERO to TrafficData.ZERO)
                }
                delay(1000L)
            }
        }
    }.flowOn(Dispatchers.IO)
        .shareIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 1000), replay = 1)

    fun createTrafficFlow() = sharedTrafficFlow

    val trafficNow: StateFlow<TrafficData> = sharedTrafficFlow.map { it.first }
        .stateIn(scope, SharingStarted.WhileSubscribed(1000), TrafficData.ZERO)

    val trafficTotal: StateFlow<TrafficData> = sharedTrafficFlow.map { it.second }
        .stateIn(scope, SharingStarted.WhileSubscribed(1000), TrafficData.ZERO)

    suspend fun queryTunnelState(): TunnelState? = withContext(Dispatchers.IO) {
        if (_proxyState.value is ProxyState.Running) {
            runCatching { ClashCore.queryTunnelState() }.getOrNull()
        } else {
            null
        }
    }

    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = proxyStateRepository.proxyGroups

    val logs: SharedFlow<LogMessage> = flow {
        val channel = ClashCore.subscribeLogcat()
        try {
            for (log in channel) {
                if (!log.message.contains("Request interrupted by user") && !log.message.contains(MLang.Proxy.PullToRefresh.DelaySuccess)) {
                    emit(log)
                }
            }
        } finally {
            ClashCore.unsubscribeLogcat()
        }
    }.flowOn(Dispatchers.IO).shareIn(scope, SharingStarted.WhileSubscribed(5000), replay = 100)

    init {
        workDir.mkdirs()
    }

    suspend fun loadProfile(profile: Profile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val configDir = getConfigDir(profile)
            if (!configDir.exists() || !configDir.isDirectory) {
                return@withContext Result.failure(IllegalStateException(MLang.Service.Message.ConfigDirMissing.format(profile.name)))
            }

            val loadResult = ClashCore.loadConfig(
                configDir = configDir, options = ClashCore.LoadOptions(
                    timeoutMs = 30_000L, resetBeforeLoad = true, clearSessionOverride = true
                )
            )

            if (loadResult.isFailure) {
                val error = loadResult.exceptionOrNull()
                Timber.e(error, MLang.Service.Status.ConfigLoadFailedWithProfile.format(profile.name))
                return@withContext loadResult
            }

            _currentProfile.value = profile
            currentProfileId.set(profile.id)

            runCatching {
                val persistOverride = ClashCore.queryOverride(Clash.OverrideSlot.Persist)
                ClashCore.patchOverride(Clash.OverrideSlot.Session, persistOverride)
                Timber.tag(TAG).d("Reapplied Persist override to Session after load: externalController=%s", persistOverride.externalController)
            }.onFailure { e ->
                Timber.w(e, "Failed to reapply Persist override to Session after load")
            }

            proxyModeProvider?.let { provider ->
                runCatching {
                    val mode = provider()
                    val persist = ClashCore.queryOverride(Clash.OverrideSlot.Persist)
                    if (persist.mode != mode) {
                        persist.mode = mode
                        ClashCore.patchOverride(Clash.OverrideSlot.Persist, persist)
                    }
                    val session = ClashCore.queryOverride(Clash.OverrideSlot.Session)
                    session.mode = mode
                    ClashCore.patchOverride(Clash.OverrideSlot.Session, session)
                }.onFailure { e ->
                    Timber.e(e, MLang.Service.Status.UnknownError)
                }
            }

            // 先恢复存储的选择
            val selections = selectionDao.getAllSelections(profile.id)

            // 获取当前组列表（不需要同步，内核刚加载）
            val groupNames = Clash.queryGroupNames(excludeNotSelectable = false)
            val groupsMap = groupNames.associateWith { name ->
                runCatching { Clash.queryGroup(name, ProxySort.Default) }.getOrNull()
            }

            // 恢复选择
            selections.forEach { (groupName, proxyName) ->
                val group = groupsMap[groupName]
                val proxy = group?.proxies?.find { it.name == proxyName }

                if (group != null && proxy != null && group.type == Proxy.Type.Selector) {
                    runCatching { ClashCore.selectProxy(groupName, proxyName) }
                }
            }

            // 开始同步
            proxyStateRepository.start()

            scope.launch {
                try {
                    val selections = selectionDao.getAllSelections(profile.id)
                    selections.forEach { (groupName, proxyName) ->
                        runCatching {
                            ClashCore.selectProxy(groupName, proxyName)
                        }.onFailure { e ->
                            Timber.e(e, MLang.Service.Status.UnknownError + ": $groupName -> $proxyName")
                        }
                    }
                    runCatching { proxyStateRepository.restoreSelections(profile.id) }
                        .onFailure { Timber.w(it, "Restore pinned selections failed") }
                    proxyStateRepository.syncFromCore()
                } catch (e: Exception) {
                    Timber.e(e, MLang.Service.Status.UnknownError)
                }
            }
            scope.launch {
                runCatching { proxyStateRepository.syncFromCore() }
                    .onFailure { Timber.w(it, "Background sync failed") }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            val importException = e.toConfigImportException()
            Timber.e(importException, MLang.Service.Status.ConfigLoadFailedWithProfile.format(profile.name))
            Result.failure(importException)
        }
    }

    private fun getConfigDir(profile: Profile): File {
        return when (profile.type) {
            ProfileType.FILE -> {
                val configFile = File(profile.config)
                configFile.parentFile ?: workDir
            }

            ProfileType.URL -> {
                val importedDir = workDir.parentFile?.resolve("imported") ?: File(workDir, "imported")
                File(importedDir, profile.id)
            }
        }
    }

    suspend fun startTun(
        fd: Int,
        config: Configuration.TunConfig = Configuration.TunConfig(),
        enableIPv6: Boolean = false,
        markSocket: (Int) -> Boolean,
        querySocketUid: (protocol: Int, source: InetSocketAddress, target: InetSocketAddress) -> Int = { _, _, _ -> -1 }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val profile =
                _currentProfile.value ?: return@withContext Result.failure(IllegalStateException(MLang.Proxy.Message.LoadProfileFirst))

            _proxyState.value = ProxyState.Connecting(RunningMode.Tun)

            val gateway = buildString {
                append("${config.gateway}/30")
                if (enableIPv6) {
                    append(",${RouteConfig.TUN_GATEWAY6}/${RouteConfig.TUN_SUBNET_PREFIX6}")
                }
            }

            val portal = buildString {
                append(config.portal)
                if (enableIPv6) {
                    append(",${RouteConfig.TUN_PORTAL6}")
                }
            }

            val dns = buildString {
                if (config.dnsHijacking) {
                    if (enableIPv6) {
                        append("0.0.0.0")
                    } else {
                        // IPv6 关闭时，不劫持 DNS，让系统 DNS 处理（避免 AAAA 查询问题）
                        append(config.dns)
                    }
                } else {
                    append(config.dns)
                    if (enableIPv6) {
                        append(",${RouteConfig.TUN_DNS6}")
                    }
                }
            }

            ClashCore.startTun(
                fd = fd,
                stack = config.stack,
                gateway = gateway,
                portal = portal,
                dns = dns,
                markSocket = markSocket,
                querySocketUid = querySocketUid
            )

            _proxyState.value = ProxyState.Running(profile, RunningMode.Tun)

            Result.success(Unit)
        } catch (e: Exception) {
            val importException = e.toConfigImportException()
            Timber.e(importException, MLang.Service.Status.TunStartFailed.format(importException.message ?: ""))
            _proxyState.value = ProxyState.Error(MLang.Service.Status.TunStartFailed.format(importException.message ?: ""), importException)
            Result.failure(importException)
        }
    }

    suspend fun startHttp(
        config: Configuration.HttpConfig = Configuration.HttpConfig()
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val profile =
                _currentProfile.value ?: return@withContext Result.failure(IllegalStateException(MLang.Proxy.Message.LoadProfileFirst))

            _proxyState.value = ProxyState.Connecting(RunningMode.Http(config.address))

            val address = ClashCore.startHttp(config.listenAddress) ?: config.address

            _proxyState.value = ProxyState.Running(profile, RunningMode.Http(address))

            Result.success(address)
        } catch (e: Exception) {
            val importException = e.toConfigImportException()
            Timber.e(importException, MLang.Service.Status.HttpStartFailed.format(importException.message ?: ""))
            _proxyState.value = ProxyState.Error(MLang.Service.Status.HttpStartFailed.format(importException.message ?: ""), importException)
            Result.failure(importException)
        }
    }

    fun stop() {
        runCatching { _proxyState.value = ProxyState.Stopping }
        runCatching { ClashCore.stopTun() }
        runCatching { ClashCore.stopHttp() }
        runCatching { SystemProxyHelper.clearSystemProxy(context) }

        runCatching {
            proxyStateRepository.stop()
            resetState()
        }.onFailure {
            runCatching { ClashCore.reset() }
            proxyStateRepository.stop()
            resetState()
        }
    }

    private fun resetState() {
        _proxyState.value = ProxyState.Idle
        _currentProfile.value = null
        currentProfileId.set(null)
    }

    suspend fun reloadCurrentProfile(): Result<Unit> = withContext(Dispatchers.IO) {
        val profile = _currentProfile.value
            ?: return@withContext Result.failure(IllegalStateException(MLang.Proxy.Message.LoadProfileFirst))
        loadProfile(profile)
    }

    suspend fun refreshProxyGroups(): Result<Unit> {
        return proxyStateRepository.syncFromCore()
    }

    fun setProxyScreenActive(active: Boolean) {
        if (active) {
            proxyStateRepository.start()
        } else {
            proxyStateRepository.stop()
        }
    }

    suspend fun selectProxy(groupName: String, proxyName: String): Boolean {
        val res = proxyStateRepository.selectProxy(groupName, proxyName, _currentProfile.value)
        return res.getOrNull() ?: false
    }

    suspend fun forceSelectProxy(groupName: String, proxyName: String): Boolean =
        proxyStateRepository.forceSelectProxy(groupName, proxyName, _currentProfile.value)

    suspend fun healthCheck(groupName: String): Result<Unit> {
        return proxyStateRepository.testGroupDelay(groupName)
    }

    suspend fun healthCheckAll(): Result<Unit> {
        return proxyStateRepository.testAllDelay()
    }

    override fun close() {
        proxyStateRepository.close()
        scope.cancel("ClashManager closed")
        resetState()
    }
}
