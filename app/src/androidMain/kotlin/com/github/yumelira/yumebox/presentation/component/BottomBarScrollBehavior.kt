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

package com.github.yumelira.yumebox.presentation.component

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

/**
 * 底栏滚动行为管理类
 * 负责监听滚动事件并控制底栏的显示/隐藏
 */
@Stable
class BottomBarScrollBehavior {
    /**
     * 底栏是否可见
     */
    var isBottomBarVisible by mutableStateOf(true)
        private set

    /**
     * 是否启用自动隐藏功能
     */
    var isAutoHideEnabled by mutableStateOf(true)

    /**
     * 滚动阈值，超过此值才会触发底栏状态变化
     */
    private val scrollThreshold = 12f

    /**
     * 防抖延迟时间，防止过于频繁的状态切换
     */
    private var lastToggleTime = 0L
    private val toggleDelay = 150L // 150ms 防抖

    /**
     * 上一次的滚动偏移量
     */
    private var previousScrollOffset = 0f

    /**
     * 累积的滚动偏移量
     */
    private var accumulatedScroll = 0f

    /**
     * 嵌套滚动连接
     */
    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (!isAutoHideEnabled) return Offset.Zero

            // 处理垂直滚动
            val delta = available.y

            // 忽略过小的滚动
            if (kotlin.math.abs(delta) < 0.5f) return Offset.Zero

            // 累积滚动偏移
            accumulatedScroll += delta

            // 检查是否超过阈值
            if (kotlin.math.abs(accumulatedScroll) >= scrollThreshold) {
                // 向上滚动（delta < 0）隐藏底栏，向下滚动（delta > 0）显示底栏
                if (accumulatedScroll < 0) {
                    hideBottomBar()
                } else {
                    showBottomBar()
                }
                // 重置累积滚动
                accumulatedScroll = 0f
            }

            previousScrollOffset = delta
            return Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            // 如果还有未消费的滚动，也进行处理
            if (available.y != 0f && isAutoHideEnabled) {
                val delta = available.y
                accumulatedScroll += delta

                if (kotlin.math.abs(accumulatedScroll) >= scrollThreshold) {
                    if (accumulatedScroll < 0) {
                        hideBottomBar()
                    } else {
                        showBottomBar()
                    }
                    accumulatedScroll = 0f
                }
            }
            return Offset.Zero
        }
    }

    /**
     * 显示底栏
     */
    fun showBottomBar() {
        val currentTime = System.currentTimeMillis()
        if (!isBottomBarVisible && currentTime - lastToggleTime >= toggleDelay) {
            isBottomBarVisible = true
            lastToggleTime = currentTime
        }
    }

    /**
     * 隐藏底栏
     */
    fun hideBottomBar() {
        val currentTime = System.currentTimeMillis()
        if (isBottomBarVisible && currentTime - lastToggleTime >= toggleDelay) {
            isBottomBarVisible = false
            lastToggleTime = currentTime
        }
    }

    /**
     * 切换底栏可见性
     */
    fun toggleBottomBarVisibility() {
        isBottomBarVisible = !isBottomBarVisible
    }

    /**
     * 重置到底栏默认可见状态
     */
    fun reset() {
        isBottomBarVisible = true
        accumulatedScroll = 0f
        previousScrollOffset = 0f
        lastToggleTime = 0L
    }
}

/**
 * 创建并记住一个 BottomBarScrollBehavior 实例
 */
@Composable
fun rememberBottomBarScrollBehavior(
    autoHideEnabled: Boolean = true
): BottomBarScrollBehavior {
    return remember(autoHideEnabled) {
        BottomBarScrollBehavior().apply {
            isAutoHideEnabled = autoHideEnabled
        }
    }
}

/**
 * 监听 LazyListState 的滚动变化并更新底栏状态
 */
@Composable
fun BottomBarScrollBehavior.withLazyListState(
    listState: LazyListState
): BottomBarScrollBehavior {
    // 根据列表状态派生底栏可见性
    val isAtTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0
        }
    }

    // 当滚动到顶部时显示底栏
    LaunchedEffect(isAtTop) {
        if (isAtTop) {
            showBottomBar()
        }
    }

    // 监听滚动状态变化
    val isScrollingUp by remember {
        derivedStateOf {
            if (listState.layoutInfo.visibleItemsInfo.isNotEmpty()) {
                val firstVisibleItem = listState.layoutInfo.visibleItemsInfo.first()
                firstVisibleItem.index == 0 && firstVisibleItem.offset == 0
            } else false
        }
    }

    LaunchedEffect(isScrollingUp) {
        if (isScrollingUp) {
            showBottomBar()
        }
    }

    return this
}

/**
 * CompositionLocal 用于提供全局的 BottomBarScrollBehavior
 */
val LocalBottomBarScrollBehavior = compositionLocalOf<BottomBarScrollBehavior?> {
    null
}