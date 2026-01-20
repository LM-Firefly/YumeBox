package com.github.yumelira.yumebox.presentation.component

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.presentation.util.extractFlaggedName
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal object ProxyCardDefaults {
    val CornerRadius = 12.dp
    val PaddingHorizontal = 12.dp
    val PaddingVertical = 16.dp
    val TextSpacing = 8.dp
}

private fun delayDisplay(delay: Int, withUnit: Boolean): Pair<String, Color>? = when {
    delay < 0 -> "TIMEOUT" to Color(0xFF9E9E9E)
    delay in 1..500 -> "${delay}${if (withUnit) "ms" else ""}" to Color(0xFF4CAF50)
    delay in 501..1000 -> "${delay}${if (withUnit) "ms" else ""}" to Color(0xFFFFC107)
    delay in 1001..5000 -> "${delay}${if (withUnit) "ms" else ""}" to Color(0xFFFF9800)
    delay in 801..5000 -> "${delay}${if (withUnit) "ms" else ""}" to Color(0xFFFFA726)
    else -> null
}

@Composable
internal fun ProxySelectableCard(
    isSelected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    paddingVertical: Dp = ProxyCardDefaults.PaddingVertical,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(ProxyCardDefaults.CornerRadius)
    val backgroundColor = if (isSelected) {
        MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MiuixTheme.colorScheme.background
    }

    val boxModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = ProxyCardDefaults.PaddingHorizontal, vertical = paddingVertical)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor)
            .let {
                if (onClick != null) {
                    it.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    it
                }
            }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = boxModifier, content = content)
            overlay?.let { overlayContent ->
                Box(modifier = Modifier.fillMaxSize()) {
                    overlayContent()
                }
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
    isDelayTesting: Boolean = false,
    onDelayTestClick: (() -> Unit)? = null,
    globalTimeout: Int = 0
) {
    ProxySelectableCard(
        isSelected = isSelected,
        onClick = onClick,
        modifier = modifier,
        paddingVertical = 12.dp,
        overlay = if (isPinned) {
            {
                Text(
                    text = "📌",
                    style = MiuixTheme.textStyles.footnote1.copy(fontSize = 18.sp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-2).dp, y = 2.dp)
                )
            }
        } else null,
    ) {
        val textColor = if (isSelected) {
            MiuixTheme.colorScheme.primary
        } else {
            MiuixTheme.colorScheme.onSurface
        }
        val flagged = remember(proxy.name) { extractFlaggedName(proxy.name) }
        val delayDisplay = remember(proxy.delay, isSingleColumn) {
            delayDisplay(proxy.delay, withUnit = isSingleColumn)
        }
        if (isSingleColumn) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val countryCode = flagged.countryCode
                    if (countryCode != null) {
                        CountryFlagCircle(countryCode = countryCode, size = 16.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = flagged.displayName,
                        style = MiuixTheme.textStyles.body2,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                }
                if (showDetail) {
                    Spacer(modifier = Modifier.height(6.dp))
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
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        ProxyDelayIndicator(
                            delayDisplay = delayDisplay,
                            isDelayTesting = isDelayTesting,
                            onDelayTestClick = onDelayTestClick,
                        )
                    }
                }
            }
        } else if (showDetail) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val countryCode = flagged.countryCode
                    if (countryCode != null) {
                        CountryFlagCircle(countryCode = countryCode, size = 16.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = flagged.displayName,
                        style = MiuixTheme.textStyles.body2,
                        color = textColor,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).basicMarquee(),
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
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
                    ProxyDelayIndicator(
                        delayDisplay = delayDisplay,
                        isDelayTesting = isDelayTesting,
                        onDelayTestClick = onDelayTestClick,
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val countryCode = flagged.countryCode
                if (countryCode != null) {
                    CountryFlagCircle(countryCode = countryCode, size = 16.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = flagged.displayName,
                    style = MiuixTheme.textStyles.body2,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).basicMarquee()
                )
            }
        }
    }
}

@Composable
private fun ProxyDelayIndicator(
    delayDisplay: Pair<String, Color>?,
    isDelayTesting: Boolean,
    onDelayTestClick: (() -> Unit)?,
) {
    val slotModifier = Modifier.width(56.dp)
    if (isDelayTesting) {
        Text(
            text = "...",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.primary,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.End,
            modifier = slotModifier,
        )
        return
    }

    if (delayDisplay == null) return
    val (delayText, delayColor) = delayDisplay

    Text(
        text = delayText,
        style = MiuixTheme.textStyles.footnote1,
        color = delayColor,
        maxLines = 1,
        softWrap = false,
        textAlign = TextAlign.End,
        modifier = Modifier.let { base ->
            val slotted = base.then(slotModifier)
            if (onDelayTestClick != null) {
                slotted.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDelayTestClick,
                )
            } else {
                slotted
            }
        },
    )
}
