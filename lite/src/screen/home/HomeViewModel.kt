package com.github.yumelira.yumebox.screen.home

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.core.util.PollingTimerSpecs
import com.github.yumelira.yumebox.core.util.PollingTimers
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.data.repository.IpMonitoringState
import com.github.yumelira.yumebox.data.repository.NetworkInfoService
import com.github.yumelira.yumebox.domain.model.TrafficData
import com.github.yumelira.yumebox.config.TunProfileSync
import com.github.yumelira.yumebox.remote.VpnPermissionRequired
import com.github.yumelira.yumebox.runtime.client.ProfilesRepository
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
import com.github.yumelira.yumebox.runtime.client.ProxyGroupSyncPriority
import com.github.yumelira.yumebox.runtime.client.RuntimeStateMapper
import com.github.yumelira.yumebox.service.runtime.entity.Profile
import com.github.yumelira.yumebox.service.runtime.state.RuntimePhase
import com.github.yumelira.yumebox.service.runtime.state.RuntimeSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

enum class HomeControlState {
    Idle,
    Connecting,
    Running,
    Disconnecting,
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
    private val tunProfileSync: TunProfileSync,
) : AndroidViewModel(application) {
    private val fallbackProfile = MutableStateFlow<Profile?>(null)
    private val pendingTransition = MutableStateFlow(PendingTransition.None)
    private val messagesFlow = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val vpnPrepareIntentFlow = MutableSharedFlow<Intent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val messages = messagesFlow.asSharedFlow()
    val vpnPrepareIntent = vpnPrepareIntentFlow.asSharedFlow()

    val runtimeSnapshot = proxyFacade.runtimeSnapshot
    val isRunning: StateFlow<Boolean> = runtimeSnapshot
        .map(RuntimeStateMapper::isActuallyRunning)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            RuntimeStateMapper.isActuallyRunning(runtimeSnapshot.value),
        )

    val currentProfile: StateFlow<Profile?> = combine(
        proxyFacade.currentProfile,
        fallbackProfile,
    ) { runtimeProfile, previewProfile ->
        runtimeProfile ?: previewProfile
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val trafficData: StateFlow<TrafficData> = proxyFacade.trafficNow
        .map(TrafficData::from)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrafficData.ZERO)

    val selectedServerName: StateFlow<String?> = proxyFacade.resolvedPrimaryNode
        .map { it?.name }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedServerPing: StateFlow<Int?> = proxyFacade.resolvedPrimaryNode
        .map { node -> node?.delay?.takeIf { it > 0 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val ipMonitoringState: StateFlow<IpMonitoringState> = isRunning
        .flatMapLatest { running ->
            if (!running) {
                flowOf(IpMonitoringState.Loading)
            } else {
                networkInfoService.startIpMonitoring(
                    isProxyActiveFlow = isRunning,
                    externalRefreshFlow = PollingTimers.ticks(PollingTimerSpecs.HomeIpRefresh).map { Unit },
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IpMonitoringState.Loading)

    val controlState: StateFlow<HomeControlState> = combine(
        runtimeSnapshot,
        pendingTransition,
    ) { snapshot, pending ->
        resolveControlState(snapshot, pending)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        resolveControlState(runtimeSnapshot.value, pendingTransition.value),
    )

    init {
        refresh()
        observeRuntimeState()
        observeProfileChanges()
    }

    fun setActive(active: Boolean) {
        proxyFacade.setProxyGroupSyncPriority(
            priority = if (active) ProxyGroupSyncPriority.FAST else ProxyGroupSyncPriority.OFF,
            source = "lite_home",
        )
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                tunProfileSync.syncActiveProfile()
                proxyFacade.reconcileRuntimeState()
                fallbackProfile.value = profilesRepository.queryActiveProfile()
                networkInfoService.triggerRefresh()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Timber.w(error, "Failed to refresh lite home")
            }
        }
    }

    fun startProxy() {
        if (pendingTransition.value == PendingTransition.AwaitingPermission) return
        if (controlState.value != HomeControlState.Idle) return
        if (currentProfile.value == null) {
            messagesFlow.tryEmit("先导入并激活一个配置")
            return
        }

        pendingTransition.value = PendingTransition.Starting
        launchStartProxy()
    }

    fun onVpnPermissionResult(granted: Boolean) {
        if (pendingTransition.value != PendingTransition.AwaitingPermission) return
        if (!granted) {
            pendingTransition.value = PendingTransition.None
            messagesFlow.tryEmit("VPN 权限被拒绝")
            return
        }
        pendingTransition.value = PendingTransition.Starting
        launchStartProxy()
    }

    fun stopProxy() {
        if (controlState.value != HomeControlState.Running) return
        pendingTransition.value = PendingTransition.Stopping
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    proxyFacade.stopProxy(ProxyMode.Tun)
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                pendingTransition.value = PendingTransition.None
                messagesFlow.emit(error.message ?: "停止失败")
            }
        }
    }

    private fun observeRuntimeState() {
        viewModelScope.launch {
            runtimeSnapshot
                .map { it.phase }
                .distinctUntilChanged()
                .collect { phase ->
                    if (phase == RuntimePhase.Idle || phase == RuntimePhase.Failed || phase == RuntimePhase.Running) {
                        pendingTransition.value = PendingTransition.None
                    }
                }
        }
    }

    private fun observeProfileChanges() {
        viewModelScope.launch {
            proxyFacade.currentProfile
                .map { it?.uuid }
                .drop(1)
                .distinctUntilChanged()
                .collect {
                    try {
                        fallbackProfile.value = profilesRepository.queryActiveProfile()
                    } catch (error: Exception) {
                        if (error is CancellationException) throw error
                    }
                }
        }
    }

    private fun launchStartProxy() {
        viewModelScope.launch {
            try {
                tunProfileSync.syncActiveProfile()
                proxyFacade.startProxy(ProxyMode.Tun)
            } catch (error: VpnPermissionRequired) {
                pendingTransition.value = PendingTransition.AwaitingPermission
                vpnPrepareIntentFlow.emit(error.intent)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                pendingTransition.value = PendingTransition.None
                messagesFlow.emit(error.message ?: "启动失败")
            }
        }
    }

    private fun resolveControlState(
        snapshot: RuntimeSnapshot,
        pending: PendingTransition,
    ): HomeControlState {
        return when (snapshot.phase) {
            RuntimePhase.Running -> HomeControlState.Running
            RuntimePhase.Starting -> HomeControlState.Connecting
            RuntimePhase.Stopping -> HomeControlState.Disconnecting
            RuntimePhase.Idle,
            RuntimePhase.Failed,
                -> when (pending) {
                    PendingTransition.AwaitingPermission,
                    PendingTransition.Starting,
                        -> HomeControlState.Connecting

                    PendingTransition.Stopping -> HomeControlState.Disconnecting
                    PendingTransition.None -> HomeControlState.Idle
                }
        }
    }
}
