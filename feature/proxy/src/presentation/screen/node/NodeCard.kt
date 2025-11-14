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

import androidx.compose.animation.animateColor
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
import com.github.yumelira.yumebox.presentation.icon.yume.CircleGauge
import com.github.yumelira.yumebox.presentation.icon.yume.Cloud
import com.github.yumelira.yumebox.presentation.util.extractNodeTags
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable

internal object NodeCardDefaults {
    val CornerRadius = 24.dp
    val PaddingHorizontal = 16.dp
    val PaddingVertical = 16.dp
    val TextSpacing = 8.dp
}

internal fun nodeLatencyLabel(delay: Int?): Pair<String, Color>? = when {
    delay == null -> null
    delay < 0 -> MLang.Proxy.Node.Timeout to Color(0xFF9E9E9E)
    delay == 0 -> null
    delay in 1..300 -> MLang.Home.NodeInfo.DelayValue.format(delay) to Color(0xFF007906)
    delay in 301..1000 -> MLang.Home.NodeInfo.DelayValue.format(delay) to Color(0xFFFFB300)
    delay in 1001..3000 -> MLang.Home.NodeInfo.DelayValue.format(delay) to Color(0xFFE53935)
    else -> null
}

internal fun Proxy.Type.displayName(): String = when (this) {
    Proxy.Type.Direct -> "Direct"
    Proxy.Type.Reject -> "Reject"
    Proxy.Type.RejectDrop -> "RejectDrop"
    Proxy.Type.Compatible -> "Compatible"
    Proxy.Type.Pass -> "Pass"
    Proxy.Type.Relay -> "Relay"
    Proxy.Type.Selector -> "Selector"
    Proxy.Type.Fallback -> "Fallback"
    Proxy.Type.URLTest -> "URLTest"
    Proxy.Type.LoadBalance -> "LoadBalance"
    Proxy.Type.Smart -> "Smart"
    Proxy.Type.Unknown -> "Unknown"
    Proxy.Type.Shadowsocks -> "SS"
    Proxy.Type.ShadowsocksR -> "SSR"
    Proxy.Type.Snell -> "Snell"
    Proxy.Type.Socks5 -> "SOCKS5"
    Proxy.Type.Http -> "HTTP"
    Proxy.Type.Vmess -> "VMess"
    Proxy.Type.Vless -> "VLESS"
    Proxy.Type.Trojan -> "Trojan"
    Proxy.Type.Hysteria -> "Hysteria"
    Proxy.Type.Hysteria2 -> "Hysteria2"
    Proxy.Type.Tuic -> "TUIC"
    Proxy.Type.WireGuard -> "WireGuard"
    Proxy.Type.Dns -> "DNS"
    Proxy.Type.Ssh -> "SSH"
    Proxy.Type.Mieru -> "Mieru"
    Proxy.Type.AnyTLS -> "AnyTLS"
    Proxy.Type.Sudoku -> "Sudoku"
    Proxy.Type.Masque -> "Masque"
    Proxy.Type.TrustTunnel -> "TrustTunnel"
}

internal fun Proxy.Type.iconLabel(): String = when (this) {
    Proxy.Type.Direct -> "DI"
    Proxy.Type.Reject -> "RJ"
    Proxy.Type.RejectDrop -> "RD"
    Proxy.Type.Compatible -> "CP"
    Proxy.Type.Pass -> "PS"
    Proxy.Type.Relay -> "RL"
    Proxy.Type.Selector -> "SE"
    Proxy.Type.Fallback -> "FB"
    Proxy.Type.URLTest -> "UT"
    Proxy.Type.LoadBalance -> "LB"
    Proxy.Type.Smart -> "SM"
    Proxy.Type.Unknown -> "UN"
    Proxy.Type.Shadowsocks -> "SS"
    Proxy.Type.ShadowsocksR -> "SR"
    Proxy.Type.Snell -> "SN"
    Proxy.Type.Socks5 -> "S5"
    Proxy.Type.Http -> "HT"
    Proxy.Type.Vmess -> "VM"
    Proxy.Type.Vless -> "VL"
    Proxy.Type.Trojan -> "TR"
    Proxy.Type.Hysteria -> "HY"
    Proxy.Type.Hysteria2 -> "H2"
    Proxy.Type.Tuic -> "TU"
    Proxy.Type.WireGuard -> "WG"
    Proxy.Type.Dns -> "DN"
    Proxy.Type.Ssh -> "SH"
    Proxy.Type.Mieru -> "MI"
    Proxy.Type.AnyTLS -> "AT"
    Proxy.Type.Sudoku -> "SU"
    Proxy.Type.Masque -> "MQ"
    Proxy.Type.TrustTunnel -> "TT"
}

@Composable
internal fun RotatingCircleGauge(
    isRotating: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = MiuixTheme.colorScheme.primary,
    contentDescription: String? = MLang.Proxy.Action.Test,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "circle_gauge_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
        ),
        label = "circle_gauge_rotation_value",
    )

    Icon(
        imageVector = Yume.CircleGauge,
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
    val transition = updateTransition(targetState = isSelected, label = "node_card_selection")
    val borderColor by transition.animateColor(
        transitionSpec = {
            if (targetState) {
                tween(durationMillis = 180, easing = FastOutSlowInEasing)
            } else {
                tween(durationMillis = 220, delayMillis = 80, easing = FastOutSlowInEasing)
            }
        },
        label = "node_card_border_color",
    ) { selected ->
        if (selected) primary.copy(alpha = 0.38f) else Color.Transparent
    }

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
    isThisProxyTesting: Boolean = false,
    onSingleNodeTestClick: (() -> Unit)? = null,
    showCountryFlag: Boolean = true,
    singleNodeTestEnabled: Boolean = true,
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
        val presentation = remember(proxy.name, proxy.title) {
            resolveProxyDisplayPresentation(name = proxy.name, title = proxy.title)
        }
        val tags = remember(proxy.name) { extractNodeTags(proxy.name) }
        val delayLabel = remember(proxy.delay) { nodeLatencyLabel(proxy.delay) }
        val typeLabel = remember(proxy.type) { proxy.type.displayName() }
        val iconLabel = remember(proxy.type) { proxy.type.iconLabel() }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            NodeLargeIcon(
                modifier = Modifier.padding(top = 2.dp),
                countryCode = presentation.countryCode.takeIf { showCountryFlag },
                typeName = iconLabel,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = presentation.displayName,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        NodeTagChip(label = typeLabel)
                        tags.keywords.forEach { kw -> NodeTagChip(label = kw) }
                        tags.multiplier?.let { m ->
                            if (m > 0f) NodeMultiplierChip(multiplier = m)
                        }
                    }
                    when {
                        delayLabel != null -> {
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
                                        if (onSingleNodeTestClick != null && singleNodeTestEnabled) m.clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = onSingleNodeTestClick,
                                        ) else m
                                    },
                            )
                        }

                        onSingleNodeTestClick != null && singleNodeTestEnabled -> {
                            if (isThisProxyTesting) {
                                RotatingCircleGauge(
                                    isRotating = true,
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .size(16.dp),
                                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            } else {
                                Icon(
                                    imageVector = Yume.Cloud,
                                    contentDescription = MLang.Proxy.Action.Test,
                                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .size(16.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = onSingleNodeTestClick,
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun NodeLargeIcon(
    modifier: Modifier = Modifier,
    countryCode: String?,
    typeName: String,
) {
    val neutral = MiuixTheme.colorScheme.onSurface
    Box(
        modifier = modifier
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
