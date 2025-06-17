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

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
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
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

val LocalPagerState = compositionLocalOf<PagerState> { error("LocalPagerState is not provided") }
val LocalHandlePageChange = compositionLocalOf<(Int) -> Unit> { error("LocalHandlePageChange is not provided") }
val LocalNavigator = compositionLocalOf<DestinationsNavigator> { error("LocalNavigator is not provided") }

@Composable
fun BottomBarContent(
    isVisible: Boolean = true,
) {
    val pagerState = LocalPagerState.current
    val page by remember(pagerState) {
        derivedStateOf { if (pagerState.isScrollInProgress) pagerState.targetPage else pagerState.currentPage }
    }
    val handlePageChange = LocalHandlePageChange.current
    val onItemClick: (Int) -> Unit = onItemClick@{ index ->
        if (index == pagerState.currentPage && !pagerState.isScrollInProgress) return@onItemClick
        handlePageChange(index)
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 200, easing = AnimationSpecs.EnterEasing),
        ) + slideInVertically(
            initialOffsetY = { (it / 5f).toInt() },
            animationSpec = tween(durationMillis = 300, easing = AnimationSpecs.EmphasizedDecelerate),
        ) + scaleIn(
            initialScale = 0.98f,
            animationSpec = tween(durationMillis = 300, easing = AnimationSpecs.EmphasizedDecelerate),
        ),
        exit = fadeOut(
            animationSpec = tween(durationMillis = 160, easing = AnimationSpecs.ExitEasing),
        ) + slideOutVertically(
            targetOffsetY = { (it / 6f).toInt() },
            animationSpec = tween(durationMillis = 240, easing = AnimationSpecs.EmphasizedAccelerate),
        ) + scaleOut(
            targetScale = 0.99f,
            animationSpec = tween(durationMillis = 240, easing = AnimationSpecs.EmphasizedAccelerate),
        ),
        label = "BottomBarVisibility"
    ) {
        val selectedColor = MiuixTheme.colorScheme.primary
        val unselectedColor = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        val containerColor = MiuixTheme.colorScheme.background
        val indicatorContainerColor = selectedColor.copy(alpha = 0.1f)

        BottomNavigationBar(
            selectedIndex = page,
            tabsCount = BottomBarDestination.entries.size,
            containerColor = containerColor,
            indicatorContainerColor = indicatorContainerColor,
            modifier = Modifier.padding(start = 48.dp, end = 48.dp, top = 6.dp, bottom = 30.dp),
        ) {
            BottomBarDestination.entries.forEachIndexed { index, destination ->
                val itemColor: Color = if (page == index) selectedColor else unselectedColor
                BottomNavigationTabItem(onClick = { onItemClick(index) }) {
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
                            fontSize = 11.sp
                        )
                    )
                }
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
    containerColor: Color,
    indicatorContainerColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val isLightTheme = !isSystemInDarkTheme()
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val safeSelectedIndex = selectedIndex.coerceIn(0, tabsCount - 1)

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
            .background(containerColor, Capsule())
            .border(
                width = 0.3.dp,
                color = if (isLightTheme) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.05f),
                shape = Capsule(),
            )
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val tabWidth = constraints.maxWidth.toFloat() / tabsCount

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
}

@Composable
private fun RowScope.BottomNavigationTabItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(Capsule())
            .clickable(
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
