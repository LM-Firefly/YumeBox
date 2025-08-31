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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.state.IntColorDrawableStateImage
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.presentation.component.CountryFlagCircle
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.chevron
import com.github.yumelira.yumebox.presentation.util.extractFlaggedName
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable
import com.github.panpf.sketch.AsyncImage as SketchAsyncImage

private data class GroupBadge(
    val label: String,
)

private fun groupBadge(type: Proxy.Type): GroupBadge = when (type) {
    Proxy.Type.URLTest, Proxy.Type.Fallback, Proxy.Type.Smart ->
        GroupBadge(type.name)

    else ->
        GroupBadge(type.name)
}

internal fun LazyListScope.nodeGroupItems(
    groups: List<ProxyGroupInfo>,
    onGroupClick: (ProxyGroupInfo) -> Unit,
    testingGroupNames: Set<String> = emptySet(),
    onGroupBoundsChanged: ((String, Rect) -> Unit)? = null,
    itemVerticalPadding: Dp = 6.dp,
) {
    items(
        items = groups,
        key = { group -> "${group.type.name}:${group.name}" },
        contentType = { "NodeGroupCard" },
    ) { group ->
        NodeGroupCard(
            group = group,
            isDelayTesting = testingGroupNames.contains(group.name),
            onClick = { onGroupClick(group) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = itemVerticalPadding),
            onBoundsChanged = onGroupBoundsChanged?.let { callback ->
                { bounds -> callback(group.name, bounds) }
            },
        )
    }
}

@Composable
internal fun NodeGroupCard(
    group: ProxyGroupInfo,
    isDelayTesting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onBoundsChanged: ((Rect) -> Unit)? = null,
) {
    val cardShape = RoundedCornerShape(26.dp)
    val interactionSource = remember { MutableInteractionSource() }

    val currentNode = remember(group.now) { extractFlaggedName(group.now) }
    val currentNodeName = remember(currentNode.displayName) {
        currentNode.displayName.ifBlank { MLang.Proxy.Mode.Direct }
    }
    val iconUri = remember(group.icon) {
        group.icon?.trim()?.takeIf { it.isNotEmpty() }?.let(::normalizeNodeGroupIconUri)
    }
    val currentDelay = remember(group.proxies, group.now) {
        group.proxies.firstOrNull { it.name == group.now }?.delay
    }
    val badge = remember(group.type) { groupBadge(group.type) }
    val delayLabel = remember(currentDelay) { nodeLatencyLabel(currentDelay) }

    Column(
        modifier = modifier
            .let { base ->
                if (onBoundsChanged != null) {
                    base.onGloballyPositioned { coords -> onBoundsChanged(coords.boundsInWindow()) }
                } else base
            }
            .shadow(
                elevation = 4.dp,
                shape = cardShape,
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.05f),
            )
            .clip(cardShape)
            .background(MiuixTheme.colorScheme.background)
            .pressable(interactionSource = interactionSource, indication = SinkFeedback())
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            if (iconUri != null) {
                NodeGroupIcon(
                    iconUri = iconUri,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp)),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {

                        Text(
                            text = group.name,
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )

                        val primary = MiuixTheme.colorScheme.primary
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(primary.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = badge.label,
                                style = MiuixTheme.textStyles.footnote1.copy(fontSize = 10.sp),
                                color = primary,
                            )
                        }
                    }

                    Text(
                        text = "${group.proxies.size} 节点",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        val cc = currentNode.countryCode
                        if (cc != null) {
                            CountryFlagCircle(countryCode = cc, size = 20.dp)
                        }
                        Text(
                            text = currentNodeName,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Box(
                        modifier = Modifier.padding(start = 8.dp),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        when {
                            delayLabel != null -> {
                                val (delayText, delayColor) = delayLabel
                                Text(
                                    text = delayText,
                                    style = MiuixTheme.textStyles.footnote1,
                                    color = delayColor,
                                )
                            }

                            isDelayTesting -> {
                                RotatingCircleGauge(
                                    isRotating = true,
                                    modifier = Modifier.size(14.dp),
                                    tint = MiuixTheme.colorScheme.primary,
                                    contentDescription = null,
                                )
                            }

                            else -> Icon(
                                Yume.chevron,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFFC7C7CC),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun normalizeNodeGroupIconUri(raw: String): String {
    if (raw.startsWith("//")) return "https:$raw"
    if (raw.startsWith("www.", ignoreCase = true)) return "https://$raw"
    return raw
}

@Composable
private fun NodeGroupIcon(
    iconUri: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val placeholderColorInt = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.10f).toArgb()
    val request = remember(context, iconUri, placeholderColorInt) {
        ImageRequest(context, iconUri) {
            placeholder(IntColorDrawableStateImage(placeholderColorInt))
            error(IntColorDrawableStateImage(placeholderColorInt))
            crossfade(true)
        }
    }
    SketchAsyncImage(
        request = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}
