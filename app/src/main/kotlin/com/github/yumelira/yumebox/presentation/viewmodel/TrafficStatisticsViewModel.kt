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
 * Copyright (c)  YumeLira 2025.
 *
 */

package com.github.yumelira.yumebox.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.data.model.DailyTrafficSummary
import com.github.yumelira.yumebox.data.model.ProfileTrafficUsage
import com.github.yumelira.yumebox.data.model.StatisticsTimeRange
import com.github.yumelira.yumebox.data.model.TimeSlot
import com.github.yumelira.yumebox.domain.facade.RuntimeFacade
import com.github.yumelira.yumebox.presentation.component.BarChartItem
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

class TrafficStatisticsViewModel(
    application: Application,
    private val runtimeFacade: RuntimeFacade
) : AndroidViewModel(application) {

    private val _selectedTimeRange = MutableStateFlow(StatisticsTimeRange.TODAY)
    val selectedTimeRange: StateFlow<StatisticsTimeRange> = _selectedTimeRange.asStateFlow()

    private val _selectedBarIndex = MutableStateFlow(-1)
    val selectedBarIndex: StateFlow<Int> = _selectedBarIndex.asStateFlow()

    val todaySummary: StateFlow<DailyTrafficSummary> = runtimeFacade.dailySummaries
        .map { runtimeFacade.getTodaySummary() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyTrafficSummary.EMPTY)

    val yesterdaySummary: StateFlow<DailyTrafficSummary> = runtimeFacade.dailySummaries
        .map { runtimeFacade.getYesterdaySummary() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyTrafficSummary.EMPTY)

    val weekSummary: StateFlow<Long> = runtimeFacade.dailySummaries
        .map {
            runtimeFacade.getDailySummaries(14).sumOf { it.total }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val trafficDifference: StateFlow<Long> = runtimeFacade.dailySummaries
        .map {
            val recentDays = runtimeFacade.getDailySummaries(2)
            if (recentDays.size >= 2) {
                val yesterday = recentDays[0]  // 昨天
                val today = recentDays[1]      // 今天
                today.total - yesterday.total
            } else if (recentDays.size == 1) {
                recentDays[0].total
            } else {
                0L
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val chartItems: StateFlow<List<BarChartItem>> = combine(
        _selectedTimeRange,
        runtimeFacade.dailySummaries
    ) { timeRange, _ ->
        when (timeRange) {
            StatisticsTimeRange.TODAY -> getTodayHourlyChartItems()
            StatisticsTimeRange.WEEK -> getDailyChartItems(14)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profileUsages: StateFlow<List<ProfileTrafficUsage>> = runtimeFacade.profileUsages
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
        val hourlyData = runtimeFacade.getTodayHourlyData()
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val currentSlot = TimeSlot.fromHour(currentHour)
        val currentSlotIndex = currentSlot.ordinal

        return TimeSlot.entries.mapIndexed { index, slot ->
            val slotData = if (index <= currentSlotIndex) {
                hourlyData.getOrNull(slot.ordinal)
            } else {
                null
            }
            BarChartItem(
                label = slot.label,
                upload = slotData?.upload ?: 0L,
                download = slotData?.download ?: 0L,
                isHighlighted = slot == currentSlot
            )
        }
    }

    private fun getDailyChartItems(days: Int): List<BarChartItem> {
        val summaries = runtimeFacade.getDailySummaries(days)
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
