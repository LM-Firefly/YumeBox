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

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.ColorInt

internal object OnboardingTransitionLauncher {

    fun startFromThumbnail(
        activity: Activity,
        anchorView: View,
        thumbnailView: View = anchorView,
        targetIntent: Intent,
        @ColorInt foregroundColor: Int,
    ): Boolean {
        if (activity.isFinishing || activity.isDestroyed) {
            return false
        }
        if (!anchorView.isLaidOut || anchorView.width <= 0 || anchorView.height <= 0) {
            return false
        }
        if (!thumbnailView.isLaidOut || thumbnailView.width <= 0 || thumbnailView.height <= 0) {
            return false
        }

        val launchIntent = Intent(targetIntent).apply {
            sourceBounds = anchorView.globalVisibleRectCompat()
            putExtra(EXTRA_SCALE_UP_ENTER, true)
            putExtra(EXTRA_TRANSITION_FOREGROUND_COLOR, foregroundColor)
        }

        val thumbnail = captureRoundedBitmap(thumbnailView)
        return try {
            val options = thumbnail?.let {
                ActivityOptions.makeThumbnailScaleUpAnimation(anchorView, it, 0, 0)
            }
            if (options != null) {
                activity.startActivity(launchIntent, options.toBundle())
            } else {
                activity.startActivity(launchIntent)
            }
            activity.finish()
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun captureRoundedBitmap(view: View): Bitmap? {
        val width = view.width
        val height = view.height
        if (width <= 0 || height <= 0) {
            return null
        }

        return runCatching {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                val canvas = Canvas(bitmap)
                val saveCount = canvas.save()
                val path = Path().apply {
                    val radius = resolveCornerRadius(view)
                    addRoundRect(
                        RectF(0f, 0f, width.toFloat(), height.toFloat()),
                        radius,
                        radius,
                        Path.Direction.CW,
                    )
                }
                canvas.clipPath(path)
                view.draw(canvas)
                canvas.restoreToCount(saveCount)
            }
        }.getOrNull()
    }

    private fun resolveCornerRadius(view: View): Float {
        val minSide = minOf(view.width, view.height).toFloat()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val outline = Outline()
            val provider = view.outlineProvider
            if (provider != null && provider !== ViewOutlineProvider.BACKGROUND) {
                runCatching { provider.getOutline(view, outline) }
            }
            if (!outline.isEmpty) {
                val rect = Rect()
                runCatching { outline.getRect(rect) }
                if (!rect.isEmpty) {
                    return minOf(rect.width(), rect.height()) / 2f
                }
            }
        }
        return minSide / 2f
    }

    private fun View.globalVisibleRectCompat(): Rect {
        val rect = Rect()
        getGlobalVisibleRect(rect)
        return rect
    }

    const val EXTRA_TRANSITION_FOREGROUND_COLOR: String =
        "com.github.yumelira.yumebox.screen.onboarding.TRANSITION_FOREGROUND_COLOR"

    const val EXTRA_SCALE_UP_ENTER: String =
        "com.github.yumelira.yumebox.screen.onboarding.SCALE_UP_ENTER"

    const val EXTRA_SCALE_DOWN_RETURN: String =
        "com.github.yumelira.yumebox.screen.onboarding.SCALE_DOWN_RETURN"
}
