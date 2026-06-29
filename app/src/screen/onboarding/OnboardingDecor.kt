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
 * Copyright (c)  YumeYucca 2025 - Present
 *
 */

package com.github.yumelira.yumebox.screen.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.common.AppConstants
import com.github.yumelira.yumebox.presentation.theme.AppTheme
import com.github.yumelira.yumebox.presentation.theme.UiDp
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val PagePadding = AppConstants.UI.DEFAULT_HORIZONTAL_PADDING
internal val DetailWidth = UiDp.dp560
internal val SectionShape = RoundedCornerShape(UiDp.dp36)
internal const val RevealDurationMs = 420
internal const val LinkTermsTag = "terms"
internal const val LinkPolicyTag = "policy"
internal val HeroBadgeSize = UiDp.dp108
internal val HeroIconSize = UiDp.dp56

/**
 * Slow-drifting "aurora" backdrop: the app surface with two primary-tinted radial blobs that breathe
 * and drift independently, plus a soft top-light / bottom-shadow ambient. Purely decorative and
 * GPU-cheap (a single Canvas), it gives every onboarding step a calm, premium sense of depth.
 */
@Composable
internal fun OnboardingBackdrop(modifier: Modifier = Modifier) {
    val opacity = AppTheme.opacity
    val surface = MiuixTheme.colorScheme.surface
    val primary = MiuixTheme.colorScheme.primary
    val blobWarm = remember(surface, primary) { lerp(surface, primary, 0.26f) }
    val blobCool = remember(surface, primary) { lerp(surface, primary, 0.14f) }

    val transition = rememberInfiniteTransition(label = "onboarding_aurora")
    val drift by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 16000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "aurora_drift",
        )
    val breathe by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 9000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "aurora_breathe",
        )

    Canvas(modifier = modifier.fillMaxSize().background(surface)) {
        val maxDim = maxOf(size.width, size.height)

        val warmCenter =
            Offset(x = size.width * (0.16f + 0.18f * drift), y = size.height * (0.16f + 0.05f * breathe))
        drawRect(
            brush =
                Brush.radialGradient(
                    colors = listOf(blobWarm.copy(alpha = 0.55f), Color.Transparent),
                    center = warmCenter,
                    radius = maxDim * (0.62f + 0.06f * breathe),
                )
        )

        val coolCenter =
            Offset(x = size.width * (0.92f - 0.16f * drift), y = size.height * (0.7f - 0.06f * breathe))
        drawRect(
            brush =
                Brush.radialGradient(
                    colors = listOf(blobCool.copy(alpha = 0.5f), Color.Transparent),
                    center = coolCenter,
                    radius = maxDim * (0.7f + 0.05f * (1f - breathe)),
                )
        )

        drawRect(
            brush =
                Brush.verticalGradient(
                    colors =
                        listOf(
                            Color.White.copy(alpha = opacity.ambientLight),
                            Color.Transparent,
                            Color.Black.copy(alpha = opacity.ambientShadow),
                        )
                )
        )
    }
}

@Composable
internal fun RevealBlock(
    delayMillis: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter =
            fadeIn(
                animationSpec =
                    tween(durationMillis = RevealDurationMs, easing = LinearOutSlowInEasing)
            ) +
                slideInVertically(
                    animationSpec =
                        tween(durationMillis = RevealDurationMs, easing = FastOutSlowInEasing),
                    initialOffsetY = { it / 7 },
                ),
    ) {
        content()
    }
}

@Composable
internal fun RevealScaleBlock(
    delayMillis: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter =
            fadeIn(
                animationSpec =
                    tween(durationMillis = RevealDurationMs, easing = LinearOutSlowInEasing)
            ) +
                scaleIn(
                    animationSpec =
                        tween(durationMillis = RevealDurationMs, easing = FastOutSlowInEasing),
                    initialScale = 0.92f,
                ),
    ) {
        content()
    }
}
