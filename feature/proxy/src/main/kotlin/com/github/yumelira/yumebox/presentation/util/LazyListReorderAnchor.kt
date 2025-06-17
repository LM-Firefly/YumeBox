package com.github.yumelira.yumebox.presentation.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.abs

@Stable
private data class LazyListTopAnchor(
    val key: String,
    val offset: Int,
)

@Composable
fun KeepLazyListTopAnchorOnReorder(
    listState: LazyListState,
    itemKeys: List<String>,
    enabled: Boolean,
) {
    var pendingAnchor by remember(listState) { mutableStateOf<LazyListTopAnchor?>(null) }

    DisposableEffect(listState, itemKeys, enabled) {
        onDispose {
            if (!enabled || itemKeys.isEmpty()) return@onDispose
            pendingAnchor = listState.captureTopAnchor()
        }
    }

    LaunchedEffect(itemKeys, enabled) {
        if (!enabled || itemKeys.isEmpty()) {
            pendingAnchor = null
            return@LaunchedEffect
        }
        val currentAnchor = pendingAnchor ?: return@LaunchedEffect
        val targetIndex = itemKeys.indexOf(currentAnchor.key)
        if (targetIndex < 0) {
            pendingAnchor = null
            return@LaunchedEffect
        }

        val topNow = listState.captureTopAnchor()
        if (topNow != null &&
            topNow.key == currentAnchor.key &&
            abs(topNow.offset - currentAnchor.offset) <= 1
        ) {
            pendingAnchor = null
            return@LaunchedEffect
        }

        listState.scrollToItem(targetIndex, currentAnchor.offset)
        pendingAnchor = null
    }
}

private fun LazyListState.captureTopAnchor(): LazyListTopAnchor? {
    val layoutInfo = layoutInfo
    val topItem = layoutInfo.visibleItemsInfo.firstOrNull() ?: return null
    val key = topItem.key as? String ?: return null
    val offset = (topItem.offset - layoutInfo.viewportStartOffset).coerceAtLeast(0)
    return LazyListTopAnchor(key = key, offset = offset)
}
