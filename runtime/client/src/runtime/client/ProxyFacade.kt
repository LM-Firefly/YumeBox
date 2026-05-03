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

import android.content.Context
import android.net.VpnService
import com.github.yumelira.yumebox.core.appContextOrSelf
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.core.model.*
import com.github.yumelira.yumebox.core.util.PollingTimerSpecs
import com.github.yumelira.yumebox.core.util.PollingTimers
import com.github.yumelira.yumebox.core.util.throttleWhenScreenOff
import com.github.yumelira.yumebox.core.model.ProxyMode
import com.github.yumelira.yumebox.data.store.NetworkSettingsStore
import com.github.yumelira.yumebox.data.util.ProxyChainResolver
import com.github.yumelira.yumebox.runtime.client.internal.ProxyEventBus
import com.github.yumelira.yumebox.runtime.client.internal.ProxyServiceEvent
import com.github.yumelira.yumebox.runtime.client.internal.RuntimeBackendRouter
import com.github.yumelira.yumebox.runtime.client.internal.TrafficStatsPoller
import com.github.yumelira.yumebox.runtime.client.remote.ServiceClient
import com.github.yumelira.yumebox.runtime.api.remote.VpnPermissionRequired
import com.github.yumelira.yumebox.runtime.client.root.RootTunController
import com.github.yumelira.yumebox.runtime.api.service.LocalRuntimePhase
import com.github.yumelira.yumebox.runtime.api.service.LocalRuntimeServiceContract
import com.github.yumelira.yumebox.runtime.api.service.LocalRuntimeStatusContract
import com.github.yumelira.yumebox.runtime.api.service.ProxyServiceContracts
import com.github.yumelira.yumebox.runtime.api.service.root.RootAccessStatus
import com.github.yumelira.yumebox.runtime.api.service.root.RootTunState
import com.github.yumelira.yumebox.runtime.api.service.root.RootTunStatus
import com.github.yumelira.yumebox.core.model.Profile
import com.github.yumelira.yumebox.runtime.api.service.runtime.entity.RuntimeOwner
import com.github.yumelira.yumebox.runtime.api.service.runtime.entity.RuntimePhase
import com.github.yumelira.yumebox.runtime.api.service.runtime.entity.RuntimeSnapshot
import com.github.yumelira.yumebox.runtime.api.service.runtime.entity.RuntimeTargetMode
import com.github.yumelira.yumebox.runtime.api.service.runtime.entity.toRuntimeTargetMode
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.ConcurrentHashMap

enum class ProxyGroupSyncPriority {
    OFF,
    SLOW,
    FAST,
}

