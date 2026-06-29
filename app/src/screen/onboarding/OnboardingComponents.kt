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

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.presentation.icon.ShellIcons
import com.github.yumelira.yumebox.presentation.theme.AppTheme
import com.github.yumelira.yumebox.presentation.theme.UiDp
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Large gradient wordmark for the welcome step, with a quiet tagline underneath. */
@Composable
internal fun HeroWordmark(title: String, tagline: String, modifier: Modifier = Modifier) {
    val colorScheme = MiuixTheme.colorScheme
    val brush =
        Brush.linearGradient(
            listOf(colorScheme.primary, lerp(colorScheme.primary, colorScheme.onSurface, 0.55f))
        )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UiDp.dp12),
    ) {
        Text(
            text = title,
            style =
                MiuixTheme.textStyles.title1.copy(
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    brush = brush,
                ),
            textAlign = TextAlign.Center,
        )
        Text(
            text = tagline,
            style = MiuixTheme.textStyles.body2.copy(lineHeight = 22.sp),
            color = colorScheme.onSurfaceVariantSummary,
            textAlign = TextAlign.Center,
        )
    }
}

/** A soft primary-tinted circle hosting the step's glyph, with a gentle infinite breathing pulse. */
@Composable
internal fun OnboardingHeroBadge(icon: ImageVector, modifier: Modifier = Modifier) {
    val opacity = AppTheme.opacity
    val pulse = rememberInfiniteTransition(label = "hero_badge_pulse")
    val scale by
        pulse.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "hero_badge_scale",
        )

    Box(
        modifier =
            modifier
                .size(HeroBadgeSize)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .clip(CircleShape)
                .background(MiuixTheme.colorScheme.primary.copy(alpha = opacity.subtle)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.primary,
            modifier = Modifier.size(HeroIconSize),
        )
    }
}

@Composable
internal fun DetailGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val opacity = AppTheme.opacity

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(SectionShape)
                .background(
                    MiuixTheme.colorScheme.surfaceVariant.copy(alpha = opacity.surfaceVariant)
                ),
        content = content,
    )
}

@Composable
internal fun DetailDivider() {
    val spacing = AppTheme.spacing
    val opacity = AppTheme.opacity
    val componentSizes = AppTheme.sizes

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = spacing.space18),
        thickness = componentSizes.thinDividerThickness,
        color = MiuixTheme.colorScheme.outline.copy(alpha = opacity.surfaceSoft),
    )
}

@Composable
internal fun PermissionRow(
    icon: ImageVector,
    title: String,
    summary: String,
    granted: Boolean,
    onClick: () -> Unit,
) {
    val spacing = AppTheme.spacing
    val radii = AppTheme.radii
    val opacity = AppTheme.opacity
    val componentSizes = AppTheme.sizes

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = spacing.space18, vertical = spacing.space16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.space14),
    ) {
        Box(
            modifier =
                Modifier.size(componentSizes.iconBadgeMedium)
                    .clip(RoundedCornerShape(radii.radius18))
                    .background(MiuixTheme.colorScheme.primary.copy(alpha = opacity.subtle)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier.size(spacing.space20),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.space4),
        ) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Medium),
                color = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = summary,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }

        if (granted) {
            Text(
                text = MLang.Onboarding.Permission.Common.Granted,
                style = MiuixTheme.textStyles.footnote1.copy(fontWeight = FontWeight.SemiBold),
                color = MiuixTheme.colorScheme.primary,
            )
        } else {
            Icon(
                imageVector = ShellIcons.NavigateForward,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(spacing.space18),
            )
        }
    }
}

@Composable
internal fun ProjectLinkRow(
    icon: ImageVector,
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = spacing.space18, vertical = spacing.space16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.space14),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.primary,
            modifier = Modifier.size(componentSizes.settingsIconGlyphSize),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.space4),
        ) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Medium),
                color = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = summary,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }

        Icon(
            imageVector = ShellIcons.NavigateForward,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.size(spacing.space18),
        )
    }
}

@Composable
internal fun PrimaryFooterAction(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AppTheme.spacing
    val radii = AppTheme.radii
    val opacity = AppTheme.opacity

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(radii.radius24))
                .background(MiuixTheme.colorScheme.primary)
                .graphicsLayer(alpha = if (enabled) 1f else opacity.disabledStrong)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = spacing.space20, vertical = spacing.space16),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Bold),
            color = MiuixTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
internal fun SecondaryFooterAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AppTheme.spacing
    val radii = AppTheme.radii
    val opacity = AppTheme.opacity

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(radii.radius24))
                .background(
                    MiuixTheme.colorScheme.surfaceVariant.copy(alpha = opacity.surfaceVariantStrong)
                )
                .clickable(onClick = onClick)
                .padding(horizontal = spacing.space20, vertical = spacing.space16),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Medium),
            color = MiuixTheme.colorScheme.onSurface,
        )
    }
}
