/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c)  YumeLira 2025 - Present
 *
 */



package com.github.yumelira.yumebox.runtime.client

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Build
import com.github.yumelira.yumebox.core.model.ProxyGroup
import com.github.yumelira.yumebox.core.model.ProxySort
import com.github.yumelira.yumebox.core.model.Traffic
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.data.store.MMKVProvider
import com.github.yumelira.yumebox.data.store.NetworkSettingsStorage
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.remote.ServiceClient
import com.github.yumelira.yumebox.remote.VpnPermissionRequired
import com.github.yumelira.yumebox.runtime.client.root.RootTunController
import com.github.yumelira.yumebox.service.ClashService
import com.github.yumelira.yumebox.service.StatusProvider
import com.github.yumelira.yumebox.service.TunService
import com.github.yumelira.yumebox.service.common.constants.Intents
import com.github.yumelira.yumebox.service.common.util.appContextOrSelf
import com.github.yumelira.yumebox.service.root.RootTunState
import com.github.yumelira.yumebox.service.root.RootTunStateStore
import com.github.yumelira.yumebox.service.root.RootTunStatus
import com.github.yumelira.yumebox.service.runtime.entity.Profile
import com.github.yumelira.yumebox.service.runtime.session.RuntimeServiceLauncher
import com.github.yumelira.yumebox.service.runtime.state.RuntimeOwner
import com.github.yumelira.yumebox.service.runtime.state.RuntimePhase
import com.github.yumelira.yumebox.service.runtime.state.RuntimeSnapshot
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

