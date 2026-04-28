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



package com.github.yumelira.yumebox.screen.home

import android.app.Application
import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.core.model.ConnectionInfo
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.core.presentation.AndroidContractStateViewModel
import com.github.yumelira.yumebox.core.presentation.LoadableState
import com.github.yumelira.yumebox.core.util.AutoStartSessionGate
import com.github.yumelira.yumebox.core.util.PollingTimerSpecs
import com.github.yumelira.yumebox.core.util.PollingTimers
import com.github.yumelira.yumebox.core.model.ProxyMode
import com.github.yumelira.yumebox.data.gateway.IpMonitoringState
import com.github.yumelira.yumebox.data.gateway.NetworkInfoService
import com.github.yumelira.yumebox.data.store.NetworkSettingsStore
import com.github.yumelira.yumebox.core.domain.model.TrafficData
import com.github.yumelira.yumebox.runtime.client.ProfilesRepository
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
import com.github.yumelira.yumebox.runtime.client.ProxyGroupSyncPriority
import com.github.yumelira.yumebox.runtime.client.RuntimeStateMapper
import com.github.yumelira.yumebox.core.model.Profile
import com.github.yumelira.yumebox.runtime.api.service.runtime.entity.RuntimePhase
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import timber.log.Timber

enum class HomeProxyControlState {
    Idle,
    Connecting,
    Running,
    Disconnecting;

    val canInteract: Boolean
        get() = this == Idle || this == Running
}

private enum class PendingTransition {
    None,
    AwaitingPermission,
    Starting,
    Stopping,
}

