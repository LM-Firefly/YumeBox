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

package com.github.yumelira.yumebox.presentation.theme


import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle

object NavigationTransitions {

    // 动画时长
    private const val DURATION_ENTER = 400
    private const val DURATION_EXIT = 400
    private const val DURATION_POP_ENTER = 400
    private const val DURATION_POP_EXIT = 400

    // 缩放比例：0.92 更有层次感
    private const val SCALE_BACK_FACTOR = 0.92f

    // 经典的“iOS风格”平滑曲线 (Emphasized Decelerate)
    private val emphasizedEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)

    val defaultStyle = object : NavHostAnimatedDestinationStyle() {

        // A -> B (新页面入场)：从右侧滑入
        override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
            {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(DURATION_ENTER, easing = emphasizedEasing)
                )
            }

        // A -> B (旧页面退场)：原地缩放 + 变暗
        override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
            {
                scaleOut(
                    targetScale = SCALE_BACK_FACTOR,
                    animationSpec = tween(DURATION_EXIT, easing = emphasizedEasing)
                ) + fadeOut(
                    targetAlpha = 0.7f, // 保持一定可见度，避免黑屏
                    animationSpec = tween(DURATION_EXIT, easing = emphasizedEasing)
                )
            }

        // B -> A (旧页面恢复)：从缩放状态放大回原状
        override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
            {
                scaleIn(
                    initialScale = SCALE_BACK_FACTOR,
                    animationSpec = tween(DURATION_POP_ENTER, easing = emphasizedEasing)
                ) + fadeIn(
                    initialAlpha = 0.7f,
                    animationSpec = tween(DURATION_POP_ENTER, easing = emphasizedEasing)
                )
            }

        // B -> A (当前页面退场)：向右侧滑出
        override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
            {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(DURATION_POP_EXIT, easing = emphasizedEasing)
                )
            }
    }
}