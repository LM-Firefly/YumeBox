package com.github.yumelira.yumebox.presentation.screen.node

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.domain.model.ProxyDisplayMode
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.presentation.component.LocalTopBarHazeState
import com.github.yumelira.yumebox.presentation.component.LocalTopBarHazeStyle
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    val hazeEnabled = hazeState != null && hazeStyle != null
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
            .background(MiuixTheme.colorScheme.surface),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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

@Composable
fun NodeSheetContent(
    group: ProxyGroupInfo,
    displayMode: ProxyDisplayMode,
    onSelectProxy: (String) -> Unit,
    isDelayTesting: Boolean,
    onTestDelay: () -> Unit,
) {
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val (minSheetHeight, maxSheetHeight) = remember(screenHeightDp) {
        val screenHeight = screenHeightDp.dp
        val minHeight = (screenHeight * 0.42f).coerceAtLeast(280.dp).coerceAtMost(380.dp)
        val maxHeight = (screenHeight * 0.72f).coerceAtLeast(420.dp).coerceAtMost(620.dp)
        minHeight to maxHeight
    }

    val shouldShowLoading = remember(group.proxies.size) {
        group.proxies.size > 10
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .let { base ->
                if (shouldShowLoading) {
                    base.height(maxSheetHeight)
                } else {
                    base.heightIn(min = minSheetHeight, max = maxSheetHeight)
                }
            },
        contentAlignment = Alignment.Center,
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
            listStateKeyPrefix = "node_sheet",
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
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
    val shouldShowLoading = remember(group.proxies.size) {
        group.proxies.size > 10
    }
    var showContent by remember(group.name, shouldShowLoading) {
        mutableStateOf(!shouldShowLoading)
    }

    LaunchedEffect(group.name, shouldShowLoading) {
        if (!shouldShowLoading) {
            showContent = true
            return@LaunchedEffect
        }
        showContent = false
        delay(450)
        showContent = true
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (!showContent && shouldShowLoading) {
            InfiniteProgressIndicator()
        } else {
            NodeGrid(
                proxies = group.proxies,
                selectedProxyName = group.now,
                displayMode = displayMode,
                onProxyClick = onProxyClick,
                isDelayTesting = isDelayTesting,
                onDelayTestClick = onTestDelay,
                listStateKey = "$listStateKeyPrefix:${group.name}",
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
