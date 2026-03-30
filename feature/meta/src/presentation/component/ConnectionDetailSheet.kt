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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.core.model.ConnectionInfo
import com.github.yumelira.yumebox.common.util.formatBytes
import com.github.yumelira.yumebox.presentation.component.AppActionBottomSheet
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ConnectionDetailSheet(
    show: Boolean,
    connectionInfo: ConnectionInfo?,
    canInterrupt: Boolean,
    onInterruptConnection: suspend (String) -> Boolean,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var isInterrupting by remember(connectionInfo?.id, show) { mutableStateOf(false) }

    val host = remember(connectionInfo) {
        connectionInfo?.metadata?.get("host")?.jsonPrimitive?.content ?: ""
    }
    val network = remember(connectionInfo) {
        connectionInfo?.metadata?.get("network")?.jsonPrimitive?.content ?: "TCP"
    }
    val process = remember(connectionInfo) {
        connectionInfo?.metadata?.get("process")?.jsonPrimitive?.content ?: ""
    }
    val destinationPort = remember(connectionInfo) {
        connectionInfo?.metadata?.get("destinationPort")?.jsonPrimitive?.content ?: ""
    }
    val sourceIP = remember(connectionInfo) {
        connectionInfo?.metadata?.get("sourceIP")?.jsonPrimitive?.content ?: ""
    }
    val sourcePort = remember(connectionInfo) {
        connectionInfo?.metadata?.get("sourcePort")?.jsonPrimitive?.content ?: ""
    }
    val destinationIP = remember(connectionInfo) {
        connectionInfo?.metadata?.get("destinationIP")?.jsonPrimitive?.content ?: ""
    }

    val displayHost = remember(host, destinationPort, sourceIP, sourcePort) {
        if (host.isNotEmpty() && destinationPort.isNotEmpty()) {
            "$host:$destinationPort"
        } else if (host.isNotEmpty()) {
            host
        } else if (sourceIP.isNotEmpty()) {
            "$sourceIP:$sourcePort"
        } else {
            ""
        }
    }

    val durationText = remember(connectionInfo?.start) {
        connectionInfo?.start?.let { calculateDuration(it) } ?: "00:00:00"
    }

    AppActionBottomSheet(
        show = show,
        title = displayHost,
        onDismissRequest = onDismiss,
        onDismissFinished = onDismissFinished,
    ) {
        connectionInfo?.let { info ->
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                item {
                    ConnectionInfoSection(
                        metadata = info.metadata,
                        network = network,
                        process = process,
                        sourceIP = sourceIP,
                        sourcePort = sourcePort,
                        destinationIP = destinationIP,
                        destinationPort = destinationPort,
                        duration = durationText,
                        upload = info.upload,
                        download = info.download,
                        chains = info.chains,
                    )
                }

                if (info.rule.isNotEmpty()) {
                    item {
                        RuleInfoSection(
                            rule = info.rule,
                            rulePayload = info.rulePayload,
                        )
                    }
                }

                if (canInterrupt) {
                    item {
                        Button(
                            onClick = {
                                if (isInterrupting) return@Button
                                isInterrupting = true
                                scope.launch {
                                    val closed = runCatching {
                                        onInterruptConnection(info.id)
                                    }.getOrDefault(false)
                                    isInterrupting = false
                                    if (closed) {
                                        onDismiss()
                                    }
                                }
                            },
                            enabled = !isInterrupting,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(),
                        ) {
                            Text(
                                text = if (isInterrupting) {
                                    MLang.Connection.Detail.Action.Interrupting
                                } else {
                                    MLang.Connection.Detail.Action.Interrupt
                                },
                                color = MiuixTheme.colorScheme.error,
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ConnectionInfoSection(
    metadata: JsonObject,
    network: String,
    process: String,
    sourceIP: String,
    sourcePort: String,
    destinationIP: String,
    destinationPort: String,
    duration: String,
    upload: Long,
    download: Long,
    chains: List<String>,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ConnectionLeadingIcon(
                metadata = metadata,
                network = network,
                size = 48.dp,
                bitmapSize = 96,
            )
            SectionTitle(MLang.Connection.Detail.Section.Info)
        }

        InfoRow(label = MLang.Connection.Detail.Label.Protocol, value = network.uppercase())
        if (process.isNotEmpty()) {
            InfoRow(label = MLang.Connection.Detail.Label.Process, value = process)
        }
        InfoRow(label = MLang.Connection.Detail.Label.SourceAddress, value = "$sourceIP:$sourcePort")
        if (destinationIP.isNotEmpty()) {
            InfoRow(label = MLang.Connection.Detail.Label.DestinationAddress, value = "$destinationIP:$destinationPort")
        }
        InfoRow(label = MLang.Connection.Detail.Label.Duration, value = duration)

        InfoRow(
            label = MLang.Connection.Detail.Label.Upload,
            value = formatBytes(upload),
            valueColor = Color(0xFF2196F3),
        )
        InfoRow(
            label = MLang.Connection.Detail.Label.Download,
            value = formatBytes(download),
            valueColor = Color(0xFF4CAF50),
        )

        if (chains.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            ProxyChainRow(chains = chains)
        }
    }
}

@Composable
private fun ProxyChainRow(chains: List<String>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(
            items = chains,
            key = { index, chain -> "$index:$chain" },
        ) { index, chain ->
            val isLast = index == chains.lastIndex

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                ChainNode(
                    name = chain,
                    isActive = isLast,
                )

                if (!isLast) {
                    Text(
                        text = "→",
                        style = MiuixTheme.textStyles.footnote1,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(horizontal = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChainNode(
    name: String,
    isActive: Boolean,
) {
    val backgroundColor = if (isActive) {
        Color(0xFF00BFA5).copy(alpha = 0.12f)
    } else {
        MiuixTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isActive) {
        Color(0xFF00BFA5)
    } else {
        Color(0xFF6B7280)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00BFA5)),
            )
        }

        Text(
            text = name,
            style = MiuixTheme.textStyles.footnote1.copy(fontSize = 11.sp),
            color = textColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun RuleInfoSection(
    rule: String,
    rulePayload: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionTitle(MLang.Connection.Detail.Section.Rule)

        InfoRow(label = MLang.Connection.Detail.Label.Type, value = rule)
        if (rulePayload.isNotEmpty()) {
            InfoRow(label = MLang.Connection.Detail.Label.Content, value = rulePayload)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MiuixTheme.textStyles.body2.copy(
            fontWeight = FontWeight.Medium,
        ),
        color = MiuixTheme.colorScheme.onSurface,
    )
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = MiuixTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.width(64.dp),
        )
        Text(
            text = value,
            style = MiuixTheme.textStyles.footnote1,
            color = valueColor,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun calculateDuration(start: String): String {
    if (start.isEmpty()) return "00:00:00"

    return try {
        val startTime = java.time.OffsetDateTime.parse(start).toInstant()
        val now = java.time.Instant.now()
        val duration = java.time.Duration.between(startTime, now)

        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        val seconds = duration.seconds % 60

        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } catch (e: Exception) {
        "00:00:00"
    }
}
