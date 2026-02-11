package com.github.yumelira.yumebox.presentation.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import androidx.compose.ui.unit.IntOffset
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle

object NavigationTransitions {

    // Flat "push with fade": old slides away and fades out by halfway; new slides in and fades in early.
    private const val DURATION = 450
    private const val FADE_DURATION = DURATION / 2
    private const val NODE_ROUTE_DURATION = 520

    private fun nodeAnimationSpec(): FiniteAnimationSpec<IntOffset> = tween<IntOffset>(
        durationMillis = NODE_ROUTE_DURATION,
        easing = LinearEasing,
    )

    private fun nodeEnter(offset: Int): EnterTransition = slideInHorizontally(
        initialOffsetX = { offset },
        animationSpec = nodeAnimationSpec(),
    )

    private fun nodeExit(offset: Int): ExitTransition = slideOutHorizontally(
        targetOffsetX = { offset },
        animationSpec = nodeAnimationSpec(),
    )

    private fun isNodeRoute(entry: NavBackStackEntry): Boolean {
        val route = entry.destination.route?.lowercase().orEmpty()
        if (route.contains("proxy_node_screen") || route.contains("proxynode")) return true
        val normalized = buildString(route.length) {
            route.forEach { ch -> if (ch.isLetterOrDigit()) append(ch) }
        }
        return normalized.contains("proxynodescreen")
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.isNodeTransition(): Boolean {
        return isNodeRoute(initialState) || isNodeRoute(targetState)
    }

    val defaultStyle = object : NavHostAnimatedDestinationStyle() {

        override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
            {
                if (isNodeTransition()) {
                    nodeEnter(offset = 1)
                } else {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(durationMillis = DURATION, easing = AnimationSpecs.StandardEasing)
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = FADE_DURATION, easing = LinearEasing),
                        initialAlpha = 0f
                    )
                }
            }

        override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
            {
                if (isNodeTransition()) {
                    nodeExit(offset = -1)
                } else {
                    slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(durationMillis = DURATION, easing = AnimationSpecs.StandardEasing)
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = FADE_DURATION, easing = LinearEasing),
                        targetAlpha = 0f
                    )
                }
            }

        override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
            {
                if (isNodeTransition()) {
                    nodeEnter(offset = -1)
                } else {
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(durationMillis = DURATION, easing = AnimationSpecs.StandardEasing)
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = FADE_DURATION, easing = LinearEasing),
                        initialAlpha = 0f
                    )
                }
            }

        override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
            {
                if (isNodeTransition()) {
                    nodeExit(offset = 1)
                } else {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(durationMillis = DURATION, easing = AnimationSpecs.StandardEasing)
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = FADE_DURATION, easing = LinearEasing),
                        targetAlpha = 0f
                    )
                }
            }
    }
}
