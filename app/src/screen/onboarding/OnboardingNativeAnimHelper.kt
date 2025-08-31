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



package com.github.yumelira.yumebox.screen.onboarding

import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator

internal object OnboardingNativeAnimHelper {

    fun startPageButtonAnim(target: View, onAnimationEnd: (() -> Unit)? = null) {
        target.scaleX = 0.9f
        target.scaleY = 0.9f
        target.alpha = 0f

        ObjectAnimator.ofFloat(target, View.SCALE_X, 0.9f, 1f).apply {
            interpolator = AccelerateDecelerateInterpolator()
            duration = 380L
            startDelay = 420L
            start()
        }

        ObjectAnimator.ofFloat(target, View.SCALE_Y, 0.9f, 1f).apply {
            interpolator = AccelerateDecelerateInterpolator()
            duration = 380L
            startDelay = 420L
            start()
        }

        ObjectAnimator.ofFloat(target, View.ALPHA, 0f, 1f).apply {
            interpolator = DecelerateInterpolator()
            duration = 320L
            startDelay = 360L
            if (onAnimationEnd != null) {
                doOnEndCompat(onAnimationEnd)
            }
            start()
        }
    }

    private fun ObjectAnimator.doOnEndCompat(action: () -> Unit) {
        addListener(
            object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) = Unit

                override fun onAnimationEnd(animation: android.animation.Animator) = action()

                override fun onAnimationCancel(animation: android.animation.Animator) = Unit

                override fun onAnimationRepeat(animation: android.animation.Animator) = Unit
            },
        )
    }
}
