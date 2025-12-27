package com.github.yumelira.yumebox.presentation.component

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

val LocalPagerState = compositionLocalOf<PagerState> { error("LocalPagerState is not provided") }
val LocalHandlePageChange = compositionLocalOf<(Int) -> Unit> { error("LocalHandlePageChange is not provided") }
val LocalNavigator = compositionLocalOf<DestinationsNavigator> { error("LocalNavigator is not provided") }
private fun androidx.compose.ui.input.pointer.PointerInputChange.safeConsumePositionChange() {
    try {
        val method = this::class.java.getMethod("consumePositionChange")
        method.invoke(this)
    } catch (_: Exception) {
        // method not available on this Compose runtime - ignore
    }
}

@Composable
fun BottomBar(
    hazeState: HazeState,
    hazeStyle: HazeStyle,
    isVisible: Boolean = true,
) {
    LocalContext.current
    val pagerState = LocalPagerState.current
    val page = pagerState.targetPage
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
    val density = LocalDensity.current
    val dragThresholdPx = with(density) { 24.dp.toPx() }
    var accumulatedDrag by remember { mutableStateOf(0f) }
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val throttleMs = 350L
    var lastSwipeTime by remember { mutableStateOf(0L) }
    var swipeActive by remember { mutableStateOf(false) }
    var swipeDirection by remember { mutableStateOf(0) } // -1 left, 1 right
    var swipeResetKey by remember { mutableStateOf(0) }
    val scale by animateFloatAsState(targetValue = if (swipeActive) 0.97f else 1f, animationSpec = tween(120))

    LaunchedEffect(swipeResetKey) {
        if (swipeResetKey > 0) {
            delay(150)
            swipeActive = false
        }
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
                .pointerInput(pagerState) {
                    detectHorizontalDragGestures(
                        onDragStart = { _ -> accumulatedDrag = 0f },
                        onHorizontalDrag = { change, delta ->
                            accumulatedDrag += delta
                            val abs = kotlin.math.abs(accumulatedDrag)
                            val now = System.currentTimeMillis()
                            if (abs > dragThresholdPx && now - lastSwipeTime >= throttleMs) {
                                val current = pagerState.currentPage
                                coroutineScope.launch {
                                    if (accumulatedDrag < 0 && current < BottomBarDestination.entries.size - 1) {
                                        lastSwipeTime = now
                                        swipeDirection = -1
                                        swipeActive = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        handlePageChange(current + 1)
                                        swipeResetKey += 1
                                        delay(150)
                                        swipeActive = false
                                    } else if (accumulatedDrag > 0 && current > 0) {
                                        lastSwipeTime = now
                                        swipeDirection = 1
                                        swipeActive = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        handlePageChange(current - 1)
                                        swipeResetKey += 1
                                        delay(150)
                                        swipeActive = false
                                    }
                                }
                                accumulatedDrag = 0f
                                change.safeConsumePositionChange()
                            } else {
                                change.safeConsumePositionChange()
                            }
                        },
                        onDragEnd = {
                            val abs = kotlin.math.abs(accumulatedDrag)
                            val now = System.currentTimeMillis()
                            if (abs > dragThresholdPx && now - lastSwipeTime >= throttleMs) {
                                val current = pagerState.currentPage
                                coroutineScope.launch {
                                    if (accumulatedDrag < 0 && current < BottomBarDestination.entries.size - 1) {
                                        lastSwipeTime = now
                                        swipeDirection = -1
                                        swipeActive = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        handlePageChange(current + 1)
                                        delay(150)
                                        swipeActive = false
                                    } else if (accumulatedDrag > 0 && current > 0) {
                                        lastSwipeTime = now
                                        swipeDirection = 1
                                        swipeActive = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        handlePageChange(current - 1)
                                        delay(150)
                                        swipeActive = false
                                    }
                                }
                            }
                            accumulatedDrag = 0f
                        },
                        onDragCancel = {
                            accumulatedDrag = 0f
                        }
                    )
                }
                .scale(scale)

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
