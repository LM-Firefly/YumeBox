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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquid
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.max

val LocalPagerState = compositionLocalOf<PagerState> { error("LocalPagerState is not provided") }
val LocalHandlePageChange = compositionLocalOf<(Int) -> Unit> { error("LocalHandlePageChange is not provided") }
val LocalNavigator = compositionLocalOf<DestinationsNavigator> { error("LocalNavigator is not provided") }
val LocalBottomBarLiquidState = compositionLocalOf<LiquidState?> { null }

@Composable
fun BottomBarContent(
    isVisible: Boolean = true,
) {
    val bottomBarScrollBehavior = LocalBottomBarScrollBehavior.current
    val liquidState = LocalBottomBarLiquidState.current
    val pagerState = LocalPagerState.current
    val page by remember(pagerState) {
        derivedStateOf { if (pagerState.isScrollInProgress) pagerState.targetPage else pagerState.currentPage }
    }
    val bottomBarVisible = isVisible && (bottomBarScrollBehavior?.isBottomBarVisible ?: true)
    val animatedScale by animateFloatAsState(
        targetValue = if (bottomBarVisible) 1f else AnimationSpecs.Proxy.VisibilityTargetScale,
        animationSpec = tween(
            durationMillis = AnimationSpecs.Proxy.VisibilityDuration,
            easing = AnimationSpecs.EmphasizedDecelerate,
        ),
        label = "bottom_bar_scale",
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (bottomBarVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = AnimationSpecs.Proxy.VisibilityFadeDuration,
            easing = AnimationSpecs.EmphasizedDecelerate,
        ),
        label = "bottom_bar_alpha",
    )
    val handlePageChange = LocalHandlePageChange.current
    val onItemClick: (Int) -> Unit = onItemClick@{ index ->
        if (index == pagerState.currentPage && !pagerState.isScrollInProgress) return@onItemClick
        handlePageChange(index)
    }

    val density = LocalDensity.current
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
        tabsCount = BottomBarDestination.entries.size,
        liquidState = liquidState,
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
    val label: String,
    val icon: ImageVector,
) {
    Home(MLang.Component.BottomBar.Home, Yume.House),
    Proxy(MLang.Component.BottomBar.Proxy, Yume.`Arrow-down-up`),
    Config(MLang.Component.BottomBar.Config, Yume.`Package-check`),
    Setting(MLang.Component.BottomBar.Setting, Yume.Bolt),
}

@Composable
private fun BottomNavigationBar(
    selectedIndex: Int,
    tabsCount: Int,
    liquidState: LiquidState?,
    containerColor: Color,
    indicatorContainerColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val isLightTheme = !isSystemInDarkTheme()
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val density = LocalDensity.current
    val safeSelectedIndex = selectedIndex.coerceIn(0, tabsCount - 1)

    val contentInset = 4.dp
    val contentInsetPx = with(density) { (contentInset * 2).toPx() }

    var indicatorIndex by remember { mutableIntStateOf(safeSelectedIndex) }
    val indicatorPosition = remember { Animatable(safeSelectedIndex.toFloat()) }
    val indicatorScale = remember { Animatable(1f) }

    LaunchedEffect(safeSelectedIndex) {
        if (indicatorIndex == safeSelectedIndex) return@LaunchedEffect
        launch {
            indicatorPosition.animateTo(
                targetValue = safeSelectedIndex.toFloat(),
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            )
        }
        launch {
            indicatorScale.animateTo(0.88f, tween(90, easing = FastOutSlowInEasing))
            indicatorScale.animateTo(1f, tween(170, easing = FastOutSlowInEasing))
        }
        indicatorIndex = safeSelectedIndex
    }

    BoxWithConstraints(
        modifier = modifier
            .height(56.dp)
            .clip(Capsule())
            .then(
                if (liquidState != null) {
                    Modifier
                        .liquid(liquidState) {
                            frost = 3.dp
                            refraction = 0.5f
                            curve = 0.6f
                            edge = 0.4f
                            dispersion = 0f
                            saturation = 1.2f
                            contrast = 1.1f
                        }
                        .background( if (isLightTheme) White.copy(alpha = 0.75f) else Black.copy(alpha = 0.8f), Capsule())
                } else {
                    Modifier.background(containerColor, Capsule())
                }
            ),
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
                            if (isLtr) indicatorPosition.value * tabWidth
                            else size.width - (indicatorPosition.value + 1f) * tabWidth
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