class HomeViewModel(
    application: Application,
    private val proxyFacade: ProxyFacade,
    private val profilesRepository: ProfilesRepository,
    private val networkInfoService: NetworkInfoService,
    private val networkSettingsStore: NetworkSettingsStore,
) : AndroidContractStateViewModel<HomeViewModel.HomeUiState, HomeViewModel.HomeUiEffect>(
    application,
    HomeUiState(),
) {
    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    private val _recommendedProfile = MutableStateFlow<Profile?>(null)
    val recommendedProfile: StateFlow<Profile?> = _recommendedProfile.asStateFlow()

    private val _profilesLoaded = MutableStateFlow(false)
    val profilesLoaded: StateFlow<Boolean> = _profilesLoaded.asStateFlow()

    val hasEnabledProfile: Flow<Boolean> = profiles.map { list ->
        list.any { it.active }
    }

    val runtimeSnapshot = proxyFacade.runtimeSnapshot
    val isRunning = runtimeSnapshot
        .map(RuntimeStateMapper::isActuallyRunning)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RuntimeStateMapper.isActuallyRunning(runtimeSnapshot.value))
    val currentProfile = proxyFacade.currentProfile
    val trafficNow = proxyFacade.trafficNow
    val proxyGroups = proxyFacade.proxyGroups

    private val _proxyMode = MutableStateFlow(ProxyMode.Tun)
    val proxyMode: StateFlow<ProxyMode> = _proxyMode.asStateFlow()

    private val _pendingTransition = MutableStateFlow(PendingTransition.None)
    private var pendingStartRequest: PendingStartRequest? = null

    private val _vpnPrepareIntent = MutableSharedFlow<Intent>(
        replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val vpnPrepareIntent = _vpnPrepareIntent.asSharedFlow()

    val controlState: StateFlow<HomeProxyControlState> = combine(
        runtimeSnapshot,
        _pendingTransition,
    ) { snapshot, pendingTransition ->
        resolveControlState(snapshot.phase, pendingTransition)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        resolveControlState(runtimeSnapshot.value.phase, _pendingTransition.value),
    )

    private val _speedHistory = MutableStateFlow<List<TrafficData>>(emptyList())
    val speedHistory: StateFlow<List<TrafficData>> = _speedHistory.asStateFlow()
    private val _connections = MutableStateFlow<List<ConnectionInfo>>(emptyList())
    val connections: StateFlow<List<ConnectionInfo>> = _connections.asStateFlow()
    val tunnelMode: StateFlow<TunnelState.Mode?> = proxyFacade.tunnelMode
    private val _homeScreenActive = MutableStateFlow(false)
    private var reconcileJob: Job? = null
    private var lastReconcileTime = 0L

    private val mainProxyNode: StateFlow<com.github.yumelira.yumebox.core.model.Proxy?> =
        proxyFacade.resolvedPrimaryNode

    val selectedServerName: StateFlow<String?> =
        mainProxyNode.map { it?.name }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedServerPing: StateFlow<Int?> = mainProxyNode.map { node ->
        node?.delay?.takeIf { d -> d > 0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val ipMonitoringState: StateFlow<IpMonitoringState> = isRunning.flatMapLatest { running ->
        if (running) {
            networkInfoService.startIpMonitoring(
                isProxyActiveFlow = isRunning,
            )
        } else {
            flowOf(IpMonitoringState.Loading)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IpMonitoringState.Loading)

    init {
        refreshProfiles()
        reconcileRuntimeState()
        observeControlState()
        observeRuntimeState()
        observeRuntimeFailures()
        syncProxyModeState()
        startSpeedSampling()
        startConnectionSampling()
        observeProfileChanges()
    }

    private fun refreshProfiles() {
        viewModelScope.launch {
            try {
                val allProfiles = profilesRepository.queryAllProfiles()
                val active = profilesRepository.queryActiveProfile()
                _profiles.value = allProfiles
                _recommendedProfile.value = active
                _profilesLoaded.value = true
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Failed to refresh profiles")
                _profilesLoaded.value = true
            }
        }
    }

    private fun observeProfileChanges() {
        viewModelScope.launch {
            proxyFacade.currentProfile
                .map { it?.uuid }
                .distinctUntilChanged()
                .collect {
                    refreshProfiles()
                }
        }
    }

    private fun observeControlState() {
        viewModelScope.launch {
            controlState
                .collect { state ->
                    if (state != HomeProxyControlState.Running) {
                        _speedHistory.value = List(24) { TrafficData.ZERO }
                        _connections.value = emptyList()
                    }
                    _uiState.update {
                        it.copy(
                            isStartingProxy = state == HomeProxyControlState.Connecting,
                            loadingProgress = if (state == HomeProxyControlState.Connecting) {
                                MLang.Home.Message.Preparing
                            } else {
                                null
                            },
                        )
                    }
                }
        }
    }

    private fun observeRuntimeState() {
        viewModelScope.launch {
            runtimeSnapshot
                .map { it.phase }
                .distinctUntilChanged()
                .collect { phase ->
                    when (phase) {
                        RuntimePhase.Starting -> {
                            clearPendingStart()
                            if (_pendingTransition.value == PendingTransition.AwaitingPermission ||
                                _pendingTransition.value == PendingTransition.Starting
                            ) {
                                _pendingTransition.value = PendingTransition.None
                            }
                        }

                        RuntimePhase.Running -> {
                            clearPendingStart()
                            if (_pendingTransition.value == PendingTransition.Starting ||
                                _pendingTransition.value == PendingTransition.AwaitingPermission
                            ) {
                                _pendingTransition.value = PendingTransition.None
                            }
                        }

                        RuntimePhase.Stopping -> {
                            clearPendingStart()
                            if (_pendingTransition.value == PendingTransition.Stopping) {
                                _pendingTransition.value = PendingTransition.None
                            }
                        }

                        RuntimePhase.Idle,
                        RuntimePhase.Failed -> {
                            clearPendingStart()
                            _pendingTransition.value = PendingTransition.None
                        }
                    }
                }
        }
    }

    private fun syncProxyModeState() {
        viewModelScope.launch {
            runtimeSnapshot
                .map { RuntimeStateMapper.resolveDisplayMode(it, networkSettingsStore.proxyMode.value) }
                .distinctUntilChanged()
                .collect {
                refreshProxyMode()
                }
        }
    }

    private fun observeRuntimeFailures() {
        viewModelScope.launch {
            runtimeSnapshot
                .drop(1)
                .map { snapshot -> Triple(snapshot.phase, snapshot.lastError, snapshot.generation) }
                .distinctUntilChanged()
                .collect { (phase, lastError, _) ->
                    if (phase == RuntimePhase.Failed && !lastError.isNullOrBlank()) {
                        showError(lastError)
                    }
                }
        }
    }

    fun refreshProxyMode() {
        val configuredMode = networkSettingsStore.proxyMode.value
        _proxyMode.value = RuntimeStateMapper.resolveDisplayMode(runtimeSnapshot.value, configuredMode)
    }

    fun setHomeScreenActive(isActive: Boolean) {
        _homeScreenActive.value = isActive
        proxyFacade.setProxyGroupSyncPriority(
            priority = if (isActive) ProxyGroupSyncPriority.FAST else ProxyGroupSyncPriority.OFF,
            source = "home",
        )
    }

    fun reconcileRuntimeState() {
        if (reconcileJob?.isActive == true) return
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastReconcileTime < RECONCILE_MIN_INTERVAL_MS) return
        lastReconcileTime = now
        reconcileJob = viewModelScope.launch {
            runCatching {
                proxyFacade.reconcileRuntimeState()
                refreshProfiles()
                refreshProxyMode()
            }.onFailure { error ->
                if (error is CancellationException) throw error
                Timber.w(error, "Failed to reconcile runtime state for home")
            }
        }
    }

    suspend fun reloadProfile() {
        try {
            applyLoading(true)

            val activeProfile = profilesRepository.queryActiveProfile()
            if (activeProfile == null) {
                showError(MLang.Home.Message.ConfigSwitchFailed.format(MLang.ProfilesVM.Error.ProfileNotExist))
                return
            }

            profilesRepository.updateProfile(activeProfile.uuid)

            profilesRepository.setActiveProfile(activeProfile.uuid)
            showMessage(MLang.Home.Message.ConfigSwitched)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "Failed to reload profile")
            showError(MLang.Home.Message.ConfigSwitchFailed.format(e.message))
        } finally {
            applyLoading(false)
        }
    }

    fun isCurrentProfile(profileId: java.util.UUID): Boolean {
        return currentProfile.value?.uuid == profileId
    }

    fun startProxy(profileId: String, mode: ProxyMode? = null) {
        if (!controlState.value.canInteract || controlState.value != HomeProxyControlState.Idle) return

        val request = PendingStartRequest(
            profileId = profileId,
            mode = mode ?: networkSettingsStore.proxyMode.value,
        )
        pendingStartRequest = request
        _pendingTransition.value = PendingTransition.Starting

        viewModelScope.launch {
            startProxyInternal(request)
        }
    }

    fun onVpnPermissionResult(granted: Boolean) {
        val request = pendingStartRequest ?: return
        if (_pendingTransition.value != PendingTransition.AwaitingPermission) return

        if (!granted) {
            clearPendingStart()
            _pendingTransition.value = PendingTransition.None
            refreshProxyMode()
            return
        }

        _pendingTransition.value = PendingTransition.Starting
        viewModelScope.launch {
            startProxyInternal(request)
        }
    }

    suspend fun stopProxy() {
        if (!controlState.value.canInteract || controlState.value != HomeProxyControlState.Running) return

        _pendingTransition.value = PendingTransition.Stopping

        try {
            withContext(Dispatchers.IO) {
                AutoStartSessionGate.markManualPaused()
                proxyFacade.stopProxy()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            _pendingTransition.value = PendingTransition.None
            Timber.e(e, "Failed to stop proxy")
            showError(MLang.Home.Message.StopFailed.format(e.message))
        }
    }

    private fun startSpeedSampling(sampleLimit: Int = 24) {
        viewModelScope.launch {
            _homeScreenActive.flatMapLatest { active ->
                if (active) PollingTimers.ticks(PollingTimerSpecs.HomeSpeedSampling) else emptyFlow()
            }.collect {
                val snapshot = runtimeSnapshot.value
                val sample = when {
                    snapshot.phase.running -> TrafficData.from(proxyFacade.trafficNow.value)
                    else -> TrafficData.ZERO
                }
                _speedHistory.update { old ->
                    val deque = ArrayDeque(old)
                    while (deque.size >= sampleLimit) deque.removeFirst()
                    deque.addLast(sample)
                    deque
                }
            }
        }
    }

    private fun startConnectionSampling(maxConnections: Int = 256) {
        viewModelScope.launch {
            _homeScreenActive.flatMapLatest { active ->
                if (active) proxyFacade.connectionSnapshot else emptyFlow()
            }.collect { snapshot ->
                if (!runtimeSnapshot.value.phase.running) {
                    _connections.value = emptyList()
                    return@collect
                }
                _connections.value = snapshot.connections.take(maxConnections)
            }
        }
    }

    private fun applyLoading(loading: Boolean) = super.setLoading(loading)
    private fun showMessage(message: String) = postMessage(message, HomeUiEffect.ShowMessage(message))
    private fun showError(error: String) = postError(error, HomeUiEffect.ShowError(error))
    fun consumeMessage() = clearMessageState()
    fun consumeError() = clearErrorState()

    private suspend fun startProxyInternal(request: PendingStartRequest) {
        val startedAt = System.currentTimeMillis()
        try {
            _proxyMode.value = request.mode
            Timber.d("Home startProxy kickoff: mode=${request.mode} profileId=${request.profileId}")

            if (request.mode == ProxyMode.RootTun) {
                val rootStatus = proxyFacade.evaluateRootAccess()
                if (!rootStatus.canStartRootTun) {
                    clearPendingStart()
                    _pendingTransition.value = PendingTransition.None
                    showError(rootStatus.rootTunBlockedMessage())
                    return
                }
            }

            withContext(Dispatchers.IO) {
                if (request.profileId.isNotBlank()) {
                    profilesRepository.setActiveProfile(java.util.UUID.fromString(request.profileId))
                }

                AutoStartSessionGate.clearManualPaused()
                proxyFacade.startProxy(request.mode)
            }

            Timber.i("Home startProxy completed in ${System.currentTimeMillis() - startedAt}ms, mode=${request.mode}")
        } catch (e: com.github.yumelira.yumebox.runtime.api.remote.VpnPermissionRequired) {
            _pendingTransition.value = PendingTransition.AwaitingPermission
            _vpnPrepareIntent.emit(e.intent)
            Timber.i("VPN permission required")
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            clearPendingStart()
            _pendingTransition.value = PendingTransition.None
            Timber.e(e, "Failed to start proxy")
            showError(MLang.Home.Message.StartFailed.format(e.message))
        }
    }

    private fun clearPendingStart() {
        pendingStartRequest = null
    }

    private fun resolveControlState(
        phase: RuntimePhase,
        pendingTransition: PendingTransition,
    ): HomeProxyControlState {
        if (pendingTransition == PendingTransition.Stopping &&
            phase != RuntimePhase.Stopping &&
            phase != RuntimePhase.Idle &&
            phase != RuntimePhase.Failed
        ) {
            return HomeProxyControlState.Disconnecting
        }
        return when (phase) {
            RuntimePhase.Running -> HomeProxyControlState.Running
            RuntimePhase.Starting -> HomeProxyControlState.Connecting
            RuntimePhase.Stopping -> HomeProxyControlState.Disconnecting
            RuntimePhase.Idle,
            RuntimePhase.Failed -> when (pendingTransition) {
                PendingTransition.AwaitingPermission,
                PendingTransition.Starting -> HomeProxyControlState.Connecting
                PendingTransition.Stopping -> HomeProxyControlState.Idle
                PendingTransition.None -> HomeProxyControlState.Idle
            }
        }
    }

    private data class PendingStartRequest(
        val profileId: String,
        val mode: ProxyMode,
    )

    companion object {
        private const val RECONCILE_MIN_INTERVAL_MS = 1_000L
    }

    data class HomeUiState(
        override val isLoading: Boolean = false,
        val isStartingProxy: Boolean = false,
        val loadingProgress: String? = null,
        override val message: String? = null,
        override val error: String? = null
    ) : LoadableState<HomeUiState> {
        override fun withLoading(loading: Boolean): HomeUiState = copy(isLoading = loading)
        override fun withError(error: String?): HomeUiState = copy(error = error)
        override fun withMessage(message: String?): HomeUiState = copy(message = message)
    }

    sealed interface HomeUiEffect {
        data class ShowMessage(val message: String) : HomeUiEffect
        data class ShowError(val message: String) : HomeUiEffect
    }
}
