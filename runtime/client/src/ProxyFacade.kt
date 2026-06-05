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
 * Copyright (c)  YumeLira & YumeRiMoe 2025 - Present
 *
 */

package com.github.yumelira.yumebox.runtime.client

import android.content.Context
import android.net.VpnService
import com.github.yumelira.yumebox.core.appContextOrSelf
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.core.domain.model.TrafficData
import com.github.yumelira.yumebox.core.model.*
import com.github.yumelira.yumebox.core.model.Profile
import com.github.yumelira.yumebox.core.model.ProxyMode
import com.github.yumelira.yumebox.core.util.AppForegroundState
import com.github.yumelira.yumebox.core.util.PollingTimers
import com.github.yumelira.yumebox.core.util.PollingTimerSpecs
import com.github.yumelira.yumebox.core.util.throttleByScene
import com.github.yumelira.yumebox.core.util.throttleWhenScreenOff
import com.github.yumelira.yumebox.data.store.NetworkSettingsStore
import com.github.yumelira.yumebox.data.util.ProxyChainResolver
import com.github.yumelira.yumebox.runtime.api.remote.VpnPermissionRequired
import com.github.yumelira.yumebox.runtime.api.service.LocalRuntimePhase
import com.github.yumelira.yumebox.runtime.api.service.LocalRuntimeServiceContract
import com.github.yumelira.yumebox.runtime.api.service.LocalRuntimeStatusContract
import com.github.yumelira.yumebox.runtime.api.service.ProxyServiceContracts
import com.github.yumelira.yumebox.runtime.api.service.root.RootAccessStatus
import com.github.yumelira.yumebox.runtime.api.service.root.RootTunState
import com.github.yumelira.yumebox.runtime.api.service.root.RootTunStatus
import com.github.yumelira.yumebox.runtime.api.service.runtime.entity.RuntimeOwner
import com.github.yumelira.yumebox.runtime.api.service.runtime.entity.RuntimePhase
import com.github.yumelira.yumebox.runtime.api.service.runtime.entity.RuntimeSnapshot
import com.github.yumelira.yumebox.runtime.api.service.runtime.entity.RuntimeTargetMode
import com.github.yumelira.yumebox.runtime.api.service.runtime.entity.toRuntimeTargetMode
import com.github.yumelira.yumebox.runtime.client.internal.ProxyEventBus
import com.github.yumelira.yumebox.runtime.client.internal.ProxyServiceEvent
import com.github.yumelira.yumebox.runtime.client.internal.RuntimeBackendRouter
import com.github.yumelira.yumebox.runtime.client.internal.TrafficStatsPoller
import com.github.yumelira.yumebox.runtime.client.remote.ServiceClient
import com.github.yumelira.yumebox.runtime.client.root.RootTunController
import com.tencent.mmkv.MMKV
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

enum class ProxyGroupSyncPriority {
    OFF,
    SLOW,
    FAST,
}

class ProxyFacade(private val context: Context, private val networkSettingsStorage: NetworkSettingsStore,) {
    private companion object {
        const val DEFAULT_SYNC_PRIORITY_SOURCE = "default"
        const val PROXY_SELECT_FULL_REFRESH_DELAY_MS = 400L
    }

    private val appContext: Context = context.appContextOrSelf
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
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
    private val proxyGroupManager = com.github.yumelira.yumebox.runtime.client.internal.ProxyGroupManager()
    private val rootTunManager = com.github.yumelira.yumebox.runtime.client.internal.RootTunManager(appContext)
    private val _runtimeSnapshot =
        MutableStateFlow(RuntimeStateMapper.idleSnapshot(networkSettingsStorage.proxyMode.value))
    val runtimeSnapshot: StateFlow<RuntimeSnapshot> = _runtimeSnapshot.asStateFlow()

    val isRunning: StateFlow<Boolean> = runtimeSnapshot
        .map { it.running }
        .stateIn(
            scope,
            SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            runtimeSnapshot.value.running,
        )

    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = proxyGroupManager.proxyGroups
    val resolvedPrimaryNode: StateFlow<Proxy?> = proxyGroupManager.resolvedPrimaryNode
    val rootTunStatus: StateFlow<RootTunStatus> = rootTunManager.rootTunStatus

    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    val trafficNow: StateFlow<Traffic> get() = traffic.trafficNow
    val trafficTotal: StateFlow<Traffic> get() = traffic.trafficTotal
    val connectionSnapshot: StateFlow<ConnectionSnapshot> get() = traffic.connectionSnapshot
    val tunnelMode: StateFlow<TunnelState.Mode?> get() = traffic.tunnelMode
    private var proxyGroupSyncJob: Job? = null
    private var previewWarmupJob: Job? = null
    private val operationMutex = Mutex()
    private val proxyGroupSyncMutex = Mutex()
    private val syncPriorityRequests =
        MutableStateFlow<Map<String, ProxyGroupSyncPriority>>(emptyMap())
    private var activeProxyGroupSyncPriority = ProxyGroupSyncPriority.OFF
    private val generationCounter = AtomicLong(0L)

