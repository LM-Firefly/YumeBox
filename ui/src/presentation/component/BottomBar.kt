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



package com.github.yumelira.yumebox.presentation.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.`Arrow-down-up`
import com.github.yumelira.yumebox.presentation.icon.yume.Bolt
import com.github.yumelira.yumebox.presentation.icon.yume.House
import com.github.yumelira.yumebox.presentation.icon.yume.`Package-check`
import com.github.yumelira.yumebox.presentation.theme.AnimationSpecs
import com.kyant.shapes.Capsule
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.max

class MainPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope,
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    var isNavigating by mutableStateOf(false)
        private set

    private var navJob: Job? = null

    fun animateToPage(targetIndex: Int) {
        if (targetIndex == selectedPage) return

        navJob?.cancel()
        selectedPage = targetIndex
        isNavigating = true

        val distance = abs(targetIndex - pagerState.currentPage).coerceAtLeast(2)
        val duration = 100 * distance + 100
        val layoutInfo = pagerState.layoutInfo
        val pageSize = layoutInfo.pageSize + layoutInfo.pageSpacing
        val currentDistanceInPages =
            targetIndex - pagerState.currentPage - pagerState.currentPageOffsetFraction
        val scrollPixels = currentDistanceInPages * pageSize

        navJob = coroutineScope.launch {
            val myJob = coroutineContext.job
            try {
                pagerState.animateScrollBy(
                    value = scrollPixels,
                    animationSpec = tween(
                        durationMillis = duration,
                        easing = EaseInOut,
                    ),
                )
            } finally {
                if (navJob == myJob) {
                    isNavigating = false
                    if (pagerState.currentPage != targetIndex) {
                        selectedPage = pagerState.currentPage
                    }
                }
            }
        }
    }

    fun syncPage() {
        if (!isNavigating && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }
}

@Composable
fun rememberMainPagerState(
    pagerState: PagerState,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): MainPagerState {
    return remember(pagerState, coroutineScope) {
        MainPagerState(pagerState, coroutineScope)
    }
}

val LocalPagerState = compositionLocalOf<PagerState> { error("LocalPagerState is not provided") }
val LocalMainPagerState = compositionLocalOf<MainPagerState> { error("LocalMainPagerState is not provided") }
val LocalHandlePageChange = compositionLocalOf<(Int) -> Unit> { error("LocalHandlePageChange is not provided") }
val LocalNavigator = compositionLocalOf<DestinationsNavigator> { error("LocalNavigator is not provided") }

@Composable
fun BottomBarContent(
    isVisible: Boolean = true,
) {
    val bottomBarScrollBehavior = LocalBottomBarScrollBehavior.current
    val mainPagerState = LocalMainPagerState.current
    val pagerState = mainPagerState.pagerState
    val page by remember(mainPagerState) {
        derivedStateOf { mainPagerState.selectedPage }
    }
    val indicatorProgress by remember(pagerState) {
        derivedStateOf {
            (
                pagerState.currentPage.toFloat() + pagerState.currentPageOffsetFraction
                ).coerceIn(0f, (BottomBarDestination.entries.size - 1).toFloat())
        }
    }
    val bottomBarVisible = isVisible && (bottomBarScrollBehavior?.isBottomBarVisible ?: true)
    val density = LocalDensity.current
    val enterOffsetPx = remember(density) { with(density) { 68.dp.toPx() } }
    val exitOffsetPx = remember(density) { with(density) { 84.dp.toPx() } }
    val animatedTranslationY = remember { Animatable(if (bottomBarVisible) 0f else exitOffsetPx) }
    val animatedScale by animateFloatAsState(
        targetValue = if (bottomBarVisible) 1f else 0.98f,
        animationSpec = tween(
            durationMillis = 240,
            easing = if (bottomBarVisible) {
                AnimationSpecs.EmphasizedDecelerate
            } else {
                AnimationSpecs.EmphasizedAccelerate
            },
        ),
        label = "bottom_bar_scale",
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (bottomBarVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 180,
            easing = if (bottomBarVisible) {
                AnimationSpecs.EmphasizedDecelerate
            } else {
                AnimationSpecs.EmphasizedAccelerate
            },
        ),
        label = "bottom_bar_alpha",
    )
    LaunchedEffect(bottomBarVisible, enterOffsetPx, exitOffsetPx) {
        if (bottomBarVisible) {
            animatedTranslationY.snapTo(enterOffsetPx)
            animatedTranslationY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = 520f,
                ),
            )
        } else {
            animatedTranslationY.animateTo(
                targetValue = exitOffsetPx,
                animationSpec = tween(
                    durationMillis = 220,
                    easing = AnimationSpecs.EmphasizedAccelerate,
                ),
            )
        }
    }
    val handlePageChange = LocalHandlePageChange.current
    val onItemClick: (Int) -> Unit = onItemClick@{ index ->
        if (index == mainPagerState.selectedPage) return@onItemClick
        handlePageChange(index)
    }

    val bottomSafeInset = with(density) {
        val navBottom = WindowInsets.navigationBars.getBottom(this)
        val gestureBottom = WindowInsets.systemGestures.getBottom(this)
        max(navBottom, gestureBottom).toDp()
    }

    val selectedColor = MiuixTheme.colorScheme.primary
    val unselectedColor = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val containerColor = MiuixTheme.colorScheme.background
    val indicatorContainerColor = selectedColor.copy(alpha = 0.1f)

    BottomNavigationBar(
        selectedIndex = page,
        indicatorProgress = indicatorProgress,
        tabsCount = BottomBarDestination.entries.size,
        containerColor = containerColor,
        indicatorContainerColor = indicatorContainerColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 48.dp,
                end = 48.dp,
                top = 6.dp,
                bottom = bottomSafeInset + 12.dp,
            )
            .graphicsLayer {
                alpha = animatedAlpha
                scaleX = animatedScale
                scaleY = animatedScale
                translationY = animatedTranslationY.value
                transformOrigin = TransformOrigin(0.5f, 1f)
            },
    ) {
        BottomBarDestination.entries.forEachIndexed { index, destination ->
            val itemColor: Color = if (page == index) selectedColor else unselectedColor
            BottomNavigationTabItem(
                enabled = bottomBarVisible,
                onClick = { onItemClick(index) },
            ) {
                Box(
                    modifier = Modifier.size(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label,
                        tint = itemColor
                    )
                }
                BasicText(
                    destination.label,
                    style = TextStyle(
                        color = itemColor,
                        fontSize = 11.sp,
                        fontWeight = if (page == index) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                )
            }
        }
    }
}

