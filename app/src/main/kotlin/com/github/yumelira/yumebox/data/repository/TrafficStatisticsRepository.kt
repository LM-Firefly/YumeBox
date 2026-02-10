package com.github.yumelira.yumebox.data.repository

import com.github.yumelira.yumebox.data.model.DailyTrafficSummary
import com.github.yumelira.yumebox.data.model.TrafficSlotData
import com.github.yumelira.yumebox.data.model.ProfileTrafficUsage
import com.github.yumelira.yumebox.data.store.TrafficStatisticsStore
import kotlinx.coroutines.flow.StateFlow

class TrafficStatisticsRepository(
    private val store: TrafficStatisticsStore,
) {
    val dailySummaries: StateFlow<Map<Long, DailyTrafficSummary>> = store.dailySummaries
    val profileUsages: StateFlow<Map<String, ProfileTrafficUsage>> = store.profileUsages

    fun getTodaySummary(): DailyTrafficSummary = store.getTodaySummary()
    fun getYesterdaySummary(): DailyTrafficSummary = store.getYesterdaySummary()
    fun getDailySummaries(days: Int): List<DailyTrafficSummary> = store.getDailySummaries(days)
    fun getTodayHourlyData(): List<TrafficSlotData> = store.getTodayHourlyData()
}