    val screenOn: StateFlow<Boolean> get() = eventBus.screenOn

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
            val shouldBootstrapRootTun = rootTunManager.shouldBootstrapRootTunRuntime()
            val rootStatus = rootTunManager.queryLiveRootTunStatus()
            rootTunManager.applyRootTunStatus(rootStatus)
            val owner = ProxyRuntimeOwnership.detectOwner(rootStatus, ::isLocalSessionActive)

            if (owner == RuntimeOwner.None) {
                stopTrafficPolling()
                clearRuntimeState(resetGroups = false)
                publishRuntimeSnapshot(RuntimeStateMapper.idleSnapshot(configuredMode))
                if (shouldBootstrapRootTun) {
                    scheduleRootTunBootstrap()
                } else {
                    rootTunManager.stopRootTunBootstrap()
                }
                refreshPreviewStateSafely()
                return
            }

            if (owner != RuntimeOwner.RootTun) {
                rootTunManager.stopRootTunBootstrap()
            }
            if (owner == RuntimeOwner.RootTun) {
                rootTunManager.ensureRootTunServiceAttached(rootStatus)
            }

            publishRuntimeSnapshot(
                ProxyRuntimeOwnership.activeSnapshot(
                    owner = owner,
                    configuredMode = configuredMode,
                    rootStatus = rootStatus,
                    localPhase = localRuntimePhaseForOwner(owner),
                    localStartedAt = localRuntimeStartedAtForOwner(owner),
                )
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
            val currentOwner =
                detectActiveOwner().takeIf { it != RuntimeOwner.None }
                    ?: _runtimeSnapshot.value.owner
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
                )
            )

            runCatching { runtimeControl.start(targetOwner, mode) }
                .onFailure { error ->
                    clearRuntimeState(resetGroups = false)
                    publishRuntimeSnapshot(
                        RuntimeStateMapper.idleSnapshot(
                            configuredMode = mode,
                            generation = generation,
                            lastError = error.message,
                        )
                    )
                    stopTrafficPolling()
                    scope.launch { refreshPreviewStateSafely() }
                    throw error
                }
            if (targetOwner == RuntimeOwner.RootTun) {
                rootTunManager.applyRootTunStatus(
                    RootTunStatus(
                        state = RootTunState.Starting
                    )
                )
                scheduleRootTunBootstrap()
                handleRuntimeStarted(forceOwner = RuntimeOwner.RootTun)
            }
        }
    }

    suspend fun stopProxy(mode: ProxyMode? = null) {
        val targetMode = mode ?: networkSettingsStorage.proxyMode.value

        operationMutex.withLock { stopProxyInternal(targetMode) }
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
        val ok =
            if (_runtimeSnapshot.value.owner == RuntimeOwner.RootTun) {
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

    suspend fun patchTunnelMode(mode: TunnelState.Mode): Boolean {
        connectCurrentBackend()
        val ok = ServiceClient.clash().patchTunnelMode(mode)
        if (ok) {
            traffic.refreshTunnelMode()
        }
        return ok
    }

    private suspend fun applyLocalForceSelection(group: String, proxyName: String) {
        val desired = proxyName.trim()
        val currentGroups = proxyGroupManager.proxyGroups.value
        if (currentGroups.isEmpty()) return
        val updatedGroups = currentGroups.map { info ->
            if (info.name != group) return@map info
            val nextNow = if (desired.isNotEmpty()) desired else info.now.trim()
            info.copy(now = nextNow, fixed = desired)
        }
        proxyGroupManager.publishProxyGroups(updatedGroups, cacheForPreview = true)
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
        proxyGroupManager.scheduleRuntimeGroupRefresh(scope, group, PollingTimerSpecs.ProxyHealthcheckRefresh.intervalMillis) { refreshProxyGroup(it) }
        scheduleRuntimeProxyGroupsRefresh(PollingTimerSpecs.ProxyHealthcheckRefresh.intervalMillis)
    }

    suspend fun healthCheckAll() {
        Timber.d("Health check all request")
        if (_runtimeSnapshot.value.owner == RuntimeOwner.RootTun) {
            RootTunController.queryAllProxyGroups(appContext, excludeNotSelectable = false)
                .map { it.name }
                .forEach { groupName ->
                    RootTunController.healthCheck(appContext, groupName)
                    proxyGroupManager.scheduleRuntimeGroupRefresh(
                        scope,
                        groupName,
                        PollingTimerSpecs.ProxyHealthcheckRefresh.intervalMillis,
                    ) { refreshProxyGroup(it) }
                }
        } else {
            connectCurrentBackend()
            Clash.healthCheckAll()
        }
        scheduleRuntimeProxyGroupsRefresh(PollingTimerSpecs.ProxyHealthcheckRefresh.intervalMillis)
    }

    suspend fun healthCheckProxy(group: String, proxyName: String): Int {
        Timber.d("Health check proxy request: group=%s proxy=%s", group, proxyName)
        val delay =
            if (_runtimeSnapshot.value.owner == RuntimeOwner.RootTun) {
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
        return rootTunManager.evaluateRootAccess()
    }

    fun hasRootPackageAccess(): Boolean {
        return rootTunManager.hasRootPackageAccess()
    }

    fun queryInstalledRootPackageNames(): Set<String>? {
        return rootTunManager.queryInstalledRootPackageNames()
    }

    suspend fun refreshProxyGroups() {
        proxyGroupManager.refreshProxyGroups(
            appContext = appContext,
            snapshot = _runtimeSnapshot.value,
            isRootSessionActive = { rootTunManager.isRootSessionActive() },
            connectCurrentBackend = { connectCurrentBackend() },
        )
    }

    suspend fun refreshProxyGroup(name: String, sort: ProxySort = ProxySort.Default) {
        proxyGroupManager.refreshProxyGroup(
            appContext = appContext,
            name = name,
            sort = sort,
            snapshot = _runtimeSnapshot.value,
            isRootSessionActive = { rootTunManager.isRootSessionActive() },
            connectCurrentBackend = { connectCurrentBackend() },
        )
    }

    suspend fun refreshCurrentProfile() {
        when {
            _runtimeSnapshot.value.owner == RuntimeOwner.RootTun &&
                _runtimeSnapshot.value.phase == RuntimePhase.Running -> {
                val status = rootTunManager.currentRootTunStatus()
                rootTunManager.applyRootTunStatus(status)
                refreshRootCurrentProfile(status)
            }

            else -> {
                runCatching {
                        connectCurrentBackend()
                        val profile = ServiceClient.profile().queryActive()
                        _currentProfile.value = profile
                        updateProfileReady(profile)
                    }
                    .onFailure { error -> Timber.e(error, "Failed to refresh current profile") }
            }
        }
    }

    suspend fun refreshAll() {
        refreshCurrentProfile()
        refreshProxyGroups()
        if (_runtimeSnapshot.value.phase == RuntimePhase.Running) {
            traffic.queryTrafficNow(notify = false)
            traffic.queryTrafficTotal(notify = false)
            traffic.notifyTrafficUpdated()
            traffic.refreshTunnelMode()
        } else {
            traffic.reset()
        }
    }

    private suspend fun stopProxyInternal(
        targetMode: ProxyMode,
        completeImmediately: Boolean = false,
    ) {
        val owner =
            detectActiveOwner().takeIf { it != RuntimeOwner.None } ?: _runtimeSnapshot.value.owner
        val generation = nextGeneration()

        if (owner == RuntimeOwner.None) {
            rootTunManager.stopRootTunBootstrap()
            clearRuntimeState(resetGroups = false)
            publishRuntimeSnapshot(
                RuntimeStateMapper.idleSnapshot(targetMode, generation = generation)
            )
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
            )
        )

        runCatching { runtimeControl.stop(owner) }
            .onFailure {
                publishRuntimeSnapshot(previousSnapshot)
                throw it
            }
        if (owner == RuntimeOwner.RootTun) {
            rootTunManager.stopRootTunBootstrap()
            rootTunManager.applyRootTunStatus(
                RootTunStatus(
                    state = RootTunState.Stopping
                )
            )
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
        traffic.queryConnections()
    }

    private fun stopProxyGroupSync() {
        proxyGroupSyncJob?.cancel()
        proxyGroupSyncJob = null
    }

    private fun initializeRuntimeSnapshot() {
        val configuredMode = networkSettingsStorage.proxyMode.value
        rootTunManager.clearLegacyRuntimeCaches()
        RuntimeContractResolver.localRuntimeStatus.reconcilePersistedRuntimeState()
        val persistedRootStatus = rootTunManager.resolveObservedRootTunStatus()
        val shouldBootstrapRootTun = rootTunManager.shouldBootstrapRootTunRuntime(persistedRootStatus)
        val rootStatus =
            persistedRootStatus.takeIf { rootTunManager.shouldAttachRootTunForegroundService(it) } ?: RootTunStatus()
        rootTunManager.applyRootTunStatus(rootStatus)
        val owner = ProxyRuntimeOwnership.detectOwner(rootStatus, ::isLocalSessionActive)

        if (owner == RuntimeOwner.None) {
            clearRuntimeState(resetGroups = false)
            publishRuntimeSnapshot(RuntimeStateMapper.idleSnapshot(configuredMode))
            if (shouldBootstrapRootTun) {
                scheduleRootTunBootstrap()
            } else {
                rootTunManager.stopRootTunBootstrap()
            }
            scope.launch { refreshPreviewStateSafely() }
            return
        }

        if (owner != RuntimeOwner.RootTun) {
            rootTunManager.stopRootTunBootstrap()
        }
        if (owner == RuntimeOwner.RootTun) {
            rootTunManager.ensureRootTunServiceAttached(rootStatus)
        }

        publishRuntimeSnapshot(
            ProxyRuntimeOwnership.activeSnapshot(
                owner = owner,
                configuredMode = configuredMode,
                rootStatus = rootStatus,
                localPhase = localRuntimePhaseForOwner(owner),
                localStartedAt = localRuntimeStartedAtForOwner(owner),
            )
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
        return ProxyRuntimeOwnership.detectOwner(rootTunManager.rootTunStatus.value, ::isLocalSessionActive)
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
            RuntimeOwner.None -> null
        }
    }

    private suspend fun handleRuntimeStarted(forceOwner: RuntimeOwner? = null) {
        val currentSnapshot = _runtimeSnapshot.value
        val owner =
            forceOwner
                ?: currentSnapshot.owner.takeIf { it != RuntimeOwner.None }
                ?: detectActiveOwner()
        if (owner == RuntimeOwner.None) return

        publishRuntimeSnapshot(
            ProxyRuntimeOwnership.startedSnapshot(
                current = currentSnapshot,
                owner = owner,
                configuredMode = networkSettingsStorage.proxyMode.value,
            )
        )
        startTrafficPolling()
        refreshAllSafely()
    }

    private suspend fun handleRuntimeStopped(reason: String?) {
        val configuredMode = networkSettingsStorage.proxyMode.value
        val generation = nextGeneration()
        rootTunManager.stopRootTunBootstrap()

        if (!rootTunManager.isRootSessionActive()) {
            rootTunManager.markIdle(reason)
        }

        clearRuntimeState(resetGroups = false)
        publishRuntimeSnapshot(
            RuntimeStateMapper.idleSnapshot(
                configuredMode = configuredMode,
                generation = generation,
                lastError = reason,
            )
        )
        stopTrafficPolling()
        scope.launch { refreshPreviewStateSafely() }
    }

    private fun handleRuntimeFailure(error: String?) {
        val generation = nextGeneration()
        rootTunManager.stopRootTunBootstrap()
        if (!rootTunManager.isRootSessionActive()) {
            rootTunManager.markIdle(error)
        }
        clearRuntimeState(resetGroups = false)
        publishRuntimeSnapshot(
            RuntimeStateMapper.idleSnapshot(
                configuredMode = networkSettingsStorage.proxyMode.value,
                generation = generation,
                lastError = error ?: "root runtime failed",
            )
        )
        stopTrafficPolling()
        scope.launch { refreshPreviewStateSafely() }
    }

    private suspend fun refreshAllSafely() {
        if (_runtimeSnapshot.value.phase != RuntimePhase.Running) {
            return
        }
        runCatching { withTimeoutOrNull(10_000L) { refreshAll() } ?: Timber.w("refreshAll timed out after 10s") }
            .onFailure { error -> Timber.d(error, "Refresh runtime data skipped") }
    }

    private suspend fun refreshPreviewStateSafely() {
        runCatching {
                refreshCurrentProfile()
                refreshProxyGroups()
            }
            .onFailure { error -> Timber.d(error, "Refresh preview data skipped") }
    }

    private fun shouldRefreshRuntimePayload(): Boolean {
        val snapshot = _runtimeSnapshot.value
        return snapshot.phase == RuntimePhase.Running &&
            (!snapshot.profileReady ||
                !snapshot.groupsReady ||
                proxyGroupManager.proxyGroups.value.isEmpty() ||
                _currentProfile.value == null)
    }

    private fun observeProxyGroupSyncPriority() {
        scope.launch {
            combine(_runtimeSnapshot, syncPriorityRequests) { snapshot, requests ->
                    resolveEffectiveProxyGroupSyncPriority(snapshot, requests)
                }
                .distinctUntilChanged()
                .collect { priority -> restartProxyGroupSyncLoop(priority) }
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
                val (bgInterval, screenOffInterval) = when (priority) {
                    ProxyGroupSyncPriority.FAST -> 5_000L to 60_000L
                    ProxyGroupSyncPriority.SLOW -> 20_000L to 120_000L
                    ProxyGroupSyncPriority.OFF -> return@withLock
                }
                proxyGroupSyncJob = scope.launch {
                    PollingTimers.ticks(timerSpec)
                        .throttleByScene(
                            screenOn = screenOn,
                            appForeground = AppForegroundState.foreground,
                            backgroundIntervalMs = bgInterval,
                            screenOffIntervalMs = screenOffInterval,
                        )
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

    private fun scheduleRuntimeProxyGroupsRefresh(delayMillis: Long = 0L) {
        proxyGroupManager.scheduleRuntimeProxyGroupsRefresh(scope, delayMillis) { refreshRuntimeProxyGroupsSafely() }
    }

    private suspend fun reconcileRootTunRuntimeStateSafely() {
        rootTunManager.reconcileRootTunRuntimeState(
            snapshotProvider = { _runtimeSnapshot.value },
            onStartTrafficPolling = { startTrafficPolling() },
            onRefreshAll = { refreshAllSafely() },
        )
    }

    private fun scheduleRootTunBootstrap() {
        rootTunManager.scheduleRootTunBootstrap(
            scope = scope,
            snapshotProvider = { _runtimeSnapshot.value },
            onStatusResolved = { status, snapshot ->
                val configuredMode = networkSettingsStorage.proxyMode.value
                publishRuntimeSnapshot(
                    ProxyRuntimeOwnership.activeSnapshot(
                        owner = RuntimeOwner.RootTun,
                        configuredMode = configuredMode,
                        rootStatus = status,
                        localPhase = localRuntimePhaseForOwner(RuntimeOwner.RootTun),
                        localStartedAt = localRuntimeStartedAtForOwner(RuntimeOwner.RootTun),
                    )
                )
            },
            onStartTrafficPolling = { startTrafficPolling() },
            onRefreshAll = { refreshAllSafely() },
        )
    }

    private fun clearRuntimeState(resetGroups: Boolean = true) {
        _currentProfile.value = null
        traffic.reset()
        if (resetGroups) {
            proxyGroupManager.clearGroups()
        }
    }

    private fun updateProfileReady(profile: Profile?) {
        val snapshot = _runtimeSnapshot.value
        publishRuntimeSnapshot(
            snapshot.copy(
                profileReady = profile != null,
                profileUuid = profile?.uuid?.toString() ?: snapshot.profileUuid,
                profileName = profile?.name ?: snapshot.profileName,
            )
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

    private fun publishRuntimeSnapshot(snapshot: RuntimeSnapshot) {
        val normalized = snapshot.copy(running = snapshot.phase.running)
        _runtimeSnapshot.value = normalized
    }

    private fun previewOverrideSignature(profile: Profile): String {
        return _runtimeSnapshot.value.effectiveFingerprint?.takeIf { it.isNotBlank() }
            ?: rootTunManager.rootTunStatus.value.overrideFingerprint?.takeIf { it.isNotBlank() }
            ?: "profile-${profile.updatedAt}"
    }

    private suspend fun refreshRootCurrentProfile(status: RootTunStatus) {
        runCatching {
                connectCurrentBackend()
                val profile =
                    status.profileUuid
                        ?.takeIf { it.isNotBlank() }
                        ?.let { uuid -> ServiceClient.profile().queryByUUID(java.util.UUID.fromString(uuid)) }
                        ?: ServiceClient.profile().queryActive()

                if (profile != null) {
                    _currentProfile.value = profile
                }
                updateProfileReady(profile)
            }
            .onFailure { error -> Timber.d(error, "Failed to refresh root current profile") }
    }

    private fun nextGeneration(): Long = generationCounter.incrementAndGet()

    private suspend fun connectCurrentBackend() {
        ServiceClient.connect(appContext)
    }
}
