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

package com.github.yumelira.yumebox.core.util

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.isActive
import java.util.concurrent.ConcurrentHashMap

data class PollingTimerSpec(
    val name: String,
    val intervalMillis: Long,
    val initialDelayMillis: Long = intervalMillis,
) {
    init {
        require(name.isNotBlank()) { "Timer name must not be blank" }
        require(intervalMillis > 0L) { "Timer interval must be > 0" }
        require(initialDelayMillis >= 0L) { "Timer initial delay must be >= 0" }
    }
}

object PollingTimerSpecs {
    val AcgElapsedClock = PollingTimerSpec("acg_elapsed_clock", 1_000L, 0L)
    val HomeSpeedSampling = PollingTimerSpec("home_speed_sampling", 1_000L, 0L)
    val LogScreenRefresh = PollingTimerSpec("log_screen_refresh", 1_000L, 0L)
    val RuntimeTrafficPolling = PollingTimerSpec("runtime_traffic_polling", 1_000L, 0L)
    val RuntimeProxyGroupSyncFast = PollingTimerSpec("runtime_proxy_group_sync_fast", 1_000L, 0L)
    val RuntimeProxyGroupSyncSlow = PollingTimerSpec("runtime_proxy_group_sync_slow", 3_000L, 0L)
    val RuntimeRootLogPolling = PollingTimerSpec("runtime_root_log_polling", 1_000L, 0L)
    val ServiceTrafficNotification = PollingTimerSpec("service_traffic_notification", 1_000L, 0L)
    val RootTunStatusNotification = PollingTimerSpec("root_tun_status_notification", 1_000L, 0L)
    val ProxyTileRefresh = PollingTimerSpec("proxy_tile_refresh", 1_000L, 0L)
    val HomeIpRefresh = PollingTimerSpec("home_ip_refresh", 15_000L, 0L)
    val TrafficStatsCollection = PollingTimerSpec("traffic_stats_collection", 5_000L, 0L)
    val ProxyHealthcheckRefresh = PollingTimerSpec("proxy_healthcheck_refresh", 1_500L, 1_500L)
    fun dynamic(
        name: String,
        intervalMillis: Long,
        initialDelayMillis: Long = intervalMillis,
    ): PollingTimerSpec {
        return PollingTimerSpec(
            name = "dynamic_$name",
            intervalMillis = intervalMillis,
            initialDelayMillis = initialDelayMillis,
        )
    }
}

object PollingTimers {
    private const val STOP_TIMEOUT_MILLIS = 5_000L

    // One lightweight scheduler lane for all periodic tick emission in this process.
    private val schedulerScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default.limitedParallelism(1),
    )
    private data class TimerKey(val intervalMillis: Long, val initialDelayMillis: Long)
    private val tickerCache = ConcurrentHashMap<TimerKey, SharedFlow<Long>>()

    fun ticks(spec: PollingTimerSpec): Flow<Long> {
        val key = TimerKey(spec.intervalMillis, spec.initialDelayMillis)
        return tickerCache.computeIfAbsent(key) {
            flow {
                if (spec.initialDelayMillis > 0L) {
                    delay(spec.initialDelayMillis)
                }
                while (currentCoroutineContext().isActive) {
                    emit(SystemClock.elapsedRealtime())
                    delay(spec.intervalMillis)
                }
            }.shareIn(
                scope = schedulerScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_TIMEOUT_MILLIS),
                replay = 0,
            )
        }
    }
}

@kotlinx.coroutines.ExperimentalCoroutinesApi
fun Flow<Long>.throttleWhenScreenOff(
    screenOn: StateFlow<Boolean>,
    slowIntervalMs: Long = 5_000L,
): Flow<Long> = screenOn.transformLatest { isScreenOn ->
    if (isScreenOn) {
        this@throttleWhenScreenOff.collect { emit(it) }
    } else {
        while (true) {
            emit(SystemClock.elapsedRealtime())
            delay(slowIntervalMs)
        }
    }
}
