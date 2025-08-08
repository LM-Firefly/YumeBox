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

package com.github.yumelira.yumebox.presentation.screen.node

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.presentation.component.CountryFlagCircle
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.BadgeDollarSign
import com.github.yumelira.yumebox.presentation.util.extractFlaggedName
import com.github.yumelira.yumebox.presentation.util.extractNodeTags
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable

internal object NodeCardDefaults {
    val CornerRadius = 24.dp
    val PaddingHorizontal = 16.dp
    val PaddingVertical = 16.dp
    val TextSpacing = 8.dp
}

// Shared latency helpers — also used by NodeGroups.kt
internal fun nodeLatencyLabel(delay: Int?): Pair<String, Color>? = when {
    delay == null -> null
    delay < 0 -> "TIMEOUT" to Color(0xFF9E9E9E)
    delay == 0 -> null
    delay in 1..300 -> "${delay}ms" to Color(0xFF007906)   // 良好 - 翠绿
    delay in 301..1000 -> "${delay}ms" to Color(0xFFFFB300)   // 一般 - 琥珀黄
    delay in 1001..3000 -> "${delay}ms" to Color(0xFFE53935)   // 较差 - 朱红
    else -> null
}

@Composable
internal fun RotatingRefreshIcon(
    isRotating: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = MiuixTheme.colorScheme.primary,
    contentDescription: String? = MLang.Proxy.Action.Test,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "node_refresh_icon_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
        ),
        label = "node_refresh_icon_rotation_value",
    )

    Icon(
        imageVector = MiuixIcons.Refresh,
        contentDescription = contentDescription,
        tint = tint,
        modifier = if (isRotating) modifier.rotate(rotation) else modifier,
    )
}

@Composable
internal fun NodeSelectableCard(
    isSelected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    paddingVertical: Dp = NodeCardDefaults.PaddingVertical,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(NodeCardDefaults.CornerRadius)
    val primary = MiuixTheme.colorScheme.primary
    val backgroundColor = MiuixTheme.colorScheme.background
    val borderColor = if (isSelected) primary.copy(alpha = 0.38f) else Color.Transparent

    Box(
        modifier = modifier
            .fillMaxWidth()
            .let {
                if (onClick != null) it.pressable(interactionSource = interactionSource, indication = SinkFeedback())
                else it
            }
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .let {
                if (onClick != null) it.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ) else it
            }
            .padding(horizontal = NodeCardDefaults.PaddingHorizontal, vertical = paddingVertical),
        content = content,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NodeCard(
    proxy: Proxy,
    isSelected: Boolean,
    onClick: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
    isDelayTesting: Boolean = false,
    onDelayTestClick: (() -> Unit)? = null,
    showCountryFlag: Boolean = true,
) {
    val onCardClick = remember(proxy.name, onClick) {
        onClick?.let { click -> { click(proxy.name) } }
    }

    NodeSelectableCard(
        isSelected = isSelected,
        onClick = onCardClick,
        modifier = modifier,
        paddingVertical = 12.dp,
    ) {
        val flagged = remember(proxy.name) { extractFlaggedName(proxy.name) }
        val tags = remember(proxy.name) { extractNodeTags(proxy.name) }
        val delayLabel = remember(proxy.delay) { nodeLatencyLabel(proxy.delay) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Large icon – left side, spans full card height
            NodeLargeIcon(
                countryCode = flagged.countryCode.takeIf { showCountryFlag },
                typeName = proxy.type.name,
                isSelected = isSelected,
            )

            // Right column: name (top) + tags+delay (bottom)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = flagged.displayName,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Tags + delay/check on same row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        NodeTagChip(label = proxy.type.name)
                        tags.keywords.forEach { kw -> NodeTagChip(label = kw) }
                        tags.multiplier?.let { m ->
                            if (m > 0f) NodeMultiplierChip(multiplier = m)
                        }
                    }
                    if (delayLabel != null) {
                        val (delayText, delayColor) = delayLabel
                        Text(
                            text = delayText,
                            style = MiuixTheme.textStyles.footnote1,
                            color = delayColor,
                            maxLines = 1,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .let { m ->
                                    if (onDelayTestClick != null) m.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onDelayTestClick,
                                    ) else m
                                },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun NodeLargeIcon(
    countryCode: String?,
    typeName: String,
    isSelected: Boolean,
) {
    val neutral = MiuixTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(neutral.copy(alpha = 0.06f)),
        contentAlignment = Alignment.Center,
    ) {
        if (countryCode != null) {
            CountryFlagCircle(countryCode = countryCode, size = 28.dp)
        } else {
            Text(
                text = typeName.take(2),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

@Composable
private fun NodeTagChip(label: String) {
    val primary = MiuixTheme.colorScheme.primary
    Text(
        text = label,
        style = MiuixTheme.textStyles.footnote1.copy(fontSize = 10.sp),
        color = primary,
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(primary.copy(alpha = 0.1f))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

@Composable
private fun NodeMultiplierChip(multiplier: Float) {
    val isHigh = multiplier >= 2.0f
    val primary = MiuixTheme.colorScheme.primary
    val chipBg = if (isHigh) Color(0x1AFF3B30) else primary.copy(alpha = 0.1f)
    val chipColor = if (isHigh) Color(0xFFFF3B30) else primary
    val label = if (multiplier == multiplier.toLong().toFloat()) "x${multiplier.toLong()}" else "x$multiplier"

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(chipBg)
            .padding(horizontal = 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = Yume.BadgeDollarSign,
            contentDescription = null,
            tint = chipColor,
            modifier = Modifier.size(9.dp),
        )
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote1.copy(fontSize = 10.sp),
            color = chipColor,
        )
    }
}
