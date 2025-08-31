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



package com.github.yumelira.yumebox.feature.meta.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.data.model.DailyTrafficSummary
import com.github.yumelira.yumebox.data.model.StatisticsTimeRange
import com.github.yumelira.yumebox.data.model.TimeSlot
import com.github.yumelira.yumebox.data.store.TrafficStatisticsStore
import com.github.yumelira.yumebox.presentation.component.BarChartItem
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

class TrafficStatisticsViewModel(
    private val trafficStatisticsStore: TrafficStatisticsStore,
) : ViewModel() {

    private val _selectedTimeRange = MutableStateFlow(StatisticsTimeRange.TODAY)
    val selectedTimeRange: StateFlow<StatisticsTimeRange> = _selectedTimeRange.asStateFlow()

    private val _selectedBarIndex = MutableStateFlow(-1)
    val selectedBarIndex: StateFlow<Int> = _selectedBarIndex.asStateFlow()

    val todaySummary: StateFlow<DailyTrafficSummary> = flow {
        emit(trafficStatisticsStore.getTodaySummary())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyTrafficSummary.EMPTY)

    val yesterdaySummary: StateFlow<DailyTrafficSummary> = flow {
        emit(trafficStatisticsStore.getYesterdaySummary())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyTrafficSummary.EMPTY)

    val weekSummary: StateFlow<Long> = flow {
        val summaries = trafficStatisticsStore.getDailySummaries(7)
        emit(summaries.sumOf { it.total })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val trafficDifference: StateFlow<Long> = combine(
        todaySummary,
        yesterdaySummary
    ) { today, yesterday ->
        today.total - yesterday.total
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val chartItems: StateFlow<List<BarChartItem>> = combine(
        _selectedTimeRange,
        todaySummary
    ) { timeRange, _ ->
        when (timeRange) {
            StatisticsTimeRange.TODAY -> getTodayHourlyChartItems()
            StatisticsTimeRange.WEEK -> getDailyChartItems()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

        return hourlyData.mapIndexed { index, slotData ->
            val slot = TimeSlot.entries[index]
            BarChartItem(
                label = slot.label,
                value = slotData.total,
                isHighlighted = slot == currentSlot
            )
        }
    }

    private fun getDailyChartItems(): List<BarChartItem> {
        val summaries = trafficStatisticsStore.getDailySummaries(7)
        val dateFormat = SimpleDateFormat("M/d", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val todayKey = getDayKey(calendar)

        return summaries.map { summary ->
            calendar.timeInMillis = summary.dateMillis
            val label = if (summary.dateMillis == todayKey) {
                MLang.TrafficStatistics.TimeRange.Today
            } else {
                dateFormat.format(calendar.time)
            }
            BarChartItem(
                label = label,
                value = summary.total,
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
