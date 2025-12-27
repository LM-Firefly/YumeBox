package com.github.yumelira.yumebox.presentation.component

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.`Arrow-down-up`
import com.github.yumelira.yumebox.presentation.icon.yume.`Package-check`
import com.github.yumelira.yumebox.presentation.icon.yume.Bolt
import com.github.yumelira.yumebox.presentation.icon.yume.House
import com.github.yumelira.yumebox.presentation.viewmodel.AppSettingsViewModel
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.oom_wg.purejoy.mlang.MLang
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

val LocalPagerState = compositionLocalOf<PagerState> { error("LocalPagerState is not provided") }
val LocalHandlePageChange = compositionLocalOf<(Int) -> Unit> { error("LocalHandlePageChange is not provided") }
val LocalNavigator = compositionLocalOf<DestinationsNavigator> { error("LocalNavigator is not provided") }

@Composable
fun BottomBar(
    hazeState: HazeState,
    hazeStyle: HazeStyle,
    isVisible: Boolean = true,
) {
    LocalContext.current
    val page = LocalPagerState.current.targetPage
    val handlePageChange = LocalHandlePageChange.current
    val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
    val bottomBarFloating by appSettingsViewModel.bottomBarFloating.state.collectAsState()
    val showDivider by appSettingsViewModel.showDivider.state.collectAsState()

    val items = BottomBarDestination.entries.map { destination ->
        NavigationItem(
            label = destination.label,
            icon = destination.icon,
        )
    }

    val onItemClick: (Int) -> Unit = { index ->
        handlePageChange(index)
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = 200,
                easing = LinearEasing
            )
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(
                durationMillis = 280,
                easing = FastOutSlowInEasing
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = 180,
                easing = LinearEasing
            )
        ),
        label = "BottomBarVisibility"
    ) {
        AnimatedContent(
            targetState = bottomBarFloating,
            transitionSpec = {
                (fadeIn(animationSpec = tween(300)) +
                        slideInVertically(
                            animationSpec = tween(300),
                            initialOffsetY = { it / 2 },
                        )).togetherWith(
                    fadeOut(animationSpec = tween(300)) +
                            slideOutVertically(
                                animationSpec = tween(300),
                                targetOffsetY = { it / 2 },
                            ),
                )
            },
            label = "BottomBarStyleTransition",
        ) { isFloating ->
            val bottomHazeStyle = hazeStyle.copy(
                backgroundColor = hazeStyle.backgroundColor.copy(alpha = 0.12f)
            )
            val navModifier = Modifier
                .hazeEffect(hazeState) {
                    style = bottomHazeStyle
                    blurRadius = 36.dp
                    noiseFactor = 0.06f
                }
            if (isFloating) {
                FloatingNavigationBar(
                    modifier = navModifier,
                    color = MiuixTheme.colorScheme.surface.copy(alpha = 0.08f),
                    items = items,
                    selected = page,
                    onClick = onItemClick,
                    showDivider = showDivider,
                )
            } else {
                NavigationBar(
                    modifier = navModifier,
                    color = MiuixTheme.colorScheme.surface.copy(alpha = 0.08f),
                    items = items,
                    selected = page,
                    onClick = onItemClick,
                    showDivider = showDivider,
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
