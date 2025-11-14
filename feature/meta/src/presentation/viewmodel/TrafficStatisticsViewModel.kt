package com.github.yumelira.yumebox.feature.meta.presentation.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.data.model.AppTrafficUsage
import com.github.yumelira.yumebox.data.model.DailyTrafficSummary
import com.github.yumelira.yumebox.data.model.StatisticsTimeRange
import com.github.yumelira.yumebox.data.model.TrafficStatisticsBuckets
import com.github.yumelira.yumebox.data.store.TrafficStatisticsStore
import com.github.yumelira.yumebox.presentation.component.TrafficDonutSlice
import com.github.yumelira.yumebox.runtime.client.AppIdentityResolver
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrafficStatisticsViewModel(
    private val trafficStatisticsStore: TrafficStatisticsStore,
) : ViewModel() {
    private val selectedTimeRange = MutableStateFlow(StatisticsTimeRange.TODAY)

    val uiState: StateFlow<TrafficStatisticsUiState> = combine(
        selectedTimeRange,
        trafficStatisticsStore.dailyAppSummaries,
    ) { range, _ ->
        val topApps = trafficStatisticsStore.getAppUsagesSorted(range)
        val totalUpload = topApps.sumOf(AppTrafficUsage::totalUpload)
        val totalDownload = topApps.sumOf(AppTrafficUsage::totalDownload)

        TrafficStatisticsUiState(
            selectedTimeRange = range,
            summary = DailyTrafficSummary(
                dateMillis = range.days.toLong(),
                totalUpload = totalUpload,
                totalDownload = totalDownload,
            ),
            topApps = topApps,
            donutSlices = buildDonutSlices(topApps),
        )
    }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrafficStatisticsUiState())

    fun setTimeRange(range: StatisticsTimeRange) {
        selectedTimeRange.value = range
    }

    fun clearAllStatistics() {
        viewModelScope.launch {
            trafficStatisticsStore.clearAll()
        }
    }

    private fun buildDonutSlices(apps: List<AppTrafficUsage>): List<TrafficDonutSlice> {
        if (apps.isEmpty()) return emptyList()

        val unknown = apps.firstOrNull { it.appKey == AppIdentityResolver.UNKNOWN_APP_KEY }
        val unattributed = apps.firstOrNull { it.appKey == TrafficStatisticsBuckets.UNATTRIBUTED_APP_KEY }
        val regularApps = apps.filterNot {
            it.appKey == AppIdentityResolver.UNKNOWN_APP_KEY ||
                it.appKey == TrafficStatisticsBuckets.UNATTRIBUTED_APP_KEY
        }
        val primaryApps = regularApps.take(MAX_DONUT_PRIMARY_APPS)
        val overflowBytes = regularApps.drop(MAX_DONUT_PRIMARY_APPS).sumOf(AppTrafficUsage::totalBytes)

        return buildList {
            primaryApps.forEach { usage ->
                add(
                    TrafficDonutSlice(
                        key = usage.appKey,
                        label = usage.appName,
                        value = usage.totalBytes,
                        color = colorForAppKey(usage.appKey),
                    ),
                )
            }

            if (overflowBytes > 0L) {
                add(
                    TrafficDonutSlice(
                        key = OTHER_SLICE_KEY,
                        label = MLang.TrafficStatistics.Donut.Other,
                        value = overflowBytes,
                        color = Color(0xFF94A3B8),
                    ),
                )
            }

            unattributed?.takeIf { it.totalBytes > 0L }?.let { usage ->
                add(
                    TrafficDonutSlice(
                        key = usage.appKey,
                        label = usage.appName,
                        value = usage.totalBytes,
                        color = Color(0xFFD97706),
                    ),
                )
            }

            unknown?.takeIf { it.totalBytes > 0L }?.let { usage ->
                add(
                    TrafficDonutSlice(
                        key = usage.appKey,
                        label = usage.appName,
                        value = usage.totalBytes,
                        color = Color(0xFF64748B),
                    ),
                )
            }
        }
    }

    private fun colorForAppKey(appKey: String): Color {
        val hue = ((appKey.hashCode().toLong() and 0xFFFFFFFFL) % 360L).toFloat()
        return Color.hsv(
            hue = hue,
            saturation = 0.62f,
            value = 0.88f,
        )
    }

    companion object {
        const val OTHER_SLICE_KEY = "other"
        private const val MAX_DONUT_PRIMARY_APPS = 5
    }
}

data class TrafficStatisticsUiState(
    val selectedTimeRange: StatisticsTimeRange = StatisticsTimeRange.TODAY,
    val summary: DailyTrafficSummary = DailyTrafficSummary.EMPTY,
    val topApps: List<AppTrafficUsage> = emptyList(),
    val donutSlices: List<TrafficDonutSlice> = emptyList(),
)
