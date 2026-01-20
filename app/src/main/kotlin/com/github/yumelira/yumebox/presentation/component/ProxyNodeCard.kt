package com.github.yumelira.yumebox.presentation.component

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.core.model.Proxy
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal object ProxyCardDefaults {
    val CornerRadius = 12.dp
    val PaddingHorizontal = 12.dp
    val PaddingVertical = 16.dp
    val TextSpacing = 8.dp
}

@Composable
internal fun DelayPill(
    delay: Int,
    onClick: (() -> Unit)?,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MiuixTheme.textStyles.footnote1,
    globalTimeout: Int = 0,
    height: Dp = 18.dp,
) {
    val timeoutThreshold = if (globalTimeout > 0) globalTimeout else 65535
    val (text, color) = when {
        delay < 0 -> "" to Color(0x00000000)
        delay >= timeoutThreshold -> "Timeout" to Color(0xFF9E9E9E)
        delay == 0 -> "N/A" to Color(0xFFBDBDBD)
        delay in 1..500 -> "${delay}" to Color(0xFF4CAF50)
        delay in 501..1000 -> "${delay}" to Color(0xFFFFC107)
        delay in 1001..5000 -> "${delay}" to Color(0xFFFF9800)
        else -> "${delay}" to Color(0xFFF44336)
    }

    val backgroundColor = remember(delay) {
        when {
            delay < 0 -> Color(0xFF9E9E9E)
            delay == 0 -> Color(0xFFBDBDBD)
            delay in 1..800 -> Color(0xFF4CAF50)
            delay in 801..5000 -> Color(0xFFFFA726)
            else -> Color.Transparent
        }.copy(alpha = 0.14f)
    }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(backgroundColor)
            .let { base ->
                if (onClick != null) {
                    base.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    base
                }
            }
            .sizeIn(minWidth = 30.dp, minHeight = height),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.size(width = height, height = height),
                contentAlignment = Alignment.Center
            ) {
                LoadingDots(color = color)
            }
        } else {
            Text(
                text = text,
                style = textStyle,
                color = color,
                maxLines = 1,
                modifier = Modifier.height(height)
            )
        }
    }
}

@Composable
private fun LoadingDots(
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 18.dp,
) {
    val transition = rememberInfiniteTransition(label = "DelayPillDots")
    val shift1 by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 380, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shift1",
    )
    val shift2 by transition.animateFloat(
        initialValue = 1f,
        targetValue = -1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 380, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shift2",
    )

    Box(
        modifier = modifier.size(width = height, height = height),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val amplitude = 3.dp
            Box(
                Modifier
                    .size(4.dp)
                    .graphicsLayer { translationY = shift1 * amplitude.toPx() }
                    .clip(CircleShape)
                    .background(color)
            )
            Box(
                Modifier
                    .size(4.dp)
                    .graphicsLayer { translationY = shift2 * amplitude.toPx() }
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
internal fun ProxySelectableCard(
    isSelected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Card(modifier = modifier.fillMaxWidth()) {
        val boxModifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isSelected) {
                    MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    MiuixTheme.colorScheme.background
                },
                shape = RoundedCornerShape(ProxyCardDefaults.CornerRadius)
            )
            .clip(RoundedCornerShape(ProxyCardDefaults.CornerRadius))
            .let {
                if (onClick != null) {
                    it.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    it
                }
            }
            .padding(
                horizontal = ProxyCardDefaults.PaddingHorizontal,
                vertical = ProxyCardDefaults.PaddingVertical
            )
        Box(
            modifier = boxModifier,
            content = content
        )
        overlay?.let { overlayContent ->
            Box(modifier = Modifier.fillMaxWidth()) {
                overlayContent()
            }
        }
    }
}

@Composable
fun ProxyNodeCard(
    proxy: Proxy,
    isSelected: Boolean,
    onClick: (() -> Unit)?,
    isPinned: Boolean = false,
    modifier: Modifier = Modifier,
    isSingleColumn: Boolean = false,
    showDetail: Boolean = false,
    onDelayClick: (() -> Unit)? = null,
    isDelayTesting: Boolean = false,
    globalTimeout: Int = 0
) {
    ProxySelectableCard(
        isSelected = isSelected,
        onClick = onClick,
        modifier = modifier,
        overlay = if (isPinned) {
            {
                Text(
                    text = "📌",
                    style = MiuixTheme.textStyles.footnote1.copy(fontSize = 18.sp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp, end = 2.dp)
                )
            }
        } else null,
    ) {
        val textColor = if (isSelected) {
            MiuixTheme.colorScheme.primary
        } else {
            MiuixTheme.colorScheme.onSurface
        }
        if (isSingleColumn) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = proxy.name,
                        style = MiuixTheme.textStyles.body1,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (showDetail) {
                        Spacer(modifier = Modifier.height(ProxyCardDefaults.TextSpacing))
                        Text(
                            text = proxy.type.name,
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }
                DelayPill(
                    delay = proxy.delay,
                    onClick = onDelayClick,
                    isLoading = isDelayTesting,
                    textStyle = MiuixTheme.textStyles.body2,
                    globalTimeout = globalTimeout,
                    height = 22.dp,
                )
            }
        } else if (showDetail) {
            Column {
                Text(
                    text = proxy.name,
                    style = MiuixTheme.textStyles.body2,
                    color = textColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(ProxyCardDefaults.TextSpacing))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = proxy.type.name,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    DelayPill(
                        delay = proxy.delay,
                        onClick = onDelayClick,
                        isLoading = isDelayTesting,
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = proxy.name,
                    style = MiuixTheme.textStyles.body2,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                DelayPill(delay = proxy.delay, onClick = onDelayClick, isLoading = isDelayTesting)
            }
        }
    }
}
