package com.github.yumelira.yumebox.runtime.client.internal

import com.github.yumelira.yumebox.core.model.ConnectionSnapshot
import com.github.yumelira.yumebox.core.model.Traffic
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.core.util.AppForegroundState
import com.github.yumelira.yumebox.core.util.PollingTimerSpecs
import com.github.yumelira.yumebox.core.util.PollingTimers
import com.github.yumelira.yumebox.core.util.throttleByScene
import com.github.yumelira.yumebox.core.util.throttleWhenScreenOff
import com.github.yumelira.yumebox.runtime.client.remote.ServiceClient
import com.github.yumelira.yumebox.runtime.client.root.RootTunController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

internal class TrafficStatsPoller(
    private val router: RuntimeBackendRouter,
    private val screenOn: StateFlow<Boolean>,
    private val onTrafficUpdated: () -> Unit,
    private val onPayloadRefreshDue: suspend () -> Unit,
    private val shouldRefreshPayload: () -> Boolean,
) {
    private companion object {
        const val TRAFFIC_TOTAL_POLL_TICKS = 10
        const val RUNTIME_PAYLOAD_REFRESH_TICKS = 15
        const val CONNECTION_IDLE_REFRESH_TICKS = 10
    }
    private val _trafficNow = MutableStateFlow(0L)
    val trafficNow: StateFlow<Traffic> = _trafficNow.asStateFlow()
    private val _trafficTotal = MutableStateFlow(0L)
    val trafficTotal: StateFlow<Traffic> = _trafficTotal.asStateFlow()
    private val _connectionSnapshot = MutableStateFlow(ConnectionSnapshot())
    val connectionSnapshot: StateFlow<ConnectionSnapshot> = _connectionSnapshot.asStateFlow()
    private val _tunnelMode = MutableStateFlow<TunnelState.Mode?>(null)
    val tunnelMode: StateFlow<TunnelState.Mode?> = _tunnelMode.asStateFlow()
    private val pollingMutex = Mutex()
    private var pollingJob: Job? = null
    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(scope: CoroutineScope) {
        scope.launch {
            pollingMutex.withLock {
                if (pollingJob?.isActive == true) return@withLock
                pollingJob?.cancel()
                pollingJob = scope.launch {
                    var tick = 0
                    var consecutiveFailures = 0
                    PollingTimers.ticks(PollingTimerSpecs.RuntimeTrafficPolling)
                        .throttleByScene(
                            screenOn = screenOn,
                            appForeground = AppForegroundState.foreground,
                            backgroundIntervalMs = 3_000L,
                            screenOffIntervalMs = 60_000L,
                        )
                        .collect {
                            if (!router.running) {
                                consecutiveFailures = 0
                                return@collect
                            }
                            runCatching {
                                queryTrafficNow()
                                if (tick % TRAFFIC_TOTAL_POLL_TICKS == 0) {
                                    queryTrafficTotal()
                                }
                                val hasSubscribers = _connectionSnapshot.subscriptionCount.value > 0
                                if (hasSubscribers || tick % CONNECTION_IDLE_REFRESH_TICKS == 0) {
                                    refreshConnectionSnapshot()
                                }
                                consecutiveFailures = 0
                            }.onFailure { error ->
                                consecutiveFailures++
                                Timber.d(
                                    error,
                                    "Traffic polling skipped (consecutive failures: %d)",
                                    consecutiveFailures,
                                )
                            }
                            tick++
                            if (tick % RUNTIME_PAYLOAD_REFRESH_TICKS == 0 && shouldRefreshPayload()) {
                                onPayloadRefreshDue()
                            }
                        }
                }
            }
        }
    }
    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
    }
    fun reset() {
        _trafficNow.value = 0L
        _trafficTotal.value = 0L
        _connectionSnapshot.value = ConnectionSnapshot()
        _tunnelMode.value = null
    }
    suspend fun queryTrafficNow(): Long {
        if (!router.running) {
            _trafficNow.value = 0L
            return 0L
        }
        val traffic = router.dispatch(
            onRoot = { ctx -> RootTunController.queryTrafficNow(ctx) },
            onLocal = { ServiceClient.clash().queryTrafficNow() },
        )
        _trafficNow.value = traffic
        onTrafficUpdated()
        return traffic
    }
    suspend fun queryTrafficTotal(): Long {
        if (!router.running) {
            _trafficTotal.value = 0L
            return 0L
        }
        val traffic = router.dispatch(
            onRoot = { ctx -> RootTunController.queryTrafficTotal(ctx) },
            onLocal = { ServiceClient.clash().queryTrafficTotal() },
        )
        _trafficTotal.value = traffic
        onTrafficUpdated()
        return traffic
    }
    suspend fun queryConnections(): ConnectionSnapshot {
        if (!router.running) return ConnectionSnapshot()
        return router.dispatch(
            onRoot = { ctx -> RootTunController.queryConnections(ctx) },
            onLocal = { ServiceClient.clash().queryConnections() },
        )
    }
    suspend fun closeConnection(id: String): Boolean {
        if (!router.running) return false
        return router.dispatch(
            onRoot = { ctx -> RootTunController.closeConnection(ctx, id) },
            onLocal = { ServiceClient.clash().closeConnection(id) },
        )
    }
    suspend fun closeAllConnections() {
        if (!router.running) return
        router.dispatch<Unit>(
            onRoot = { ctx -> RootTunController.closeAllConnections(ctx) },
            onLocal = { ServiceClient.clash().closeAllConnections() },
        )
    }
    suspend fun queryTunnelState(): TunnelState {
        return router.dispatch(
            onRoot = { ctx -> RootTunController.queryTunnelState(ctx) },
            onLocal = { ServiceClient.clash().queryTunnelState() },
        )
    }
    suspend fun refreshTunnelMode() {
        _tunnelMode.value = runCatching { queryTunnelState().mode }.getOrNull()
    }
    fun resetTunnelMode() {
        _tunnelMode.value = null
    }
    private suspend fun refreshConnectionSnapshot() {
        _connectionSnapshot.value = queryConnections()
    }
}
