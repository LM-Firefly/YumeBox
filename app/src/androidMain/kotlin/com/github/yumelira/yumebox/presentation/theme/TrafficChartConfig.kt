package com.github.yumelira.yumebox.presentation.theme

object TrafficChartConfig {
    const val BOUND_A = 0.5 * 1024 * 1024
    const val BOUND_B = 5.0 * 1024 * 1024
    const val BOUND_C = 40.0 * 1024 * 1024

    const val MIN_VISIBLE_HEIGHT = 0.02f

    const val DEFAULT_SAMPLE_LIMIT = 24

    fun calculateBarFraction(speedBytes: Long): Float {
        return when {
            speedBytes <= 0 -> MIN_VISIBLE_HEIGHT
            speedBytes < BOUND_A -> {
                val ratio = (speedBytes / BOUND_A).coerceIn(0.0, 1.0)
                (ratio * 0.4).toFloat().coerceAtLeast(MIN_VISIBLE_HEIGHT)
            }

            speedBytes < BOUND_B -> {
                val ratio = ((speedBytes - BOUND_A) / (BOUND_B - BOUND_A)).coerceIn(0.0, 1.0)
                (0.4 + ratio * 0.3).toFloat()
            }

            speedBytes < BOUND_C -> {
                val ratio = ((speedBytes - BOUND_B) / (BOUND_C - BOUND_B)).coerceIn(0.0, 1.0)
                (0.7 + ratio * 0.3).toFloat()
            }

            else -> 1.0f
        }
    }
}
