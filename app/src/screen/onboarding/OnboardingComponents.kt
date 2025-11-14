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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.presentation.icon.ShellIcons
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun StartupTypewriterWord(
    phrases: List<String>,
    modifier: Modifier = Modifier,
) {
    var phraseIndex by remember(phrases) { mutableStateOf(0) }
    var visibleLength by remember(phrases) { mutableStateOf(0) }
    var deleting by remember(phrases) { mutableStateOf(false) }

    LaunchedEffect(phrases) {
        while (true) {
            val currentText = phrases.getOrElse(phraseIndex) { "" }
            if (!deleting) {
                if (visibleLength < currentText.length) {
                    delay(95)
                    visibleLength += 1
                } else {
                    delay(1850)
                    deleting = true
                }
            } else {
                if (visibleLength > 0) {
                    delay(65)
                    visibleLength -= 1
                } else {
                    delay(700)
                    deleting = false
                    phraseIndex = (phraseIndex + 1) % phrases.size
                }
            }
        }
    }

    val currentText = phrases.getOrElse(phraseIndex) { "" }
    val displayText = remember(currentText, visibleLength) {
        currentText.take(visibleLength)
    }
    val showCursor = visibleLength < currentText.length || deleting

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = displayText,
            style = MiuixTheme.textStyles.title1.copy(
                fontSize = 54.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.2.sp,
            ),
            color = MiuixTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        if (showCursor) {
            Box(
                modifier = Modifier
                    .padding(start = 4.dp, top = 3.dp)
                    .width(1.2.dp)
                    .height(46.dp)
                    .background(
                        color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(50),
                    ),
            )
        }
    }
}

@Composable
internal fun HeroStartButton(
    enabled: Boolean,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pulseTransition = rememberInfiniteTransition(label = "startup_button_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1400,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "startup_button_scale",
    )

    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onStart)
            .graphicsLayer(
                alpha = if (enabled) 1f else 0.45f,
                scaleX = if (enabled) pulseScale else 1f,
                scaleY = if (enabled) pulseScale else 1f,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(MiuixTheme.colorScheme.primary),
        )
        Icon(
            imageVector = ShellIcons.NavigateForward,
            contentDescription = "Start",
            tint = MiuixTheme.colorScheme.onPrimary,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
internal fun DetailPreviewBadge(icon: ImageVector) {
    Box(
        modifier = Modifier.size(DetailPreviewBadgeSize),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.primary,
            modifier = Modifier.size(DetailPreviewIconSize),
        )
    }
}

@Composable
internal fun DetailGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(SectionShape)
            .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f)),
        content = content,
    )
}

@Composable
internal fun DetailDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 18.dp),
        thickness = 0.5.dp,
        color = MiuixTheme.colorScheme.outline.copy(alpha = 0.24f),
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
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
                modifier = Modifier.size(18.dp),
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
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
            modifier = Modifier.size(18.dp),
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MiuixTheme.colorScheme.primary)
            .graphicsLayer(alpha = if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.84f))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Medium),
            color = MiuixTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun SecondaryLinkAction(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.footnote1.copy(fontWeight = FontWeight.Medium),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}
