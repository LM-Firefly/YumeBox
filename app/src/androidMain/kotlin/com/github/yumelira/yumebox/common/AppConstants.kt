package com.github.yumelira.yumebox.common

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object AppConstants {

    object Timing {
        const val AUTO_START_DELAY_MS = 1500L
        const val HEALTH_CHECK_WAIT_MS = 2000L
        const val PROXY_REFRESH_DELAY_MS = 500L
        const val SELECTION_APPLY_DELAY_MS = 300L
        const val NOTIFICATION_DISMISS_DELAY_MS = 3000L
        const val IP_REFRESH_INTERVAL_MS = 10000L
        const val SELECTION_RESTORE_DELAY_MS = 300L
        const val PROFILE_RELOAD_DELAY_MS = 1000L
        const val SPEED_SAMPLE_INTERVAL_MS = 1000L
    }

    object UI {
        val TRAFFIC_FONT_SIZE = 96.sp
        val TRAFFIC_LETTER_SPACING = (-3).sp
        val TRAFFIC_UNIT_FONT_SIZE = 24.sp
        val QUOTE_FONT_SIZE = 32.sp
        val QUOTE_LINE_HEIGHT = 48.sp
        val AUTHOR_FONT_SIZE = 18.sp
        val CARD_CORNER_RADIUS = 12.dp
        val BUTTON_CORNER_RADIUS = 32.dp
        val DEFAULT_HORIZONTAL_PADDING = 24.dp
        val DEFAULT_VERTICAL_SPACING = 24.dp
        val SPEED_CHART_HEIGHT = 100.dp
    }

    object Limits {
        const val MAX_LOG_ENTRIES = 50
        const val SPEED_HISTORY_SIZE = 24
        const val MAX_CONCURRENT_TESTS = 5
    }
}
