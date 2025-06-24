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

package com.github.yumelira.yumebox.data.repository

import com.github.yumelira.yumebox.data.model.DailyTrafficSummary
import com.github.yumelira.yumebox.data.model.TrafficSlotData
import com.github.yumelira.yumebox.data.store.TrafficStatisticsStore
import kotlinx.coroutines.flow.StateFlow

class TrafficStatisticsRepository(
    private val store: TrafficStatisticsStore,
) {
    val dailySummaries: StateFlow<Map<Long, DailyTrafficSummary>> = store.dailySummaries

    fun getTodaySummary(): DailyTrafficSummary = store.getTodaySummary()
    fun getYesterdaySummary(): DailyTrafficSummary = store.getYesterdaySummary()
    fun getDailySummaries(days: Int): List<DailyTrafficSummary> = store.getDailySummaries(days)
    fun getTodayHourlyData(): List<TrafficSlotData> = store.getTodayHourlyData()
}
