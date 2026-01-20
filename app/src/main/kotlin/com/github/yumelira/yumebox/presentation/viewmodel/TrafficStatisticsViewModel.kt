package com.github.yumelira.yumebox.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.data.model.DailyTrafficSummary
import com.github.yumelira.yumebox.data.model.ProfileTrafficUsage
import com.github.yumelira.yumebox.data.model.StatisticsTimeRange
import com.github.yumelira.yumebox.data.model.TimeSlot
import com.github.yumelira.yumebox.data.store.TrafficStatisticsStore
import com.github.yumelira.yumebox.presentation.component.BarChartItem
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

class TrafficStatisticsViewModel(
    application: Application,
    private val trafficStatisticsStore: TrafficStatisticsStore
) : AndroidViewModel(application) {

    private val _selectedTimeRange = MutableStateFlow(StatisticsTimeRange.TODAY)
    val selectedTimeRange: StateFlow<StatisticsTimeRange> = _selectedTimeRange.asStateFlow()

    private val _selectedBarIndex = MutableStateFlow(-1)
    val selectedBarIndex: StateFlow<Int> = _selectedBarIndex.asStateFlow()

    val todaySummary: StateFlow<DailyTrafficSummary> = trafficStatisticsStore.dailySummaries
        .map { trafficStatisticsStore.getTodaySummary() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyTrafficSummary.EMPTY)

    val yesterdaySummary: StateFlow<DailyTrafficSummary> = trafficStatisticsStore.dailySummaries
        .map { trafficStatisticsStore.getYesterdaySummary() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyTrafficSummary.EMPTY)

    val weekSummary: StateFlow<Long> = trafficStatisticsStore.dailySummaries
        .map {
            trafficStatisticsStore.getDailySummaries(14).sumOf { it.total }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val trafficDifference: StateFlow<Long> = combine(todaySummary, yesterdaySummary) { today, yesterday ->
        today.total - yesterday.total
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val chartItems: StateFlow<List<BarChartItem>> = combine(
        _selectedTimeRange,
        trafficStatisticsStore.dailySummaries
    ) { timeRange, _ ->
        when (timeRange) {
            StatisticsTimeRange.TODAY -> getTodayHourlyChartItems()
            StatisticsTimeRange.WEEK -> getDailyChartItems(14)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profileUsages: StateFlow<List<ProfileTrafficUsage>> = trafficStatisticsStore.profileUsages
        .map { usages ->
            usages.values
                .sortedByDescending { it.totalBytes }
                .take(10)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTimeRange(range: StatisticsTimeRange) {
        _selectedTimeRange.value = range
        _selectedBarIndex.value = -1
    }

    fun setSelectedBarIndex(index: Int) {
        _selectedBarIndex.value = index
    }

    private fun getTodayHourlyChartItems(): List<BarChartItem> {
        val hourlyData = trafficStatisticsStore.getTodayHourlyData()
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val currentSlot = TimeSlot.fromHour(currentHour)

        return TimeSlot.entries.map { slot ->
            val slotData = hourlyData.getOrNull(slot.ordinal)
            BarChartItem(
                label = slot.label,
                upload = slotData?.upload ?: 0L,
                download = slotData?.download ?: 0L,
                isHighlighted = slot == currentSlot
            )
        }
    }

    private fun getDailyChartItems(days: Int): List<BarChartItem> {
        val summaries = trafficStatisticsStore.getDailySummaries(days)
        val dateFormat = SimpleDateFormat("M/d", Locale.getDefault())
        val todayKey = getDayKey(Calendar.getInstance())

        return summaries.map { summary ->
            val calendar = Calendar.getInstance().apply { timeInMillis = summary.dateMillis }
            val label = if (summary.dateMillis == todayKey) {
                MLang.TrafficStatistics.TimeRange.Today
            } else {
                dateFormat.format(calendar.time)
            }
            BarChartItem(
                label = label,
                upload = summary.totalUpload,
                download = summary.totalDownload,
                isHighlighted = summary.dateMillis == todayKey
            )
        }
    }

    private fun getDayKey(calendar: Calendar): Long {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