class ProxyFacade(
    private val context: Context,
) {
    private data class PreviewCacheKey(
        val profileId: UUID,
        val profileUpdatedAt: Long,
        val excludeNotSelectable: Boolean,
        val overrideSignature: String,
    )

    private data class PreviewCacheEntry(
        val key: PreviewCacheKey,
        val groups: List<ProxyGroupInfo>,
    )

    private val appContext: Context = context.appContextOrSelf
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val networkSettingsStorage by lazy {
        NetworkSettingsStorage(MMKVProvider().getMMKV("network_settings"))
    }
    private val rootTunStateStore by lazy { RootTunStateStore(appContext) }
    private val _rootTunStatus = MutableStateFlow(rootTunStateStore.snapshot())
    val rootTunStatus: StateFlow<RootTunStatus> = _rootTunStatus.asStateFlow()
    private val _runtimeSnapshot = MutableStateFlow(
        RuntimeStateMapper.idleSnapshot(networkSettingsStorage.proxyMode.value),
    )
    val runtimeSnapshot: StateFlow<RuntimeSnapshot> = _runtimeSnapshot.asStateFlow()

    private val actionServiceRecreated: String get() = Intents.actionServiceRecreated(appContext.packageName)
    private val actionClashStarted: String get() = Intents.actionClashStarted(appContext.packageName)
    private val actionClashStopped: String get() = Intents.actionClashStopped(appContext.packageName)
    private val actionClashRequestStop: String get() = Intents.actionClashRequestStop(appContext.packageName)
    private val actionProfileChanged: String get() = Intents.actionProfileChanged(appContext.packageName)
    private val actionProfileLoaded: String get() = Intents.actionProfileLoaded(appContext.packageName)
    private val actionOverrideChanged: String get() = Intents.actionOverrideChanged(appContext.packageName)
    private val actionRootRuntimeFailed: String get() = Intents.actionRootRuntimeFailed(appContext.packageName)

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _proxyGroups = MutableStateFlow<List<ProxyGroupInfo>>(emptyList())
    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = _proxyGroups.asStateFlow()

    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    private val _trafficNow = MutableStateFlow(0L)
    val trafficNow: StateFlow<Traffic> = _trafficNow.asStateFlow()

    private val _trafficTotal = MutableStateFlow(0L)
    val trafficTotal: StateFlow<Traffic> = _trafficTotal.asStateFlow()

    private var trafficPollingJob: Job? = null
    private var previewCache: PreviewCacheEntry? = null
    private var previewWarmupJob: Job? = null
    private val refreshProxyGroupsMutex = Mutex()
    private val operationMutex = Mutex()
    private var generationCounter = 0L

    private val serviceEventsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action ?: return) {
                actionClashStarted -> {
                    scope.launch { handleRuntimeStarted() }
                }

                actionClashStopped -> {
                    scope.launch { handleRuntimeStopped(intent.getStringExtra(Intents.EXTRA_STOP_REASON)) }
                }

                actionProfileLoaded,
                actionProfileChanged,
                actionOverrideChanged,
                actionServiceRecreated -> {
                    scope.launch {
                        if (_runtimeSnapshot.value.phase == RuntimePhase.Running) {
                            refreshAllSafely()
                        } else {
                            refreshPreviewStateSafely()
                        }
                    }
                }

                actionRootRuntimeFailed -> {
                    val error = intent.getStringExtra("error")
                    Timber.w("Root runtime failed: $error")
                    scope.launch { handleRuntimeFailure(error) }
                }
            }
        }
    }

    init {
        registerServiceEventReceiver()
        initializeRuntimeSnapshot()
    }

    fun warmUpProxyGroups() {
        if (previewWarmupJob?.isActive == true) return
        previewWarmupJob = scope.launch {
            runCatching { refreshProxyGroups() }
                .onFailure { error -> Timber.d(error, "Warm up proxy groups skipped") }
        }
    }

    suspend fun startProxy(mode: ProxyMode = networkSettingsStorage.proxyMode.value) {
        Timber.i("Start proxy: mode=$mode")
        ServiceClient.connect(appContext)

        val activeProfile = ServiceClient.profile().queryActive()
        check(activeProfile != null) { "No profile selected" }

        if (mode == ProxyMode.Tun) {
            val vpnIntent = VpnService.prepare(context)
            if (vpnIntent != null) {
                throw VpnPermissionRequired(vpnIntent)
            }
        }

        operationMutex.withLock {
            val targetOwner = ownerForMode(mode)
            val currentOwner = detectActiveOwner().takeIf { it != RuntimeOwner.None } ?: _runtimeSnapshot.value.owner
            if (currentOwner != RuntimeOwner.None) {
                stopProxyInternal(targetMode = mode)
            }

            val generation = nextGeneration()

            clearRuntimeState(resetGroups = false)
            _currentProfile.value = activeProfile
            publishRuntimeSnapshot(
                RuntimeSnapshot(
                    owner = targetOwner,
                    phase = RuntimePhase.Starting,
                    targetMode = mode,
                    profileReady = true,
                    profileUuid = activeProfile.uuid.toString(),
                    profileName = activeProfile.name,
                    startedAt = System.currentTimeMillis(),
                    generation = generation,
                ),
            )

            when (targetOwner) {
                RuntimeOwner.RootTun -> {
                    startRootTun().getOrThrow()
                    handleRuntimeStarted(forceOwner = RuntimeOwner.RootTun)
                }
                RuntimeOwner.LocalTun,
                RuntimeOwner.LocalHttp -> startLocalRuntime(mode)
                RuntimeOwner.None -> Unit
            }
        }
    }

    suspend fun stopProxy(mode: ProxyMode? = null) {
        val targetMode = mode ?: networkSettingsStorage.proxyMode.value

        operationMutex.withLock {
            stopProxyInternal(targetMode)
        }
    }

    suspend fun queryProxyGroupNames(excludeNotSelectable: Boolean = false): List<String> {
        connectCurrentBackend()
        return ServiceClient.clash().queryProxyGroupNames(excludeNotSelectable)
    }

    suspend fun queryProfileProxyGroups(excludeNotSelectable: Boolean = false): List<ProxyGroup> {
        connectCurrentBackend()
        return ServiceClient.clash().queryProfileProxyGroups(excludeNotSelectable)
    }

    suspend fun queryProxyGroup(name: String, sort: ProxySort = ProxySort.Default): ProxyGroup {
        connectCurrentBackend()
        return ServiceClient.clash().queryProxyGroup(name, sort)
    }

    suspend fun selectProxy(group: String, proxyName: String): Boolean {
        Timber.d("Select proxy: group=$group proxy=$proxyName")
        connectCurrentBackend()
        val ok = ServiceClient.clash().patchSelector(group, proxyName)
        if (ok) {
            delay(200.milliseconds)
            refreshProxyGroups()
        }
        return ok
    }

    suspend fun healthCheck(group: String, refreshAfter: Boolean = true) {
        connectCurrentBackend()
        Timber.d("Health check request: group=%s refreshAfter=%s", group, refreshAfter)
        ServiceClient.clash().healthCheck(group)
        Timber.d("Health check dispatched: group=%s", group)
        if (refreshAfter) {
            repeat(4) {
                delay(1000.milliseconds)
                refreshProxyGroups()
            }
        } else {
            scope.launch {
                delay(300.milliseconds)
                repeat(3) {
                    runCatching { refreshProxyGroups() }
                    delay(500.milliseconds)
                }
            }
        }
    }

    suspend fun healthCheckProxy(proxyName: String): Int {
        connectCurrentBackend()
        Timber.d("Health check proxy request: proxy=%s", proxyName)
        val delay = ServiceClient.clash().healthCheckProxy(proxyName)
        Timber.d("Health check proxy done: proxy=%s delay=%s", proxyName, delay)
        refreshProxyGroups()
        return delay
    }

    suspend fun queryTunnelState(): TunnelState {
        connectCurrentBackend()
        return ServiceClient.clash().queryTunnelState()
    }

    suspend fun queryTrafficTotal(): Long {
        if (!_runtimeSnapshot.value.running) {
            _trafficTotal.value = 0L
            return 0L
        }
        connectCurrentBackend()
        val traffic = ServiceClient.clash().queryTrafficTotal()
        _trafficTotal.value = traffic
        updateTrafficReady()
        return traffic
    }

    suspend fun queryTrafficNow(): Long {
        if (!_runtimeSnapshot.value.running) {
            _trafficNow.value = 0L
            return 0L
        }
        connectCurrentBackend()
        val traffic = ServiceClient.clash().queryTrafficNow()
        _trafficNow.value = traffic
        updateTrafficReady()
        return traffic
    }

    suspend fun reloadCurrentProfile(): Result<Unit> {
        return runCatching {
            val profileManager = ServiceClient.profile()
            val currentProfile = profileManager.queryActive()
            if (currentProfile != null) {
                profileManager.setActive(currentProfile)
                _currentProfile.value = currentProfile
                delay(600.milliseconds)
                refreshAll()
            }
        }
    }

    fun updateServiceState(isRunning: Boolean) {
        _isRunning.value = isRunning
    }

    suspend fun refreshProxyGroups() {
        refreshProxyGroupsMutex.withLock {
            val snapshot = _runtimeSnapshot.value
            val groups = withContext(Dispatchers.IO) {
                runCatching {
                    if (!snapshot.running) {
                        return@runCatching queryPreviewProxyGroups()
                    }

                    if (snapshot.owner == RuntimeOwner.RootTun && !isRootSessionActive()) {
                        error("RootTun runtime not ready")
                    }

                    connectCurrentBackend()
                    ServiceClient.clash().queryAllProxyGroups(excludeNotSelectable = false).map(::toProxyGroupInfo)
                }.getOrElse { error ->
                    Timber.e(error, "Failed to refresh proxy groups")
                    null
                }
            }

            groups?.let {
                _proxyGroups.value = it
                updateGroupsReady(it.isNotEmpty())
                backfillPreviewCache(it)
            } ?: fallbackPreviewGroups(snapshot)?.let { cached ->
                _proxyGroups.value = cached
                updateGroupsReady(cached.isNotEmpty())
            }
        }
    }

    suspend fun refreshCurrentProfile() {
        when {
            _runtimeSnapshot.value.owner == RuntimeOwner.RootTun &&
                _runtimeSnapshot.value.phase == RuntimePhase.Running -> {
                val status = currentRootTunStatus()
                applyRootTunStatus(status)
                refreshRootCurrentProfile(status)
            }

            else -> {
                runCatching {
                    val profile = ServiceClient.profile().queryActive()
                    _currentProfile.value = profile
                    updateProfileReady(profile)
                }.onFailure { error ->
                    Timber.e(error, "Failed to refresh current profile")
                }
            }
        }
    }

    suspend fun refreshAll() {
        refreshCurrentProfile()
        refreshProxyGroups()
        if (_runtimeSnapshot.value.phase == RuntimePhase.Running) {
            queryTrafficNow()
            queryTrafficTotal()
        } else {
            _trafficNow.value = 0L
            _trafficTotal.value = 0L
        }
    }

    private suspend fun startLocalRuntime(mode: ProxyMode) {
        when (mode) {
            ProxyMode.Tun,
            ProxyMode.Http -> RuntimeServiceLauncher.start(
                context = appContext,
                mode = mode,
                source = RuntimeServiceLauncher.SOURCE_UI,
            )
            ProxyMode.RootTun -> error("local runtime does not support RootTun")
        }
    }

    private suspend fun startRootTun(): Result<Unit> {
        return runCatching {
            val result = RootTunController.start(appContext)
            if (!result.success) {
                error(result.error ?: "RootTun start failed")
            }
            applyRootTunStatus(rootTunStateStore.snapshot())
        }
    }

    private suspend fun stopRootTun(): Result<Unit> {
        return runCatching {
            val result = RootTunController.stop(appContext)
            if (!result.success) {
                error(result.error ?: "RootTun stop failed")
            }
            applyRootTunStatus(rootTunStateStore.snapshot())
        }
    }

    private suspend fun triggerStop(owner: RuntimeOwner) {
        when (owner) {
            RuntimeOwner.RootTun -> {
                stopRootTun().getOrThrow()
            }

            RuntimeOwner.LocalTun,
            RuntimeOwner.LocalHttp -> {
                withContext(Dispatchers.IO) {
                    runCatching {
                        ServiceClient.connect(appContext)
                        ServiceClient.clash().requestStop()
                    }.onFailure {
                        appContext.sendBroadcast(Intent(actionClashRequestStop).setPackage(appContext.packageName))
                    }
                    context.stopService(Intent(context, TunService::class.java))
                    context.stopService(Intent(context, ClashService::class.java))
                }
            }

            RuntimeOwner.None -> Unit
        }
    }

    private suspend fun stopProxyInternal(targetMode: ProxyMode) {
        val owner = detectActiveOwner().takeIf { it != RuntimeOwner.None } ?: _runtimeSnapshot.value.owner
        val generation = nextGeneration()

        if (owner == RuntimeOwner.None) {
            clearRuntimeState(resetGroups = false)
            publishRuntimeSnapshot(RuntimeStateMapper.idleSnapshot(targetMode, generation = generation))
            stopTrafficPolling()
            scope.launch { refreshPreviewStateSafely() }
            return
        }

        publishRuntimeSnapshot(
            _runtimeSnapshot.value.copy(
                owner = owner,
                phase = RuntimePhase.Stopping,
                targetMode = targetMode,
                profileReady = false,
                groupsReady = false,
                trafficReady = false,
                lastError = null,
                generation = generation,
            ),
        )

        triggerStop(owner)

        clearRuntimeState(resetGroups = false)
        publishRuntimeSnapshot(RuntimeStateMapper.idleSnapshot(targetMode, generation = generation))
        stopTrafficPolling()
        scope.launch { refreshPreviewStateSafely() }
    }

    private fun startTrafficPolling() {
        if (trafficPollingJob?.isActive == true) return
        trafficPollingJob = scope.launch {
            var tick = 0
            while (true) {
                val snapshot = _runtimeSnapshot.value
                if (!snapshot.running) {
                    delay(1000L.milliseconds)
                    continue
                }

                runCatching {
                    connectCurrentBackend()
                    queryTrafficNow()
                    if (tick % 5 == 0) {
                        queryTrafficTotal()
                    }
                }.onFailure { error ->
                    Timber.d(error, "Traffic polling skipped")
                }
                tick++

                if (tick % 3 == 0 && shouldRefreshRuntimePayload()) {
                    refreshAllSafely()
                }

                delay(1000L.milliseconds)
            }
        }
    }

    private fun stopTrafficPolling() {
        trafficPollingJob?.cancel()
        trafficPollingJob = null
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerServiceEventReceiver() {
        val filter = IntentFilter().apply {
            addAction(actionClashStarted)
            addAction(actionClashStopped)
            addAction(actionProfileChanged)
            addAction(actionProfileLoaded)
            addAction(actionOverrideChanged)
            addAction(actionServiceRecreated)
            addAction(actionRootRuntimeFailed)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(serviceEventsReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                appContext.registerReceiver(serviceEventsReceiver, filter)
            }
        }.onFailure { error ->
            Timber.w(error, "Failed to register service event receiver")
        }
    }

    private fun initializeRuntimeSnapshot() {
        val configuredMode = networkSettingsStorage.proxyMode.value
        clearLegacyRuntimeCaches()
        val rootStatus = resolveInitialRootTunStatus()
        applyRootTunStatus(rootStatus)
        val owner = when {
            rootStatus.state.isActive || rootStatus.runtimeReady -> RuntimeOwner.RootTun
            isLocalSessionActive(ProxyMode.Tun) -> RuntimeOwner.LocalTun
            isLocalSessionActive(ProxyMode.Http) -> RuntimeOwner.LocalHttp
            else -> RuntimeOwner.None
        }

        if (owner == RuntimeOwner.None) {
            clearRuntimeState(resetGroups = false)
            publishRuntimeSnapshot(RuntimeStateMapper.idleSnapshot(configuredMode))
            scope.launch { refreshPreviewStateSafely() }
            return
        }

        publishRuntimeSnapshot(
            RuntimeSnapshot(
                owner = owner,
                phase = if (owner == RuntimeOwner.RootTun) rootPhase(rootStatus) else RuntimePhase.Running,
                targetMode = modeForOwner(owner),
                profileReady = owner == RuntimeOwner.RootTun && !rootStatus.profileUuid.isNullOrBlank(),
                profileUuid = rootStatus.profileUuid.takeIf { owner == RuntimeOwner.RootTun },
                profileName = rootStatus.profileName.takeIf { owner == RuntimeOwner.RootTun },
                lastError = if (owner == RuntimeOwner.RootTun) rootStatus.lastError else null,
                startedAt = rootStatus.startedAt.takeIf { owner == RuntimeOwner.RootTun },
            ),
        )
        startTrafficPolling()
        scope.launch { refreshAllSafely() }
    }

    private fun detectActiveOwner(): RuntimeOwner {
        return when {
            isRootSessionActive() -> RuntimeOwner.RootTun
            isLocalSessionActive(ProxyMode.Tun) -> RuntimeOwner.LocalTun
            isLocalSessionActive(ProxyMode.Http) -> RuntimeOwner.LocalHttp
            else -> RuntimeOwner.None
        }
    }

    private fun resolveInitialRootTunStatus(): RootTunStatus {
        val persisted = rootTunStateStore.snapshot()
        if (!persisted.state.isActive && !persisted.runtimeReady) {
            return persisted
        }


        rootTunStateStore.markIdle("app restart cleanup")
        return rootTunStateStore.snapshot()
    }

    private fun isRootSessionActive(): Boolean {
        val status = _rootTunStatus.value
        return status.state.isActive || status.runtimeReady
    }

    private fun isLocalSessionActive(mode: ProxyMode?): Boolean {
        if (mode == null) return false
        return StatusProvider.isRuntimeActive(mode)
    }

    private suspend fun handleRuntimeStarted(forceOwner: RuntimeOwner? = null) {
        val currentSnapshot = _runtimeSnapshot.value
        val owner = forceOwner
            ?: currentSnapshot.owner.takeIf { it != RuntimeOwner.None }
            ?: detectActiveOwner()
        if (owner == RuntimeOwner.None) return

        publishRuntimeSnapshot(
            currentSnapshot.copy(
                owner = owner,
                phase = RuntimePhase.Running,
                targetMode = modeForOwner(owner),
                lastError = null,
            ),
        )
        startTrafficPolling()
        refreshAllSafely()
    }

    private suspend fun handleRuntimeStopped(reason: String?) {
        val configuredMode = networkSettingsStorage.proxyMode.value
        val generation = nextGeneration()

        if (!isRootSessionActive()) {
            val status = rootTunStateStore.snapshot()
            if (status.state.isActive) {
                rootTunStateStore.markIdle(reason ?: status.lastError)
            }
            applyRootTunStatus(rootTunStateStore.snapshot())
        }

        clearRuntimeState(resetGroups = false)
        publishRuntimeSnapshot(
            RuntimeStateMapper.idleSnapshot(
                configuredMode = configuredMode,
                generation = generation,
                lastError = reason,
            ),
        )
        stopTrafficPolling()
        scope.launch { refreshPreviewStateSafely() }
    }

    private fun handleRuntimeFailure(error: String?) {
        val generation = nextGeneration()
        if (!isRootSessionActive()) {
            rootTunStateStore.markIdle(error)
            applyRootTunStatus(rootTunStateStore.snapshot())
        }
        clearRuntimeState(resetGroups = false)
        publishRuntimeSnapshot(
            RuntimeStateMapper.idleSnapshot(
                configuredMode = networkSettingsStorage.proxyMode.value,
                generation = generation,
                lastError = error ?: "root runtime failed",
            ),
        )
        stopTrafficPolling()
        scope.launch { refreshPreviewStateSafely() }
    }

    private suspend fun refreshAllSafely() {
        if (_runtimeSnapshot.value.phase != RuntimePhase.Running) {
            return
        }
        runCatching { refreshAll() }
            .onFailure { error -> Timber.d(error, "Refresh runtime data skipped") }
    }

    private suspend fun refreshPreviewStateSafely() {
        runCatching {
            refreshCurrentProfile()
            refreshProxyGroups()
        }.onFailure { error ->
            Timber.d(error, "Refresh preview data skipped")
        }
    }

    private fun shouldRefreshRuntimePayload(): Boolean {
        val snapshot = _runtimeSnapshot.value
        return snapshot.phase == RuntimePhase.Running &&
            (!snapshot.profileReady || !snapshot.groupsReady || _proxyGroups.value.isEmpty() || _currentProfile.value == null)
    }

    private suspend fun currentRootTunStatus(): RootTunStatus {
        return runCatching { RootTunController.queryStatus(appContext) }
            .getOrElse { _rootTunStatus.value }
    }

    private fun clearLegacyRuntimeCaches() {
        StatusProvider.clearLegacyStateFiles()
        val rootStatus = rootTunStateStore.snapshot()
        if (!rootStatus.state.isActive && !rootStatus.runtimeReady) {
            runCatching {
                rootTunStateStore.clear()
            }
            applyRootTunStatus(rootTunStateStore.snapshot())
        } else {
            applyRootTunStatus(rootStatus)
        }
        if (!StatusProvider.serviceRunning) {
            runCatching {
                MMKV.mmkvWithID("runtime_snapshot", MMKV.MULTI_PROCESS_MODE)?.clearAll()
            }
        }
    }

    private fun applyRootTunStatus(status: RootTunStatus) {
        _rootTunStatus.value = status
    }

    private fun publishRuntimeSnapshot(snapshot: RuntimeSnapshot) {
        val normalized = snapshot.copy(running = snapshot.phase.running)
        _runtimeSnapshot.value = normalized
        _isRunning.value = normalized.running
    }

    private fun nextGeneration(): Long {
        generationCounter += 1L
        return generationCounter
    }

    private fun ownerForMode(mode: ProxyMode): RuntimeOwner {
        return when (mode) {
            ProxyMode.Tun -> RuntimeOwner.LocalTun
            ProxyMode.Http -> RuntimeOwner.LocalHttp
            ProxyMode.RootTun -> RuntimeOwner.RootTun
        }
    }

    private fun modeForOwner(owner: RuntimeOwner): ProxyMode {
        return when (owner) {
            RuntimeOwner.LocalTun -> ProxyMode.Tun
            RuntimeOwner.LocalHttp -> ProxyMode.Http
            RuntimeOwner.RootTun -> ProxyMode.RootTun
            RuntimeOwner.None -> networkSettingsStorage.proxyMode.value
        }
    }

    private suspend fun connectCurrentBackend() {
        ServiceClient.connect(appContext)
    }

    private suspend fun refreshRootCurrentProfile(status: RootTunStatus) {
        runCatching {
            connectCurrentBackend()
            val profile = status.profileUuid
                ?.takeIf { it.isNotBlank() }
                ?.let { uuid -> ServiceClient.profile().queryByUUID(UUID.fromString(uuid)) }
                ?: ServiceClient.profile().queryActive()

            if (profile != null) {
                _currentProfile.value = profile
            }
            updateProfileReady(profile)
        }.onFailure { error ->
            Timber.d(error, "Failed to refresh root current profile")
        }
    }

    private suspend fun queryPreviewProxyGroups(): List<ProxyGroupInfo> {
        val activeProfile = ServiceClient.profile().queryActive().also {
            _currentProfile.value = it
            updateProfileReady(it)
        }

        if (activeProfile == null) {
            return emptyList()
        }
        connectCurrentBackend()
        val groups = ServiceClient.clash()
            .queryProfileProxyGroups(excludeNotSelectable = false)
            .map(::toProxyGroupInfo)

        return groups
    }

    private fun backfillPreviewCache(groups: List<ProxyGroupInfo>) {
        val profile = _currentProfile.value ?: return
        previewCache = PreviewCacheEntry(
            previewCacheKey(profile, excludeNotSelectable = false),
            groups,
        )
    }

    private fun fallbackPreviewGroups(snapshot: RuntimeSnapshot): List<ProxyGroupInfo>? {
        if (snapshot.phase == RuntimePhase.Running) return null
        val profile = _currentProfile.value ?: return previewCache?.groups
        val key = previewCacheKey(profile, excludeNotSelectable = false)
        return previewCache?.takeIf { it.key == key }?.groups
    }

    private fun previewCacheKey(
        profile: Profile,
        excludeNotSelectable: Boolean,
    ): PreviewCacheKey {
        val overrideSignature = _runtimeSnapshot.value.effectiveFingerprint
            ?.takeIf { it.isNotBlank() }
            ?: _rootTunStatus.value.overrideFingerprint?.takeIf { it.isNotBlank() }
            ?: "profile-${profile.updatedAt}"
        return PreviewCacheKey(
            profileId = profile.uuid,
            profileUpdatedAt = profile.updatedAt,
            excludeNotSelectable = excludeNotSelectable,
            overrideSignature = overrideSignature,
        )
    }

    private fun toProxyGroupInfo(group: ProxyGroup): ProxyGroupInfo {
        return ProxyGroupInfo(
            name = group.name,
            type = group.type,
            proxies = group.proxies,
            now = group.now.ifBlank { "-" },
            icon = group.icon,
        )
    }

    private fun rootPhase(status: RootTunStatus): RuntimePhase {
        return when (status.state) {
            RootTunState.Idle -> RuntimePhase.Idle
            RootTunState.Starting -> RuntimePhase.Starting
            RootTunState.Running -> RuntimePhase.Running
            RootTunState.Stopping -> RuntimePhase.Stopping
            RootTunState.Failed -> RuntimePhase.Failed
        }
    }

    private fun clearRuntimeState(resetGroups: Boolean = true) {
        _currentProfile.value = null
        if (resetGroups) {
            _proxyGroups.value = emptyList()
        }
        _trafficNow.value = 0L
        _trafficTotal.value = 0L
    }

    private fun updateProfileReady(profile: Profile?) {
        val snapshot = _runtimeSnapshot.value
        publishRuntimeSnapshot(
            snapshot.copy(
                profileReady = profile != null,
                profileUuid = profile?.uuid?.toString() ?: snapshot.profileUuid,
                profileName = profile?.name ?: snapshot.profileName,
            ),
        )
    }

    private fun updateGroupsReady(ready: Boolean) {
        publishRuntimeSnapshot(_runtimeSnapshot.value.copy(groupsReady = ready))
    }

    private fun updateTrafficReady() {
        if (!_runtimeSnapshot.value.trafficReady) {
            publishRuntimeSnapshot(_runtimeSnapshot.value.copy(trafficReady = true))
        }
    }
}
