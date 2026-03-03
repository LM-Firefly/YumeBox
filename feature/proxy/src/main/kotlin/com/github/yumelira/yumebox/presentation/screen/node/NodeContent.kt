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

package com.github.yumelira.yumebox.presentation.screen.node

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.domain.model.ProxyDisplayMode
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.domain.model.normalizeProxySheetHeightFraction
import com.github.yumelira.yumebox.presentation.component.LocalTopBarHazeState
import com.github.yumelira.yumebox.presentation.component.LocalTopBarHazeStyle
import com.github.yumelira.yumebox.presentation.util.usePullToRefreshTesting
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollHorizontal
import top.yukonga.miuix.kmp.utils.overScrollVertical

val NodeSheetContentPadding = PaddingValues(
    start = 0.dp,
    end = 0.dp,
    top = 8.dp,
    bottom = 16.dp,
)

private fun Modifier.nodeTabHaze(state: HazeState?, style: HazeStyle?): Modifier {
    if (state == null || style == null) return this
    return hazeEffect(state) {
        this.style = style
        blurRadius = 30.dp
        noiseFactor = 0f
        progressive = HazeProgressive.verticalGradient(
            startIntensity = 1f,
            endIntensity = 0f,
            preferPerformance = true,
        )
    }
}

@Composable
internal fun NodeTabs(
    groups: List<ProxyGroupInfo>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val hazeState = LocalTopBarHazeState.current
    val hazeStyle = LocalTopBarHazeStyle.current
    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex, groups.size) {
        if (groups.isEmpty()) return@LaunchedEffect
        val target = (selectedIndex - 1).coerceAtLeast(0).coerceAtMost(groups.lastIndex)
        if (target != listState.firstVisibleItemIndex) {
            listState.animateScrollToItem(target)
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .nodeTabHaze(hazeState, hazeStyle)
            .background(MiuixTheme.colorScheme.surface)
            .overScrollHorizontal(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        overscrollEffect = null,
    ) {
        itemsIndexed(groups, key = { _, group -> group.name }) { index, group ->
            val selected = index == selectedIndex
            val background = if (selected) {
                MiuixTheme.colorScheme.primary
            } else {
                MiuixTheme.colorScheme.surface
            }
            val textColor = if (selected) {
                MiuixTheme.colorScheme.onPrimary
            } else {
                MiuixTheme.colorScheme.onSurface
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(background)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(index) },
                    )
                    .padding(horizontal = 11.dp, vertical = 6.dp),
            ) {
                Text(
                    text = group.name,
                    color = textColor,
                    style = MiuixTheme.textStyles.footnote1,
                )
            }
        }
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
internal fun rememberNodeSheetHeight(sheetHeightFraction: Float): Dp {
    val normalized = normalizeProxySheetHeightFraction(sheetHeightFraction)
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    return remember(screenHeightDp, normalized) { screenHeightDp.dp * normalized }
}

@Composable
internal fun NodeGroupSheetContent(
    groups: List<ProxyGroupInfo>,
    testingGroupNames: Set<String>,
    sheetHeightFraction: Float,
    onGroupClick: (ProxyGroupInfo) -> Unit,
) {
    val sheetHeight = rememberNodeSheetHeight(sheetHeightFraction)

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(sheetHeight)
            .overScrollVertical(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = NodeSheetContentPadding,
        overscrollEffect = null,
    ) {
        nodeGroupItems(
            groups = groups,
            onGroupClick = onGroupClick,
            testingGroupNames = testingGroupNames,
            onGroupBoundsChanged = null,
            itemVerticalPadding = 0.dp,
        )
    }
}

@Composable
fun NodeSheetContent(
    group: ProxyGroupInfo,
    displayMode: ProxyDisplayMode,
    onSelectProxy: (String) -> Unit,
    isDelayTesting: Boolean,
    onTestDelay: () -> Unit,
    sheetHeightFraction: Float,
) {
    val sheetHeight = rememberNodeSheetHeight(sheetHeightFraction)
    val refreshTexts = remember {
        listOf(
            MLang.Proxy.PullToRefresh.PullToTestCurrentGroup,
            MLang.Proxy.PullToRefresh.ReleaseToTestCurrentGroup,
            MLang.Proxy.PullToRefresh.TestingCurrentGroup,
            MLang.Proxy.Testing.RequestSent,
        )
    }
    val pullToRefreshState = rememberPullToRefreshState()
    val (pullRefreshing, pullRefreshObservedTesting) = usePullToRefreshTesting(setOf(group.name))

    PullToRefresh(
        modifier = Modifier
            .fillMaxWidth()
            .height(sheetHeight),
        isRefreshing = pullRefreshing.value,
        onRefresh = {
            if (pullRefreshing.value) return@PullToRefresh
            pullRefreshObservedTesting.value = false
            pullRefreshing.value = true
            onTestDelay()
        },
        pullToRefreshState = pullToRefreshState,
        refreshTexts = refreshTexts,
    ) {
        NodeList(
            group = group,
            displayMode = displayMode,
            onProxyClick = { proxyName ->
                if (group.type == Proxy.Type.Selector) {
                    onSelectProxy(proxyName)
                } else {
                    onTestDelay()
                }
            },
            isDelayTesting = isDelayTesting,
            onTestDelay = onTestDelay,
            listStateKeyPrefix = "node_sheet:${group.name}",
            contentPadding = NodeSheetContentPadding,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
internal fun NodeList(
    group: ProxyGroupInfo,
    displayMode: ProxyDisplayMode,
    onProxyClick: (String) -> Unit,
    isDelayTesting: Boolean,
    onTestDelay: () -> Unit,
    listStateKeyPrefix: String,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    NodeGrid(
        proxies = group.proxies,
        selectedProxyName = group.now,
        displayMode = displayMode,
        onProxyClick = onProxyClick,
        isDelayTesting = isDelayTesting,
        onDelayTestClick = onTestDelay,
        listStateKey = "$listStateKeyPrefix:${group.name}",
        contentPadding = contentPadding,
        modifier = modifier,
    )
}
