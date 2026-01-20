package com.github.yumelira.yumebox.clash.manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import com.github.yumelira.yumebox.clash.config.Configuration
import com.github.yumelira.yumebox.clash.config.RouteConfig
import com.github.yumelira.yumebox.clash.exception.ConfigValidationException
import com.github.yumelira.yumebox.clash.exception.FileAccessException
import com.github.yumelira.yumebox.clash.exception.TimeoutException
import com.github.yumelira.yumebox.clash.exception.toConfigImportException
import com.github.yumelira.yumebox.common.util.SystemProxyHelper
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.bridge.ClashException
import com.github.yumelira.yumebox.core.model.ConfigurationOverride
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.core.model.ProxySort
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.model.ProfileType
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
        private val SUSPICIOUS_TYPE_LOGGED = java.util.concurrent.atomic.AtomicBoolean(false)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val selectionDao = SelectionDao(context)

    private val currentProfileId = java.util.concurrent.atomic.AtomicReference<String>(null)

    val proxyStateRepository = ProxyStateRepository(
        context = context,
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
    private val _trafficNow = MutableStateFlow(TrafficData.ZERO)
    val trafficNow: StateFlow<TrafficData> = _trafficNow.asStateFlow()
    private val _trafficTotal = MutableStateFlow(TrafficData.ZERO)
    val trafficTotal: StateFlow<TrafficData> = _trafficTotal.asStateFlow()
    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = proxyStateRepository.proxyGroups
    private val _logs = MutableSharedFlow<LogMessage>(replay = 100)
    val logs: SharedFlow<LogMessage> = flow {
        val channel = Clash.subscribeLogcat()
        try {
            for (log in channel) {
                if (!log.message.contains("Request interrupted by user") && !log.message.contains(MLang.Proxy.PullToRefresh.DelaySuccess)) {
                    emit(log)
                }
            }
        } finally {
            Clash.unsubscribeLogcat()
        }
    }.flowOn(Dispatchers.IO).shareIn(scope, SharingStarted.WhileSubscribed(5000), replay = 100)
    private val _isAppForeground = MutableStateFlow(false)
    val isAppForeground: StateFlow<Boolean> = _isAppForeground.asStateFlow()
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
    private val isUiVisible = combine(screenStateFlow, _isAppForeground) { isScreenOn, isForeground ->
        isScreenOn && isForeground
    }.stateIn(scope, SharingStarted.Eagerly, false)
    private val _trafficMonitorRequested = MutableStateFlow(false)
    fun setTrafficMonitorRequested(requested: Boolean) {
        _trafficMonitorRequested.value = requested
    }
    fun createTrafficFlow() = combine(_trafficNow, _trafficTotal) { now, total -> now to total }
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val subscriptionConnectionsFlow = isUiVisible
        .transformLatest { isActive ->
        if (isActive) {
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            emitAll(channelFlow {
                // emit initial snapshot (best-effort)
                try {
                    val initial = try {
                        val s = com.github.yumelira.yumebox.core.Clash.queryConnectionsJson()
                        if (s.isNotBlank()) json.decodeFromString(com.github.yumelira.yumebox.domain.model.ConnectionsSnapshot.serializer(), s) else null
                    } catch (t: Throwable) {
                        null
                    }
                    if (initial != null) {
                        Timber.tag(TAG).d("Subscription initial snapshot size=%d", initial.connections?.size ?: 0)
                        trySend(initial)
                    } else {
                        Timber.tag(TAG).d("Subscription initial snapshot empty")
                    }
                } catch (t: Throwable) {
                    Timber.e(t, "Failed to emit initial connections snapshot")
                }
                // subscribe to native events (receive JSON strings from core and decode)
                val ch = com.github.yumelira.yumebox.core.Clash.subscribeConnections()
                try {
                    for (raw in ch) {
                        try {
                            if (raw.isBlank()) {
                                Timber.tag(TAG).v("Subscription raw payload blank, skipping")
                                continue
                            }
                            val snap = json.decodeFromString(com.github.yumelira.yumebox.domain.model.ConnectionsSnapshot.serializer(), raw)
                            Timber.tag(TAG).v("Subscription payload parsed, snapshot size=%d", snap.connections?.size ?: 0)
                            trySend(snap)
                        } catch (t: Throwable) {
                            Timber.e(t, "Error parsing connections snapshot: %s", raw.take(500))
                        }
                    }
                } catch (t: Throwable) {
                    Timber.e(t, "Error receiving subscription connections")
                } finally {
                    com.github.yumelira.yumebox.core.Clash.unsubscribeConnections()
                }
                awaitClose {
                    // ensure unsubscribe if collector cancelled
                    try {
                        com.github.yumelira.yumebox.core.Clash.unsubscribeConnections()
                    } catch (t: Throwable) { /* ignore */ }
                }
            })
        }
    }.flowOn(Dispatchers.IO)
        .shareIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 1000), replay = 1)
    val connections: SharedFlow<com.github.yumelira.yumebox.domain.model.ConnectionsSnapshot> = run {
        val jsonEnc = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val previousMap = java.util.concurrent.ConcurrentHashMap<String, com.github.yumelira.yumebox.domain.model.Connection>()
        subscriptionConnectionsFlow.map { snapshot ->
            val rawConnections = snapshot.connections ?: emptyList()
            val sanitized = rawConnections.map { conn ->
                conn.copy(
                    id = conn.id.ifBlank { "<unknown-id>" },
                    chains = conn.chains.map { it.ifBlank { "<unknown>" } },
                    rule = conn.rule.ifBlank { "<unknown-rule>" },
                    rulePayload = conn.rulePayload.ifBlank { "" },
                    start = conn.start.ifBlank { "" },
                    metadata = conn.metadata.copy(
                        network = conn.metadata.network.ifBlank { "unknown" },
                        type = conn.metadata.type.ifBlank { "unknown" },
                        sourceIP = conn.metadata.sourceIP.ifBlank { "" },
                        sourcePort = conn.metadata.sourcePort.ifBlank { "" },
                        destinationIP = conn.metadata.destinationIP.ifBlank { "" },
                        destinationPort = conn.metadata.destinationPort.ifBlank { "" },
                        host = conn.metadata.host.ifBlank { "" },
                        dnsMode = conn.metadata.dnsMode.ifBlank { "" },
                        process = conn.metadata.process.ifBlank { "" },
                        processPath = conn.metadata.processPath.ifBlank { "" }
                    )
                )
            }.take(200)
            val currentMap = java.util.HashMap<String, com.github.yumelira.yumebox.domain.model.Connection>()
            sanitized.forEach { s ->
                val prev = previousMap[s.id]
                if (prev != null) {
                    s.downloadSpeed = s.download - prev.download
                    s.uploadSpeed = s.upload - prev.upload
                }
                currentMap[s.id] = s
            }
            previousMap.clear()
            previousMap.putAll(currentMap)
            val result = com.github.yumelira.yumebox.domain.model.ConnectionsSnapshot(sanitized, snapshot.downloadTotal, snapshot.uploadTotal)
            val suspiciousFound = sanitized.any { it.metadata.type.matches(Regex("^[no]\\d+$")) }
            if (suspiciousFound && SUSPICIOUS_TYPE_LOGGED.compareAndSet(false, true)) {
                Timber.tag(TAG).w("Suspicious metadata.type detected in connections; dumping snapshot")
                try {
                    Timber.tag(TAG).d(jsonEnc.encodeToString(com.github.yumelira.yumebox.domain.model.ConnectionsSnapshot.serializer(), result))
                } catch (t: Throwable) { /* ignore */ }
            }
            result
        }
        .distinctUntilChangedBy { jsonEnc.encodeToString(com.github.yumelira.yumebox.domain.model.ConnectionsSnapshot.serializer(), it) }
        .flowOn(Dispatchers.IO)
        .shareIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 1000), replay = 1)
    }

    private var monitorJob: Job? = null
    private var logJob: Job? = null
    suspend fun queryTunnelState(): TunnelState? = withContext(Dispatchers.IO) {
        if (_proxyState.value is ProxyState.Running) {
            runCatching { Clash.queryTunnelState() }.getOrNull()
        } else {
            null
        }
    }
    init {
        workDir.mkdirs()
        scope.launch {
            _proxyState.map { it.isRunning }
                .distinctUntilChanged()
                .collect { running ->
                    if (running) startMonitor() else stopMonitor()
                }
        }
        scope.launch {
            combine(_currentProfile, isUiVisible) { profile, visible ->
                profile != null && visible
            }.distinctUntilChanged().collectLatest { shouldSync ->
                if (shouldSync) {
                    launch { 
                        runCatching { proxyStateRepository.syncOnce() }
                            .onFailure { Timber.w(it, "Auto-sync immediate failed") }
                    }
                    proxyStateRepository.start()
                } else {
                    proxyStateRepository.stop()
                }
            }
        }
        (context.applicationContext as? android.app.Application)?.registerActivityLifecycleCallbacks(object : android.app.Application.ActivityLifecycleCallbacks {
            private var startedCount = 0
            override fun onActivityStarted(activity: android.app.Activity) {
                startedCount++
                _isAppForeground.value = startedCount > 0
            }
            override fun onActivityStopped(activity: android.app.Activity) {
                startedCount--
                _isAppForeground.value = startedCount > 0
            }
            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
            override fun onActivityResumed(activity: android.app.Activity) {}
            override fun onActivityPaused(activity: android.app.Activity) {}
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {}
        })
    }

    suspend fun loadProfile(profile: Profile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val configDir = getConfigDir(profile)
            if (!configDir.exists() || !configDir.isDirectory) {
                return@withContext Result.failure(IllegalStateException("配置目录不存在: ${profile.name}"))
            }

            val loadResult = loadConfig(
                configDir = configDir,
                timeoutMs = 60_000L,
                resetBeforeLoad = true,
                clearSessionOverride = true
            )

            if (loadResult.isFailure) {
                val error = loadResult.exceptionOrNull()
                Timber.e(error, MLang.Service.Status.ConfigLoadFailedWithProfile.format(profile.name))
                return@withContext loadResult
            }

            _currentProfile.value = profile
            currentProfileId.set(profile.id)

            runCatching {
                val persistOverride = Clash.queryOverride(Clash.OverrideSlot.Persist)
                Clash.patchOverride(Clash.OverrideSlot.Session, persistOverride)
                Timber.tag(TAG).d("Reapplied Persist override to Session after load: externalController=%s", persistOverride.externalController)
            }.onFailure { e ->
                Timber.w(e, "Failed to reapply Persist override to Session after load")
            }
            proxyModeProvider?.let { provider ->
                runCatching {
                    val mode = provider()
                    val persist = safeQueryOverride(Clash.OverrideSlot.Persist)
                    if (persist.mode != mode) {
                        persist.mode = mode
                        Clash.patchOverride(Clash.OverrideSlot.Persist, persist)
                    }
                    val session = safeQueryOverride(Clash.OverrideSlot.Session)
                    session.mode = mode
                    Clash.patchOverride(Clash.OverrideSlot.Session, session)
                }
            }

            // 恢复选择状态
            runCatching {
                proxyStateRepository.restoreSelections(profile.id) 
            }.onFailure {
                Timber.w(it, "Restore selections failed") 
            }

            // 触发一次初始同步以更新 UI
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
                _currentProfile.value ?: return@withContext Result.failure(IllegalStateException("请先加载配置"))

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

            Clash.startTun(
                fd = fd,
                stack = config.stack,
                gateway = gateway,
                portal = portal,
                dns = dns,
                markSocket = markSocket,
                querySocketUid = querySocketUid
            )

            _proxyState.value = ProxyState.Running(profile, RunningMode.Tun)
            startMonitor()

            Result.success(Unit)
        } catch (e: Exception) {
            val importException = e.toConfigImportException()
            Timber.e(importException, MLang.Service.Status.TunStartFailed.format(importException.message ?: ""))
            _proxyState.value = ProxyState.Error("TUN启动失败: ${importException.message}", importException)
            Result.failure(importException)
        }
    }

    suspend fun startHttp(
        config: Configuration.HttpConfig = Configuration.HttpConfig()
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val profile =
                _currentProfile.value ?: return@withContext Result.failure(IllegalStateException("请先加载配置"))

            _proxyState.value = ProxyState.Connecting(RunningMode.Http(config.address))

            val address = Clash.startHttp(config.listenAddress) ?: config.address

            _proxyState.value = ProxyState.Running(profile, RunningMode.Http(address))
            startMonitor()

            Result.success(address)
        } catch (e: Exception) {
            val importException = e.toConfigImportException()
            Timber.e(importException, MLang.Service.Status.HttpStartFailed.format(importException.message ?: ""))
            _proxyState.value = ProxyState.Error("HTTP启动失败: ${importException.message}", importException)
            Result.failure(importException)
        }
    }

    fun stop() {
        runCatching { _proxyState.value = ProxyState.Stopping }
        runCatching { Clash.stopTun() }
        runCatching { Clash.stopHttp() }
        runCatching { SystemProxyHelper.clearSystemProxy(context) }

        runCatching {
            proxyStateRepository.stop()
            stopMonitor()
            resetState()
        }.onFailure {
            runCatching { Clash.reset() }
            proxyStateRepository.stop()
            stopMonitor()
            resetState()
        }
    }

    private fun startMonitor() {
        stopMonitor()

        monitorJob = scope.launch {
            combine(isUiVisible, _trafficMonitorRequested, screenStateFlow) { uiVisible, requested, screenOn ->
                uiVisible || (requested && screenOn)
            }.collectLatest { shouldRun ->
                if (shouldRun) {
                    while (isActive) {
                        runCatching {
                            _trafficNow.value = TrafficData.from(runCatching { Clash.queryTrafficNow() }.getOrDefault(0L))
                            _trafficTotal.value = TrafficData.from(runCatching { Clash.queryTrafficTotal() }.getOrDefault(0L))
                        }
                        delay(1000)
                    }
                }
            }
        }
    }

    private fun stopMonitor() {
        monitorJob?.cancel()
        monitorJob = null
    }

    private fun startLogSubscription() {
        logJob = scope.launch {
            val channel = Clash.subscribeLogcat()
            for (log in channel) {
                if (!log.message.contains("Request interrupted by user") && !log.message.contains("更新延迟")) {
                    _logs.emit(log)
                }
            }
        }
    }

    private fun resetState() {
        _proxyState.value = ProxyState.Idle
        _currentProfile.value = null
        currentProfileId.set(null)
        _trafficNow.value = TrafficData.ZERO
        _trafficTotal.value = TrafficData.ZERO
    }

    suspend fun reloadCurrentProfile(): Result<Unit> = withContext(Dispatchers.IO) {
        val profile = _currentProfile.value
            ?: return@withContext Result.failure(IllegalStateException("没有加载的配置"))
        loadProfile(profile)
    }

    suspend fun refreshProxyGroups(): Result<Unit> {
        return proxyStateRepository.syncFromCore()
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

    private suspend fun loadConfig(
        configDir: File,
        timeoutMs: Long,
        resetBeforeLoad: Boolean,
        clearSessionOverride: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!configDir.exists() || !configDir.isDirectory) {
                return@withContext Result.failure(
                    FileAccessException(
                        "配置目录不存在或不是目录",
                        filePath = configDir.absolutePath,
                        reason = FileAccessException.Reason.NOT_FOUND
                    )
                )
            }

            val configFile = File(configDir, "config.yaml")
            if (!configFile.exists()) {
                return@withContext Result.failure(
                    FileAccessException(
                        "配置文件不存在",
                        filePath = configFile.absolutePath,
                        reason = FileAccessException.Reason.NOT_FOUND
                    )
                )
            }

            if (resetBeforeLoad) {
                Clash.reset()
            }

            if (clearSessionOverride) {
                Clash.clearOverride(Clash.OverrideSlot.Session)
            }

            val deferred = Clash.load(configDir)
            try {
                withTimeout(timeoutMs) {
                    deferred.await()
                }
            } finally {
                if (deferred.isActive) {
                    deferred.cancel()
                }
            }

            Result.success(Unit)
        } catch (e: TimeoutCancellationException) {
            Result.failure(
                TimeoutException(
                    "配置加载超时",
                    e,
                    timeoutMs = timeoutMs,
                    operation = "加载配置"
                )
            )
        } catch (e: ClashException) {
            Result.failure(
                ConfigValidationException(
                    e.message ?: "配置加载失败",
                    e
                )
            )
        } catch (e: Exception) {
            Result.failure(e.toConfigImportException())
        }
    }

    private fun safeQueryOverride(slot: Clash.OverrideSlot): ConfigurationOverride {
        return runCatching { Clash.queryOverride(slot) }
            .getOrElse { ConfigurationOverride() }
    }
}
