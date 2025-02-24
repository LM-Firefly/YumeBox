package com.github.yumelira.yumebox.presentation.screen.node

import androidx.compose.ui.geometry.Rect
import com.github.yumelira.yumebox.domain.model.ProxySortMode

const val NODE_BOUNDS_UNSET = -1f

internal val NodeSortModes = listOf(
    ProxySortMode.DEFAULT,
    ProxySortMode.BY_NAME,
    ProxySortMode.BY_LATENCY,
)

internal fun nodeRectFromArgs(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
): Rect? {
    if (left < 0f || top < 0f || right < 0f || bottom < 0f) return null
    if (right <= left || bottom <= top) return null
    return Rect(left, top, right, bottom)
}
