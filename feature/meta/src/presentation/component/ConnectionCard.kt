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



package com.github.yumelira.yumebox.feature.meta.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.core.model.ConnectionInfo
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.serialization.json.jsonPrimitive
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConnectionCard(
    connectionInfo: ConnectionInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    val backgroundColor = MiuixTheme.colorScheme.background
    val interactionSource = remember { MutableInteractionSource() }

    val host = remember(connectionInfo.metadata) {
        connectionInfo.metadata["host"]?.jsonPrimitive?.content ?: ""
    }
    val network = remember(connectionInfo.metadata) {
        connectionInfo.metadata["network"]?.jsonPrimitive?.content ?: "TCP"
    }
    val destinationPort = remember(connectionInfo.metadata) {
        connectionInfo.metadata["destinationPort"]?.jsonPrimitive?.content ?: ""
    }
    val sourceIP = remember(connectionInfo.metadata) {
        connectionInfo.metadata["sourceIP"]?.jsonPrimitive?.content ?: ""
    }
    val sourcePort = remember(connectionInfo.metadata) {
        connectionInfo.metadata["sourcePort"]?.jsonPrimitive?.content ?: ""
    }

    val destinationIp = remember(connectionInfo.metadata) {
        connectionInfo.metadata["destinationIP"]?.jsonPrimitive?.content ?: ""
    }

    val displayHost = remember(host, destinationIp, destinationPort, sourceIP, sourcePort) {
        if (host.isNotEmpty() && destinationPort.isNotEmpty()) {
            "$host:$destinationPort"
        } else if (host.isNotEmpty()) {
            host
        } else if (destinationIp.isNotEmpty() && destinationPort.isNotEmpty()) {
            "$destinationIp:$destinationPort"
        } else if (destinationIp.isNotEmpty()) {
            destinationIp
        } else {
            "$sourceIP:$sourcePort"
        }
    }

    val relativeTime = formatRelativeTime(connectionInfo.start)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pressable(interactionSource = interactionSource, indication = SinkFeedback())
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, MiuixTheme.colorScheme.surfaceVariant, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ConnectionLeadingIcon(
                metadata = connectionInfo.metadata,
                network = network,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {

                Text(
                    text = displayHost,
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

                        ConnectionTagChip(
                            label = network.uppercase(),
                            backgroundColor = getProtocolColor(network),
                        )

                        if (connectionInfo.rule.isNotEmpty()) {
                            ConnectionTagChip(label = connectionInfo.rule)
                        }

                        if (connectionInfo.chains.isNotEmpty()) {
                            ConnectionTagChip(label = MLang.Connection.ChainCount.format(connectionInfo.chains.size))
                        }

                    }

                    Text(
                        text = relativeTime,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionTagChip(
    label: String,
    backgroundColor: Color = MiuixTheme.colorScheme.primary,
) {
    Text(
        text = label,
        style = MiuixTheme.textStyles.footnote1.copy(fontSize = 10.sp),
        color = backgroundColor,
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(backgroundColor.copy(alpha = 0.1f))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

private fun formatRelativeTime(start: String): String {
    if (start.isEmpty()) return ""

    return try {
        val startTime = java.time.OffsetDateTime.parse(start).toInstant()
        val now = java.time.Instant.now()
        val duration = java.time.Duration.between(startTime, now)

        val seconds = duration.seconds
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        when {
            seconds < 60 -> MLang.Connection.RelativeTime.JustNow
            minutes < 60 -> MLang.Connection.RelativeTime.MinutesAgo.format(minutes)
            hours < 24 -> MLang.Connection.RelativeTime.HoursAgo.format(hours)
            days < 7 -> MLang.Connection.RelativeTime.DaysAgo.format(days)
            else -> {
                val date = java.time.LocalDateTime.ofInstant(startTime, java.time.ZoneId.systemDefault())
                MLang.Connection.RelativeTime.Date.format(date.monthValue, date.dayOfMonth)
            }
        }
    } catch (e: Exception) {
        ""
    }
}