enum class BottomBarDestination(
    val icon: ImageVector,
) {
    Home(Yume.House),
    Proxy(Yume.`Arrow-down-up`),
    Config(Yume.`Package-check`),
    Setting(Yume.Bolt),
    ;

    val label: String
        get() = when (this) {
            Home -> MLang.Component.BottomBar.Home
            Proxy -> MLang.Component.BottomBar.Proxy
            Config -> MLang.Component.BottomBar.Config
            Setting -> MLang.Component.BottomBar.Setting
        }
}

@Composable
private fun BottomNavigationBar(
    selectedIndex: Int,
    indicatorProgress: Float,
    tabsCount: Int,
    containerColor: Color,
    indicatorContainerColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val isLightTheme = !isSystemInDarkTheme()
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val density = LocalDensity.current
    val safeSelectedIndex = selectedIndex.coerceIn(0, tabsCount - 1)
    val safeIndicatorProgress = indicatorProgress.coerceIn(0f, (tabsCount - 1).toFloat())

    val contentInset = 4.dp
    val contentInsetPx = with(density) { (contentInset * 2).toPx() }

    val indicatorScale = remember { Animatable(1f) }

    LaunchedEffect(safeSelectedIndex) {
        launch {
            indicatorScale.animateTo(0.9f, tween(120, easing = FastOutSlowInEasing))
            indicatorScale.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .height(56.dp)
            .clip(Capsule())
            .background(containerColor, Capsule()),
        contentAlignment = Alignment.CenterStart,
    ) {
        val tabWidth = (constraints.maxWidth.toFloat() - contentInsetPx) / tabsCount

        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(contentInset),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .graphicsLayer {
                        translationX =
                            if (isLtr) safeIndicatorProgress * tabWidth
                            else size.width - (safeIndicatorProgress + 1f) * tabWidth
                        scaleX = indicatorScale.value
                        scaleY = indicatorScale.value
                    }
                    .height(48.dp)
                    .fillMaxWidth(1f / tabsCount)
                    .background(indicatorContainerColor, Capsule()),
            )

            Row(
                Modifier
                    .height(48.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }

        Box(
            Modifier
                .matchParentSize()
                .border(
                    width = 0.3.dp,
                    color = if (isLightTheme) {
                        White.copy(alpha = 0.4f)
                    } else {
                        Black.copy(alpha = 0.2f)
                    },
                    shape = Capsule(),
                ),
        )

        Box(
            Modifier
                .matchParentSize()
                .padding(1.dp)
                .border(
                    width = 0.2.dp,
                    color = if (isLightTheme) {
                        Black.copy(alpha = 0.045f)
                    } else {
                        White.copy(alpha = 0.08f)
                    },
                    shape = Capsule(),
                ),
        )
    }
}

@Composable
private fun RowScope.BottomNavigationTabItem(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(Capsule())
            .clickable(
                enabled = enabled,
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .fillMaxHeight()
            .weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}