class ProxyFacade(
    private val context: Context,
    private val networkSettingsStorage: NetworkSettingsStore,
) {
    private data class DelayCacheEntry(
        val delay: Int,
        val updatedAt: Long,
    )

    private companion object {
        const val DEFAULT_SYNC_PRIORITY_SOURCE = "default"
        const val PROXY_SELECT_FULL_REFRESH_DELAY_MS = 400L
        const val ROOT_TUN_BOOTSTRAP_ATTEMPTS = 20
        const val ROOT_TUN_BOOTSTRAP_DELAY_MS = 300L
        const val PROXY_DELAY_CACHE_TTL_MS = 5 * 60 * 1000L
    }

    private val appContext: Context = context.appContextOrSelf
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val rootTunStateStore by lazy { RuntimeContractResolver.rootTunStateStore(appContext) }
    private val eventBus = ProxyEventBus(appContext)
    private val router = RuntimeBackendRouter(
        appContext = appContext,
        ownerProvider = { _runtimeSnapshot.value.owner },
        runningProvider = { _runtimeSnapshot.value.running },
    )
    private val traffic = TrafficStatsPoller(
        router = router,
        screenOn = eventBus.screenOn,
        onTrafficUpdated = ::updateTrafficReady,
        onPayloadRefreshDue = ::refreshAllSafely,
        shouldRefreshPayload = ::shouldRefreshRuntimePayload,
    )
    private val runtimeControl = ProxyRuntimeControl(appContext) { eventBus.actionClashRequestStop }
    private val previewCache = ProxyGroupPreviewCache()
    private val proxyChainResolver = ProxyChainResolver()
    private val _rootTunStatus = MutableStateFlow(RootTunStatus())
    val rootTunStatus: StateFlow<RootTunStatus> = _rootTunStatus.asStateFlow()
    private val _runtimeSnapshot = MutableStateFlow(
        RuntimeStateMapper.idleSnapshot(networkSettingsStorage.proxyMode.value),
    )
    val runtimeSnapshot: StateFlow<RuntimeSnapshot> = _runtimeSnapshot.asStateFlow()

    val isRunning: StateFlow<Boolean> = runtimeSnapshot
        .map { it.running }
        .stateIn(
            scope,
            SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            runtimeSnapshot.value.running,
        )

    private val _proxyGroups = MutableStateFlow<List<ProxyGroupInfo>>(emptyList())
    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = _proxyGroups.asStateFlow()
    private val _resolvedPrimaryNode = MutableStateFlow<Proxy?>(null)
    val resolvedPrimaryNode: StateFlow<Proxy?> = _resolvedPrimaryNode.asStateFlow()

    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    val trafficNow: StateFlow<Traffic> get() = traffic.trafficNow
    val trafficTotal: StateFlow<Traffic> get() = traffic.trafficTotal
    val connectionSnapshot: StateFlow<ConnectionSnapshot> get() = traffic.connectionSnapshot
    val tunnelMode: StateFlow<TunnelState.Mode?> get() = traffic.tunnelMode
    private var proxyGroupSyncJob: Job? = null
    private var previewWarmupJob: Job? = null
    private var rootTunBootstrapJob: Job? = null
    private val refreshProxyGroupsMutex = Mutex()
    private val operationMutex = Mutex()
    private val proxyGroupSyncMutex = Mutex()
    private val rootTunBootstrapMutex = Mutex()
    private val syncPriorityRequests = MutableStateFlow<Map<String, ProxyGroupSyncPriority>>(emptyMap())
    private var activeProxyGroupSyncPriority = ProxyGroupSyncPriority.OFF
    private var lastProxyGroupsSummary: String? = null
    private val generationCounter = AtomicLong(0L)
    private val proxyDelayCache = ConcurrentHashMap<String, DelayCacheEntry>()
    private var pendingGroupsRefreshJob: Job? = null
    private val pendingGroupRefreshJobs = ConcurrentHashMap<String, Job>()

    val screenOn: StateFlow<Boolean> get() = eventBus.screenOn

    private val _screenOn: StateFlow<Boolean> get() = eventBus.screenOn

    init {
        RuntimeContractResolver.warmUp(appContext)
        eventBus.register()
        observeServiceEvents()
        observeProxyGroupSyncPriority()
        initializeRuntimeSnapshot()
    }

    fun shutdown() {
        runCatching { eventBus.unregister() }
        runCatching { ServiceClient.disconnect() }
        runCatching { scope.cancel() }
    }

    private fun observeServiceEvents() {
        eventBus.events
            .onEach { event ->
                when (event) {
                    ProxyServiceEvent.ClashStarted ->
                        reconcileAndRefreshRuntimeState()
                    is ProxyServiceEvent.ClashStopped ->
                        handleRuntimeStopped(event.reason)
                    ProxyServiceEvent.ProfileLoaded,
                    ProxyServiceEvent.ProfileChanged,
                    ProxyServiceEvent.OverrideChanged,
                    ProxyServiceEvent.ServiceRecreated ->
                        reconcileAndRefreshRuntimeState()
                    is ProxyServiceEvent.RootRuntimeFailed -> {
                        Timber.w("Root runtime failed: ${event.error}")
                        handleRuntimeFailure(event.error)
                    }
                }
            }
            .launchIn(scope)
    }

    fun setProxyGroupSyncPriority(
        priority: ProxyGroupSyncPriority,
        source: String = DEFAULT_SYNC_PRIORITY_SOURCE,
    ) {
        syncPriorityRequests.update { current ->
            if (priority == ProxyGroupSyncPriority.OFF) {
                current - source
            } else {
                current + (source to priority)
            }
        }
    }

    fun warmUpProxyGroups() {
        if (previewWarmupJob?.isActive == true) return
        previewWarmupJob = launchPreviewWarmup()
    }

    suspend fun awaitProxyGroupWarmUp() {
        previewWarmupJob?.let { existing ->
            when {
                existing.isActive -> {
                    existing.join()
                    return
                }

                existing.isCompleted -> return
            }
        }

        val job = launchPreviewWarmup()
        previewWarmupJob = job
        job.join()
    }

    suspend fun reconcileRuntimeState() {
        operationMutex.withLock {
            val configuredMode = networkSettingsStorage.proxyMode.value
            RuntimeContractResolver.localRuntimeStatus.reconcilePersistedRuntimeState()
            val shouldBootstrapRootTun = shouldBootstrapRootTunRuntime()
            val rootStatus = resolveObservedRootTunStatus()
            applyRootTunStatus(rootStatus)
            val owner = ProxyRuntimeOwnership.detectOwner(rootStatus, ::isLocalSessionActive)

            if (owner == RuntimeOwner.None) {
                stopTrafficPolling()
                clearRuntimeState(resetGroups = false)
                publishRuntimeSnapshot(RuntimeStateMapper.idleSnapshot(configuredMode))
                if (shouldBootstrapRootTun) {
                    scheduleRootTunBootstrap()
                } else {
                    stopRootTunBootstrap()
                }
                refreshPreviewStateSafely()
                return
            }

            if (owner != RuntimeOwner.RootTun) {
                stopRootTunBootstrap()
            }
            if (owner == RuntimeOwner.RootTun) {
                ensureRootTunServiceAttached(rootStatus)
            }

            publishRuntimeSnapshot(
                ProxyRuntimeOwnership.activeSnapshot(
                    owner = owner,
                    configuredMode = configuredMode,
                    rootStatus = rootStatus,
                    localPhase = localRuntimePhaseForOwner(owner),
                    localStartedAt = localRuntimeStartedAtForOwner(owner),
                ),
            )

            if (_runtimeSnapshot.value.phase.running) {
                startTrafficPolling()
                refreshAllSafely()
            } else {
                stopTrafficPolling()
                refreshPreviewStateSafely()
            }
            if (owner == RuntimeOwner.RootTun) {
                scheduleRootTunBootstrap()
            }
        }
    }

    private suspend fun reconcileAndRefreshRuntimeState() {
        reconcileRuntimeState()
        if (_runtimeSnapshot.value.phase == RuntimePhase.Running) {
            refreshAllSafely()
        } else {
            refreshPreviewStateSafely()
        }
    }

    private fun launchPreviewWarmup(): Job {
        return scope.launch {
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
            val targetOwner = ProxyRuntimeOwnership.ownerForMode(mode)
            val currentOwner = detectActiveOwner().takeIf { it != RuntimeOwner.None } ?: _runtimeSnapshot.value.owner
            if (currentOwner != RuntimeOwner.None) {
                stopProxyInternal(targetMode = mode, completeImmediately = true)
            }

            val generation = nextGeneration()

            clearRuntimeState(resetGroups = false)
            _currentProfile.value = activeProfile
            publishRuntimeSnapshot(
                ProxyRuntimeOwnership.startingSnapshot(
                    owner = targetOwner,
                    targetMode = mode,
                    profile = activeProfile,
                    generation = generation,
                ),
            )

            runCatching {
                runtimeControl.start(targetOwner, mode)
            }.onFailure { error ->
                clearRuntimeState(resetGroups = false)
                publishRuntimeSnapshot(
                    RuntimeStateMapper.idleSnapshot(
                        configuredMode = mode,
                        generation = generation,
                        lastError = error.message,
                    ),
                )
                stopTrafficPolling()
                scope.launch { refreshPreviewStateSafely() }
                throw error
            }
            if (targetOwner == RuntimeOwner.RootTun) {
                applyRootTunStatus(RootTunStatus(state = RootTunState.Starting))
                scheduleRootTunBootstrap()
                handleRuntimeStarted(forceOwner = RuntimeOwner.RootTun)
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
        if (_runtimeSnapshot.value.owner == RuntimeOwner.RootTun) {
            return RootTunController.queryProxyGroupNames(appContext, excludeNotSelectable)
        }
        connectCurrentBackend()
        return ServiceClient.clash().queryProxyGroupNames(excludeNotSelectable)
    }

    suspend fun queryProfileProxyGroups(excludeNotSelectable: Boolean = false): List<ProxyGroup> {
        connectCurrentBackend()
        return ServiceClient.clash().queryProfileProxyGroups(excludeNotSelectable)
    }

    suspend fun queryProxyGroup(name: String, sort: ProxySort = ProxySort.Default): ProxyGroup {
        if (_runtimeSnapshot.value.owner == RuntimeOwner.RootTun) {
            return RootTunController.queryProxyGroup(appContext, name, sort)
        }
        connectCurrentBackend()
        return ServiceClient.clash().queryProxyGroup(name, sort)
    }

    suspend fun selectProxy(group: String, proxyName: String): Boolean {
        Timber.d("Select proxy: group=$group proxy=$proxyName")
        val ok = if (_runtimeSnapshot.value.owner == RuntimeOwner.RootTun) {
            RootTunController.patchSelector(appContext, group, proxyName)
        } else {
            connectCurrentBackend()
            ServiceClient.clash().patchSelector(group, proxyName)
        }
        if (ok) {
            delay(200L)
            refreshProxyGroup(group)
            scheduleRuntimeProxyGroupsRefresh(PROXY_SELECT_FULL_REFRESH_DELAY_MS)
        }
        return ok
    }

    suspend fun forceSelectProxy(group: String, proxyName: String): Boolean {
        Timber.d("Force select proxy: group=$group proxy=$proxyName")
        val ok = if (_runtimeSnapshot.value.owner == RuntimeOwner.RootTun) {
            RootTunController.patchForceSelector(appContext, group, proxyName)
        } else {
            connectCurrentBackend()
            ServiceClient.clash().patchForceSelector(group, proxyName)
        }
        if (ok) {
            applyLocalForceSelection(group = group, proxyName = proxyName)
            scope.launch {
                runCatching {
                    refreshProxyGroup(group)
                    scheduleRuntimeProxyGroupsRefresh(PROXY_SELECT_FULL_REFRESH_DELAY_MS)
                }
            }
        }
        return ok
    }

    private suspend fun applyLocalForceSelection(group: String, proxyName: String) {
        refreshProxyGroupsMutex.withLock {
            val desired = proxyName.trim()
            val currentGroups = _proxyGroups.value
            if (currentGroups.isEmpty()) return@withLock
            val updatedGroups = currentGroups.map { info ->
                if (info.name != group) return@map info
                val nextNow = if (desired.isNotEmpty()) desired else info.now.trim()
                info.copy(now = nextNow, fixed = desired)
            }
            publishProxyGroups(attachChainPaths(updatedGroups), cacheForPreview = true)
        }
    }

    suspend fun healthCheck(group: String) {
        Timber.d("Health check request: group=%s", group)
        if (_runtimeSnapshot.value.owner == RuntimeOwner.RootTun) {
            RootTunController.healthCheck(appContext, group)
        } else {
            connectCurrentBackend()
            ServiceClient.clash().healthCheck(group)
        }
        Timber.d("Health check dispatched: group=%s", group)
        scheduleRuntimeGroupRefresh(group, PollingTimerSpecs.ProxyHealthcheckRefresh.intervalMillis)
        scheduleRuntimeProxyGroupsRefresh(PollingTimerSpecs.ProxyHealthcheckRefresh.intervalMillis)
    }

    suspend fun healthCheckAll() {
        Timber.d("Health check all request")
        if (_runtimeSnapshot.value.owner == RuntimeOwner.RootTun) {
            RootTunController.queryAllProxyGroups(appContext, excludeNotSelectable = false)
                .map { it.name }
                .forEach { groupName ->
                    RootTunController.healthCheck(appContext, groupName)
                    scheduleRuntimeGroupRefresh(groupName, PollingTimerSpecs.ProxyHealthcheckRefresh.intervalMillis)
                }
        } else {
            connectCurrentBackend()
            Clash.healthCheckAll()
        }
        scheduleRuntimeProxyGroupsRefresh(PollingTimerSpecs.ProxyHealthcheckRefresh.intervalMillis)
    }

    suspend fun healthCheckProxy(group: String, proxyName: String): Int {
        Timber.d("Health check proxy request: group=%s proxy=%s", group, proxyName)
        val delay = if (_runtimeSnapshot.value.owner == RuntimeOwner.RootTun) {
            RootTunController.healthCheckProxy(appContext, group, proxyName).toIntOrNull() ?: 0
        } else {
            connectCurrentBackend()
            ServiceClient.clash().healthCheckProxy(group, proxyName)
        }
        Timber.d("Health check proxy done: group=%s proxy=%s delay=%s", group, proxyName, delay)
        refreshProxyGroup(group)
        scheduleRuntimeProxyGroupsRefresh(PROXY_SELECT_FULL_REFRESH_DELAY_MS)
        return delay
    }

    suspend fun queryTunnelState(): TunnelState = traffic.queryTunnelState()

    suspend fun queryConnections(): ConnectionSnapshot = traffic.queryConnections()

    suspend fun closeConnection(id: String): Boolean = traffic.closeConnection(id)

    suspend fun closeAllConnections() = traffic.closeAllConnections()

    suspend fun queryTrafficTotal(): Long = traffic.queryTrafficTotal()

    suspend fun queryTrafficNow(): Long = traffic.queryTrafficNow()

    suspend fun reloadCurrentProfile(): Result<Unit> {
        return runCatching {
            connectCurrentBackend()
            val profileManager = ServiceClient.profile()
            val currentProfile = profileManager.queryActive()
            if (currentProfile != null) {
                profileManager.setActive(currentProfile)
                _currentProfile.value = currentProfile
                delay(600L)
                refreshAll()
            }
        }
    }

    suspend fun evaluateRootAccess(): RootAccessStatus {
        return RuntimeContractResolver.rootAccessSupport.evaluateAsync(appContext)
    }

    fun hasRootPackageAccess(): Boolean {
        return RuntimeContractResolver.rootPackageQuery.hasRootAccess()
    }

    fun queryInstalledRootPackageNames(): Set<String>? {
        return RuntimeContractResolver.rootPackageQuery.queryInstalledPackageNames()
    }

    suspend fun refreshProxyGroups() {
        refreshProxyGroupsMutex.withLock {
            val snapshot = _runtimeSnapshot.value
            var missingLocalRuntime = false
            val groups = withContext(Dispatchers.IO) {
                runCatching {
                    if (!snapshot.running) {
                        return@runCatching queryPreviewProxyGroups()
                    }

                    if (snapshot.owner == RuntimeOwner.RootTun && !isRootSessionActive()) {
                        error("RootTun runtime not ready")
                    }

                    if (snapshot.owner == RuntimeOwner.RootTun) {
                        RootTunController.queryAllProxyGroups(
                            context = appContext,
                            excludeNotSelectable = false,
                        ).let(::toProxyGroupInfos)
                    } else {
                        connectCurrentBackend()
                        ServiceClient.clash().queryAllProxyGroups(excludeNotSelectable = false).let(::toProxyGroupInfos)
                    }
                }.getOrElse { error ->
                    Timber.e(error, "Failed to refresh proxy groups")
                    missingLocalRuntime = isMissingLocalRuntime(snapshot)
                    null
                }
            }

            if (groups != null) {
                publishProxyGroups(groups, cacheForPreview = true)
            } else if (missingLocalRuntime) {
                handleMissingLocalRuntime(snapshot, "runtime backend unavailable")
            } else if (!snapshot.running) {
                fallbackPreviewGroups(snapshot)?.let { cached ->
                    publishProxyGroups(cached, cacheForPreview = false)
                }
            }
        }
    }

    suspend fun refreshProxyGroup(name: String, sort: ProxySort = ProxySort.Default) {
        if (!_runtimeSnapshot.value.running) {
            if (_proxyGroups.value.isEmpty()) {
                refreshProxyGroups()
            }
            return
        }

        refreshProxyGroupsMutex.withLock {
            val snapshot = _runtimeSnapshot.value
            val updatedGroup = withContext(Dispatchers.IO) {
                runCatching {
                    if (snapshot.owner == RuntimeOwner.RootTun && !isRootSessionActive()) {
                        error("RootTun runtime not ready")
                    }

                    if (snapshot.owner == RuntimeOwner.RootTun) {
                        toProxyGroupInfo(RootTunController.queryProxyGroup(appContext, name, sort))
                    } else {
                        connectCurrentBackend()
                        toProxyGroupInfo(ServiceClient.clash().queryProxyGroup(name, sort))
                    }
                }.getOrElse { error ->
                    Timber.e(error, "Failed to refresh proxy group: %s", name)
                    null
                }
            } ?: return

            val updatedGroups = attachChainPaths(updateCachedProxyGroup(updatedGroup))
            publishProxyGroups(updatedGroups, cacheForPreview = true)
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
                    connectCurrentBackend()
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
            traffic.queryTrafficNow()
            traffic.queryTrafficTotal()
            traffic.refreshTunnelMode()
        } else {
            traffic.reset()
        }
    }

    private suspend fun stopProxyInternal(targetMode: ProxyMode, completeImmediately: Boolean = false) {
        val owner = detectActiveOwner().takeIf { it != RuntimeOwner.None } ?: _runtimeSnapshot.value.owner
        val generation = nextGeneration()

        if (owner == RuntimeOwner.None) {
            stopRootTunBootstrap()
            clearRuntimeState(resetGroups = false)
            publishRuntimeSnapshot(RuntimeStateMapper.idleSnapshot(targetMode, generation = generation))
            stopTrafficPolling()
            scope.launch { refreshPreviewStateSafely() }
            return
        }

        val previousSnapshot = _runtimeSnapshot.value
        publishRuntimeSnapshot(
            previousSnapshot.copy(
                owner = owner,
                phase = RuntimePhase.Stopping,
                targetMode = targetMode.toRuntimeTargetMode(),
                profileReady = false,
                groupsReady = false,
                trafficReady = false,
                lastError = null,
                generation = generation,
            ),
        )

        runCatching {
            runtimeControl.stop(owner)
        }.onFailure {
            publishRuntimeSnapshot(previousSnapshot)
            throw it
        }
        if (owner == RuntimeOwner.RootTun) {
            stopRootTunBootstrap()
            applyRootTunStatus(RootTunStatus(state = RootTunState.Stopping))
        }

        stopTrafficPolling()
        if (!completeImmediately) {
            return
        }

        clearRuntimeState(resetGroups = false)
        publishRuntimeSnapshot(RuntimeStateMapper.idleSnapshot(targetMode, generation = generation))
        scope.launch { refreshPreviewStateSafely() }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun startTrafficPolling() {
        traffic.start(scope)
    }

    private fun stopTrafficPolling() {
        traffic.stop()
    }

    private suspend fun refreshConnectionSnapshot() {
        // Retained for compatibility with [TrafficStatsPoller] callers; the poller now owns the connection cache.
        traffic.queryConnections()
    }

    private fun stopProxyGroupSync() {
        proxyGroupSyncJob?.cancel()
        proxyGroupSyncJob = null
    }

    private fun stopRootTunBootstrap() {
        rootTunBootstrapJob?.cancel()
        rootTunBootstrapJob = null
    }

    private fun initializeRuntimeSnapshot() {
        val configuredMode = networkSettingsStorage.proxyMode.value
        clearLegacyRuntimeCaches()
        RuntimeContractResolver.localRuntimeStatus.reconcilePersistedRuntimeState()
        val persistedRootStatus = rootTunStateStore.snapshot()
        val shouldBootstrapRootTun = shouldBootstrapRootTunRuntime(persistedRootStatus)
        val rootStatus = persistedRootStatus.takeIf(::shouldAttachRootTunForegroundService) ?: RootTunStatus()
        applyRootTunStatus(rootStatus)
        val owner = ProxyRuntimeOwnership.detectOwner(rootStatus, ::isLocalSessionActive)

        if (owner == RuntimeOwner.None) {
            clearRuntimeState(resetGroups = false)
            publishRuntimeSnapshot(RuntimeStateMapper.idleSnapshot(configuredMode))
            if (shouldBootstrapRootTun) {
                scheduleRootTunBootstrap()
            } else {
                stopRootTunBootstrap()
            }
            scope.launch { refreshPreviewStateSafely() }
            return
        }

        if (owner != RuntimeOwner.RootTun) {
            stopRootTunBootstrap()
        }
        if (owner == RuntimeOwner.RootTun) {
            ensureRootTunServiceAttached(rootStatus)
        }

        publishRuntimeSnapshot(
            ProxyRuntimeOwnership.activeSnapshot(
                owner = owner,
                configuredMode = configuredMode,
                rootStatus = rootStatus,
                localPhase = localRuntimePhaseForOwner(owner),
                localStartedAt = localRuntimeStartedAtForOwner(owner),
            ),
        )
        if (_runtimeSnapshot.value.phase.running) {
            startTrafficPolling()
            scope.launch { refreshAllSafely() }
        } else {
            stopTrafficPolling()
            scope.launch { refreshPreviewStateSafely() }
        }
        if (owner == RuntimeOwner.RootTun) {
            scheduleRootTunBootstrap()
            scope.launch { reconcileRootTunRuntimeStateSafely() }
        }
    }

    private fun detectActiveOwner(): RuntimeOwner {
        RuntimeContractResolver.localRuntimeStatus.reconcilePersistedRuntimeState()
        return ProxyRuntimeOwnership.detectOwner(_rootTunStatus.value, ::isLocalSessionActive)
    }

    private suspend fun resolveObservedRootTunStatus(): RootTunStatus {
        val shouldProbeRuntime = shouldBootstrapRootTunRuntime() ||
            _runtimeSnapshot.value.owner == RuntimeOwner.RootTun
        if (!shouldProbeRuntime) {
            return RootTunStatus()
        }

        return runCatching {
            RootTunController.queryStatus(appContext)
        }.onSuccess { status ->
            rootTunStateStore.updateStatus(status)
        }.getOrElse { error ->
            Timber.d(error, "RootTun live status unavailable during runtime reconcile")
            rootTunStateStore.snapshot()
        }
    }

    private fun shouldBootstrapRootTunRuntime(status: RootTunStatus = rootTunStateStore.snapshot()): Boolean {
        return shouldAttachRootTunForegroundService(status)
    }

    private fun shouldAttachRootTunForegroundService(status: RootTunStatus): Boolean {
        return status.state.isActive || status.runtimeReady
    }

    private fun isRootSessionActive(): Boolean {
        val status = _rootTunStatus.value
        return status.state.isActive || status.runtimeReady
    }

    private fun isLocalSessionActive(mode: ProxyMode?): Boolean {
        if (mode == null) return false
        return RuntimeContractResolver.localRuntimeStatus.isRuntimeActive(mode.toRuntimeTargetMode())
    }

    private fun localRuntimePhaseForOwner(owner: RuntimeOwner): LocalRuntimePhase {
        val localMode = localModeForOwner(owner) ?: return LocalRuntimePhase.Idle
        return RuntimeContractResolver.localRuntimeStatus.queryRuntimePhase(localMode.toRuntimeTargetMode())
    }

    private fun localRuntimeStartedAtForOwner(owner: RuntimeOwner): Long? {
        val localMode = localModeForOwner(owner) ?: return null
        return RuntimeContractResolver.localRuntimeStatus.queryRuntimeStartedAt(localMode.toRuntimeTargetMode())
            ?: _runtimeSnapshot.value.startedAt?.takeIf { _runtimeSnapshot.value.owner == owner }
    }

    private fun localModeForOwner(owner: RuntimeOwner): ProxyMode? {
        return when (owner) {
            RuntimeOwner.LocalTun -> ProxyMode.Tun
            RuntimeOwner.LocalHttp -> ProxyMode.Http
            RuntimeOwner.RootTun,
            RuntimeOwner.None,
                -> null
        }
    }

    private suspend fun handleRuntimeStarted(forceOwner: RuntimeOwner? = null) {
        val currentSnapshot = _runtimeSnapshot.value
        val owner = forceOwner
            ?: currentSnapshot.owner.takeIf { it != RuntimeOwner.None }
            ?: detectActiveOwner()
        if (owner == RuntimeOwner.None) return

        publishRuntimeSnapshot(
            ProxyRuntimeOwnership.startedSnapshot(
                current = currentSnapshot,
                owner = owner,
                configuredMode = networkSettingsStorage.proxyMode.value,
            ),
        )
        startTrafficPolling()
        refreshAllSafely()
    }

    private suspend fun handleRuntimeStopped(reason: String?) {
        val configuredMode = networkSettingsStorage.proxyMode.value
        val generation = nextGeneration()
        stopRootTunBootstrap()

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
        stopRootTunBootstrap()
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
        runCatching {
            withTimeoutOrNull(10_000L) { refreshAll() }
                ?: Timber.w("refreshAll timed out after 10s")
        }.onFailure { error -> Timber.d(error, "Refresh runtime data skipped") }
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

    private fun observeProxyGroupSyncPriority() {
        scope.launch {
            combine(_runtimeSnapshot, syncPriorityRequests) { snapshot, requests ->
                resolveEffectiveProxyGroupSyncPriority(snapshot, requests)
            }.distinctUntilChanged().collect { priority ->
                restartProxyGroupSyncLoop(priority)
            }
        }
    }

    private fun resolveEffectiveProxyGroupSyncPriority(
        snapshot: RuntimeSnapshot,
        requests: Map<String, ProxyGroupSyncPriority>,
    ): ProxyGroupSyncPriority {
        if (snapshot.phase != RuntimePhase.Running) {
            return ProxyGroupSyncPriority.OFF
        }
        return requests.values.maxByOrNull { it.ordinal } ?: ProxyGroupSyncPriority.OFF
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun restartProxyGroupSyncLoop(priority: ProxyGroupSyncPriority) {
        scope.launch {
            proxyGroupSyncMutex.withLock {
                if (activeProxyGroupSyncPriority == priority && proxyGroupSyncJob?.isActive == true) {
                    return@withLock
                }
                activeProxyGroupSyncPriority = priority
                proxyGroupSyncJob?.cancel()
                proxyGroupSyncJob = null
                if (priority == ProxyGroupSyncPriority.OFF) {
                    return@withLock
                }
                val timerSpec = when (priority) {
                    ProxyGroupSyncPriority.FAST -> PollingTimerSpecs.RuntimeProxyGroupSyncFast
                    ProxyGroupSyncPriority.SLOW -> PollingTimerSpecs.RuntimeProxyGroupSyncSlow
                    ProxyGroupSyncPriority.OFF -> return@withLock
                }
                proxyGroupSyncJob = scope.launch {
                    PollingTimers.ticks(timerSpec)
                        .throttleWhenScreenOff(_screenOn)
                        .collect {
                        refreshRuntimeProxyGroupsSafely()
                    }
                }
            }
        }
    }

    private suspend fun refreshRuntimeProxyGroupsSafely() {
        if (_runtimeSnapshot.value.phase != RuntimePhase.Running) {
            return
        }
        runCatching { refreshProxyGroups() }
            .onFailure { error -> Timber.d(error, "Runtime proxy group sync skipped") }
    }

    private fun scheduleRuntimeGroupRefresh(groupName: String, delayMillis: Long = 0L) {
        if (groupName.isBlank()) return
        pendingGroupRefreshJobs[groupName]?.cancel()
        pendingGroupRefreshJobs[groupName] = scope.launch {
            if (delayMillis > 0L) delay(delayMillis)
            runCatching { refreshProxyGroup(groupName) }
                .onFailure { error -> Timber.d(error, "Deferred proxy group refresh skipped: %s", groupName) }
            pendingGroupRefreshJobs.remove(groupName)
        }
    }

    private fun scheduleRuntimeProxyGroupsRefresh(delayMillis: Long = 0L) {
        pendingGroupsRefreshJob?.cancel()
        pendingGroupsRefreshJob = scope.launch {
            if (delayMillis > 0L) delay(delayMillis)
            refreshRuntimeProxyGroupsSafely()
        }
    }

    private suspend fun currentRootTunStatus(): RootTunStatus {
        return runCatching { RootTunController.queryStatus(appContext) }
            .getOrElse { rootTunStateStore.snapshot() }
    }

    private suspend fun reconcileRootTunRuntimeStateSafely() {
        runCatching {
            val persistedStatus = rootTunStateStore.snapshot()
            if (!shouldAttachRootTunForegroundService(persistedStatus)) {
                return@runCatching
            }
            ensureRootTunServiceAttached(persistedStatus)
            val status = RootTunController.queryStatus(appContext)
            rootTunStateStore.updateStatus(status)
            applyRootTunStatus(status)
            val configuredMode = networkSettingsStorage.proxyMode.value
            publishRuntimeSnapshot(
                ProxyRuntimeOwnership.activeSnapshot(
                    owner = RuntimeOwner.RootTun,
                    configuredMode = configuredMode,
                    rootStatus = status,
                    localPhase = LocalRuntimePhase.Idle,
                ),
            )
            if (status.state == RootTunState.Running) {
                startTrafficPolling()
                refreshAllSafely()
            }
        }.onFailure { error ->
            Timber.d(error, "RootTun bootstrap reconcile skipped")
        }
    }

    private fun ensureRootTunServiceAttached(status: RootTunStatus = rootTunStateStore.snapshot()) {
        if (!shouldAttachRootTunForegroundService(status)) {
            return
        }
        runCatching { RuntimeContractResolver.rootTunForegroundService.start(appContext) }
            .onFailure { error -> Timber.d(error, "Attach RootTun foreground service skipped") }
    }

    private fun scheduleRootTunBootstrap() {
        scope.launch {
            rootTunBootstrapMutex.withLock {
                if (rootTunBootstrapJob?.isActive == true) return@withLock
                rootTunBootstrapJob?.cancel()
                rootTunBootstrapJob = scope.launch {
                    repeat(ROOT_TUN_BOOTSTRAP_ATTEMPTS) { attempt ->
                        val persistedStatus = rootTunStateStore.snapshot()
                        if (!shouldAttachRootTunForegroundService(persistedStatus)) {
                            return@launch
                        }
                        val status = runCatching {
                            ensureRootTunServiceAttached(persistedStatus)
                            RootTunController.queryStatus(appContext)
                        }.getOrNull()
                        if (status != null) {
                            rootTunStateStore.updateStatus(status)
                            applyRootTunStatus(status)
                            val configuredMode = networkSettingsStorage.proxyMode.value
                            publishRuntimeSnapshot(
                                ProxyRuntimeOwnership.activeSnapshot(
                                    owner = RuntimeOwner.RootTun,
                                    configuredMode = configuredMode,
                                    rootStatus = status,
                                    localPhase = LocalRuntimePhase.Idle,
                                ),
                            )
                            if (status.state == RootTunState.Running) {
                                startTrafficPolling()
                                refreshAllSafely()
                                return@launch
                            }
                        }
                        if (attempt < ROOT_TUN_BOOTSTRAP_ATTEMPTS - 1) {
                            delay(ROOT_TUN_BOOTSTRAP_DELAY_MS)
                        }
                    }
                }
            }
        }
    }

    private fun clearLegacyRuntimeCaches() {
        RuntimeContractResolver.localRuntimeStatus.clearLegacyStateFiles()
        RuntimeContractResolver.localRuntimeStatus.reconcilePersistedRuntimeState()
        val rootStatus = rootTunStateStore.snapshot()
        if (!rootStatus.state.isActive && !rootStatus.runtimeReady) {
            runCatching {
                rootTunStateStore.clear()
            }
        }
        applyRootTunStatus(RootTunStatus())
        if (!RuntimeContractResolver.localRuntimeStatus.serviceRunning) {
            runCatching {
                MMKV.mmkvWithID("runtime_snapshot", MMKV.MULTI_PROCESS_MODE).clearAll()
            }
        }
    }

    private fun isMissingLocalRuntime(snapshot: RuntimeSnapshot): Boolean {
        if (snapshot.owner == RuntimeOwner.RootTun || snapshot.owner == RuntimeOwner.None) {
            return false
        }
        val mode = RuntimeStateMapper.modeForOwner(snapshot.owner) ?: return false
        return !RuntimeContractResolver.localRuntimeStatus.isLocalRuntimeServiceAlive(mode.toRuntimeTargetMode())
    }

    private suspend fun handleMissingLocalRuntime(snapshot: RuntimeSnapshot, reason: String?) {
        val mode = RuntimeStateMapper.modeForOwner(snapshot.owner) ?: return
        RuntimeContractResolver.localRuntimeStatus.markRuntimeIdle(mode.toRuntimeTargetMode())
        clearRuntimeState(resetGroups = false)
        publishRuntimeSnapshot(
            RuntimeStateMapper.idleSnapshot(
                configuredMode = networkSettingsStorage.proxyMode.value,
                generation = nextGeneration(),
                lastError = reason,
            ),
        )
        stopTrafficPolling()
        runCatching { queryPreviewProxyGroups() }
            .onSuccess { groups ->
                publishProxyGroups(groups, cacheForPreview = true)
            }
            .onFailure { error ->
                Timber.d(error, "Fallback preview refresh skipped after stale runtime reset")
            }
    }

    private fun applyRootTunStatus(status: RootTunStatus) {
        _rootTunStatus.value = status
    }

    private fun publishRuntimeSnapshot(snapshot: RuntimeSnapshot) {
        val normalized = snapshot.copy(running = snapshot.phase.running)
        _runtimeSnapshot.value = normalized
    }

    private fun nextGeneration(): Long = generationCounter.incrementAndGet()

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
        connectCurrentBackend()
        val activeProfile = ServiceClient.profile().queryActive().also {
            _currentProfile.value = it
            updateProfileReady(it)
        }

        if (activeProfile == null) {
            return emptyList()
        }
        val groups = ServiceClient.clash()
            .queryProfileProxyGroups(excludeNotSelectable = false)
            .let(::toProxyGroupInfos)

        return groups
    }

    private fun backfillPreviewCache(groups: List<ProxyGroupInfo>) {
        val profile = _currentProfile.value ?: return
        previewCache.store(
            profile = profile,
            excludeNotSelectable = false,
            overrideSignature = previewOverrideSignature(profile),
            groups = groups,
        )
    }

    private fun fallbackPreviewGroups(snapshot: RuntimeSnapshot): List<ProxyGroupInfo>? {
        val profile = _currentProfile.value
        return previewCache.fallback(
            phase = snapshot.phase,
            profile = profile,
            excludeNotSelectable = false,
            overrideSignature = profile?.let(::previewOverrideSignature).orEmpty(),
        )
    }

    private fun publishProxyGroups(groups: List<ProxyGroupInfo>, cacheForPreview: Boolean) {
        val normalizedGroups = enrichProxyGroupDelays(groups)
        val summary = summarizeProxyGroups(normalizedGroups)
        if (summary != lastProxyGroupsSummary) {
            _proxyGroups.value = normalizedGroups
            lastProxyGroupsSummary = summary
        }
        updateGroupsReady(normalizedGroups.isNotEmpty())
        updateResolvedPrimaryNode(normalizedGroups)
        if (cacheForPreview) {
            backfillPreviewCache(normalizedGroups)
        }
    }

    private fun enrichProxyGroupDelays(groups: List<ProxyGroupInfo>): List<ProxyGroupInfo> {
        if (groups.isEmpty()) {
            proxyDelayCache.clear()
            return groups
        }
        val now = System.currentTimeMillis()
        groups.asSequence()
            .flatMap { group -> group.proxies.asSequence() }
            .forEach { proxy ->
                if (proxy.delay != 0) {
                    proxyDelayCache[proxy.name] = DelayCacheEntry(delay = proxy.delay, updatedAt = now)
                }
            }
        val validDelayMap = proxyDelayCache.entries
            .filter { (_, entry) -> now - entry.updatedAt <= PROXY_DELAY_CACHE_TTL_MS }
            .associate { (name, entry) -> name to entry.delay }
        if (validDelayMap.isEmpty()) {
            proxyDelayCache.clear()
            return groups
        }
        proxyDelayCache.keys.removeAll { name -> name !in validDelayMap }
        val groupNowMap = groups.associate { group -> group.name to group.now.trim() }
        return groups.map { group ->
            val enrichedProxies = group.proxies.map { proxy ->
                val effectiveDelay = resolveEffectiveDelay(
                    name = proxy.name,
                    delayMap = validDelayMap,
                    groupNowMap = groupNowMap,
                    visited = mutableSetOf(),
                )
                if (effectiveDelay != null && effectiveDelay != proxy.delay) {
                    proxy.copy(delay = effectiveDelay)
                } else {
                    proxy
                }
            }
            group.copy(proxies = enrichedProxies)
        }
    }

    private fun resolveEffectiveDelay(
        name: String,
        delayMap: Map<String, Int>,
        groupNowMap: Map<String, String>,
        visited: MutableSet<String>,
    ): Int? {
        if (!visited.add(name)) return null
        val selectedChild = groupNowMap[name].orEmpty()
        if (selectedChild.isNotEmpty()) {
            val childDelay = resolveEffectiveDelay(
                name = selectedChild,
                delayMap = delayMap,
                groupNowMap = groupNowMap,
                visited = visited,
            )
            if (childDelay != null && childDelay != 0) {
                return childDelay
            }
        }
        return delayMap[name]?.takeIf { it != 0 }
    }

    private fun toProxyGroupInfo(group: ProxyGroup): ProxyGroupInfo {
        return ProxyGroupInfo(
            name = group.name,
            type = group.type,
            proxies = group.proxies,
            now = group.now.trim(),
            icon = group.icon,
            hidden = group.hidden,
            fixed = group.fixed.trim(),
            chainPath = emptyList(),
        )
    }

    private fun toProxyGroupInfos(groups: List<ProxyGroup>): List<ProxyGroupInfo> {
        return attachChainPaths(groups.map(::toProxyGroupInfo))
    }

    private fun attachChainPaths(groups: List<ProxyGroupInfo>): List<ProxyGroupInfo> {
        if (groups.isEmpty()) return groups
        val groupMap = groups.associateBy { it.name }
        return groups.map { group ->
            if (!group.type.group || group.now.isBlank()) {
                group.copy(chainPath = emptyList())
            } else {
                group.copy(
                    chainPath = proxyChainResolver.buildChainPathFromMap(
                        groupName = group.name,
                        currentNode = group.now,
                        groups = groupMap,
                    ),
                )
            }
        }
    }

    private fun updateCachedProxyGroup(updated: ProxyGroupInfo): List<ProxyGroupInfo> {
        val currentGroups = _proxyGroups.value
        if (currentGroups.isEmpty()) return listOf(updated)
        if (currentGroups.none { it.name == updated.name }) {
            return currentGroups + updated
        }
        return currentGroups.map { group ->
            if (group.name == updated.name) updated else group
        }
    }

    private fun summarizeProxyGroups(groups: List<ProxyGroupInfo>): String {
        return groups.joinToString(separator = "\n") { group ->
            buildString {
                append(group.name)
                append('|')
                append(group.type.name)
                append('|')
                append(group.now)
                append('|')
                append(group.hidden)
                append('|')
                append(group.proxies.size)
                group.proxies.forEach { proxy ->
                    append('|')
                    append(proxy.name)
                    append(':')
                    append(proxy.type.name)
                    append(':')
                    append(proxy.delay)
                }
            }
        }
    }

    private fun updateResolvedPrimaryNode(groups: List<ProxyGroupInfo>) {
        if (_runtimeSnapshot.value.phase != RuntimePhase.Running || groups.isEmpty()) {
            _resolvedPrimaryNode.value = null
            return
        }
        val mainGroup = groups.find { it.name.equals("Proxy", ignoreCase = true) } ?: groups.firstOrNull()
        val targetNode = mainGroup?.now?.trim().orEmpty()
        _resolvedPrimaryNode.value = targetNode.takeIf(String::isNotEmpty)?.let { resolveProxyNode(it, groups) }
    }

    private fun resolveProxyNode(
        nodeName: String,
        groups: List<ProxyGroupInfo>,
        visited: MutableSet<String> = linkedSetOf(),
    ): Proxy? {
        if (!visited.add(nodeName)) {
            return null
        }

        val group = groups.firstOrNull { it.name == nodeName }
        if (group != null) {
            val groupNow = group.now.trim()
            return groupNow.takeIf { it.isNotEmpty() }?.let { resolveProxyNode(it, groups, visited) }
        }

        groups.forEach { proxyGroup ->
            val proxy = proxyGroup.proxies.firstOrNull { it.name == nodeName } ?: return@forEach
            if (proxy.type.group) {
                val nextGroup = groups.firstOrNull { it.name == proxy.name } ?: return null
                val nextNode = nextGroup.now.trim()
                return nextNode.takeIf { it.isNotEmpty() }?.let { resolveProxyNode(it, groups, visited) }
            }
            return proxy
        }
        return null
    }

    private fun clearRuntimeState(resetGroups: Boolean = true) {
        _currentProfile.value = null
        traffic.reset()
        if (resetGroups) {
            _proxyGroups.value = emptyList()
            lastProxyGroupsSummary = null
        }
        _resolvedPrimaryNode.value = null
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

    private fun previewOverrideSignature(profile: Profile): String {
        return _runtimeSnapshot.value.effectiveFingerprint
            ?.takeIf { it.isNotBlank() }
            ?: _rootTunStatus.value.overrideFingerprint?.takeIf { it.isNotBlank() }
            ?: "profile-${profile.updatedAt}"
    }
}
