package com.github.yumelira.yumebox.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TrafficRecord(
    val timestamp: Long,
    val upload: Long,
    val download: Long,
    val profileId: String? = null
) {
    val total: Long get() = upload + download
}

enum class TimeSlot(val startHour: Int, val endHour: Int, val label: String) {
    SLOT_0_2(0, 2, "0-2"),
    SLOT_2_4(2, 4, "2-4"),
    SLOT_4_6(4, 6, "4-6"),
    SLOT_6_8(6, 8, "6-8"),
    SLOT_8_10(8, 10, "8-10"),
    SLOT_10_12(10, 12, "10-12"),
    SLOT_12_14(12, 14, "12-14"),
    SLOT_14_16(14, 16, "14-16"),
    SLOT_16_18(16, 18, "16-18"),
    SLOT_18_20(18, 20, "18-20"),
    SLOT_20_22(20, 22, "20-22"),
    SLOT_22_24(22, 24, "22-24");

    companion object {
        fun fromHour(hour: Int): TimeSlot {
            return entries.first { hour >= it.startHour && hour < it.endHour }
        }
    }
}

@Serializable
data class DailyTrafficSummary(
    val dateMillis: Long,
    val totalUpload: Long,
    val totalDownload: Long,
    val hourlyData: Map<Int, TrafficSlotData> = emptyMap()
) {
    val total: Long get() = totalUpload + totalDownload

    companion object {
        val EMPTY = DailyTrafficSummary(0L, 0L, 0L)
    }
}

@Serializable
data class TrafficSlotData(
    val slotIndex: Int,
    val upload: Long,
    val download: Long
) {
    val total: Long get() = upload + download
}

@Serializable
data class ProfileTrafficUsage(
    val profileId: String,
    val profileName: String,
    val totalUpload: Long,
    val totalDownload: Long
) {
    val totalBytes: Long get() = totalUpload + totalDownload
}

enum class StatisticsTimeRange(val days: Int) {
    TODAY(1),
    WEEK(14);

    val label: String
        get() = when (this) {
            TODAY -> dev.oom_wg.purejoy.mlang.MLang.TrafficStatistics.TimeRange.Today
            WEEK -> dev.oom_wg.purejoy.mlang.MLang.TrafficStatistics.TimeRange.Week
        }
}

enum class ChartGranularity {
    HOURLY,
    DAILY;

    val label: String
        get() = when (this) {
            HOURLY -> dev.oom_wg.purejoy.mlang.MLang.TrafficStatistics.Chart.Hourly
            DAILY -> dev.oom_wg.purejoy.mlang.MLang.TrafficStatistics.Chart.Daily
        }
}
