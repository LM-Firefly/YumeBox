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



package com.github.yumelira.yumebox.screen.home

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.common.AppConstants
import com.github.yumelira.yumebox.common.util.formatBytesForDisplay
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.domain.model.TrafficData
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.*
import com.github.yumelira.yumebox.presentation.theme.AnimationSpecs
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun TrafficDisplay(
    trafficNow: TrafficData,
    profileName: String?,
    tunnelMode: TunnelState.Mode?,
    isRunning: Boolean,
    proxyMode: ProxyMode,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = isEnabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(top = 60.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        DownloadSection(
            downloadSpeed = trafficNow.download,
            profileName = profileName,
            tunnelMode = tunnelMode
        )

        UploadSection(uploadSpeed = trafficNow.upload, isRunning = isRunning, proxyMode = proxyMode)
    }
}

@Composable
private fun DownloadSection(
    downloadSpeed: Long,
    profileName: String?,
    tunnelMode: TunnelState.Mode?
) {
    Column(horizontalAlignment = Alignment.Start) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DOWNLOAD",
                style = MiuixTheme.textStyles.footnote1.copy(fontSize = 14.sp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )

            ProfileModeBadge(profileName = profileName, tunnelMode = tunnelMode)
        }

        SpeedValue(speed = downloadSpeed)
    }
}

@Composable
private fun ProfileModeBadge(
    profileName: String?,
    tunnelMode: TunnelState.Mode?
) {
    Surface(
        color = MiuixTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(50),
        modifier = Modifier.height(28.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = profileName ?: "No Profile",
                style = MiuixTheme.textStyles.footnote1.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MiuixTheme.colorScheme.primary
            )

            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(MiuixTheme.colorScheme.primary, CircleShape)
            )

            Text(
                text = tunnelMode.toDisplayName(),
                style = MiuixTheme.textStyles.footnote1.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MiuixTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SpeedValue(speed: Long) {
    val (value, unit) = formatBytesForDisplay(speed)
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = value,
            style = MiuixTheme.textStyles.headline1.copy(
                fontSize = AppConstants.UI.TRAFFIC_FONT_SIZE,
                lineHeight = AppConstants.UI.TRAFFIC_FONT_SIZE,
                letterSpacing = AppConstants.UI.TRAFFIC_LETTER_SPACING
            ),
            color = MiuixTheme.colorScheme.primary
        )
        Text(
            text = unit,
            style = MiuixTheme.textStyles.title2.copy(
                fontSize = AppConstants.UI.TRAFFIC_UNIT_FONT_SIZE
            ),
            color = MiuixTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 14.dp, start = 8.dp)
        )
    }
}

@Composable
private fun UploadSection(uploadSpeed: Long, isRunning: Boolean, proxyMode: ProxyMode) {
    val (value, unit) = formatBytesForDisplay(uploadSpeed)
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "UPLOAD",
                style = MiuixTheme.textStyles.footnote1.copy(fontSize = 14.sp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
            Text(
                text = "$value $unit",
                style = MiuixTheme.textStyles.title2.copy(fontSize = 20.sp),
                color = MiuixTheme.colorScheme.primary
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.animateContentSize(tween(AnimationSpecs.DURATION_FAST, easing = AnimationSpecs.EmphasizedDecelerate))
        ) {
            ProxyStatusCapsule(isRunning = isRunning)
            AnimatedVisibility(
                visible = isRunning,
                enter = slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(AnimationSpecs.DURATION_FAST, easing = AnimationSpecs.EmphasizedDecelerate)
                ) + fadeIn(tween(AnimationSpecs.DURATION_FAST, easing = AnimationSpecs.EnterEasing)),
                exit = slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(AnimationSpecs.DURATION_INSTANT, easing = AnimationSpecs.EmphasizedAccelerate)
                ) + fadeOut(tween(AnimationSpecs.DURATION_INSTANT, easing = AnimationSpecs.ExitEasing))
            ) {
                ProxyTypeCapsule(proxyMode = proxyMode)
            }
        }
    }
}

@Composable
private fun ProxyTypeCapsule(proxyMode: ProxyMode) {
    val primary = MiuixTheme.colorScheme.primary
    Surface(
        color = primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(50),
        modifier = Modifier.height(28.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = when (proxyMode) {
                    ProxyMode.Tun -> Yume.PlaneTakeoff
                    ProxyMode.RootTun -> Yume.Tun
                    ProxyMode.Http -> Yume.Wifi
                },
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = when (proxyMode) {
                    ProxyMode.Tun -> "VPN"
                    ProxyMode.RootTun -> "TUN"
                    ProxyMode.Http -> "HTTP"
                },
                style = MiuixTheme.textStyles.footnote1.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = primary
            )
        }
    }
}

@Composable
private fun ProxyStatusCapsule(isRunning: Boolean) {
    val primary = MiuixTheme.colorScheme.primary
    Surface(
        color = primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .height(28.dp)
            .animateContentSize(tween(AnimationSpecs.DURATION_FAST, easing = AnimationSpecs.EmphasizedDecelerate))
    ) {
        AnimatedContent(
            targetState = isRunning,
            transitionSpec = {
                (slideInHorizontally(
                    initialOffsetX = { it / 2 },
                    animationSpec = tween(AnimationSpecs.DURATION_FAST, easing = AnimationSpecs.EmphasizedDecelerate)
                ) + fadeIn(tween(AnimationSpecs.DURATION_FAST, easing = AnimationSpecs.EnterEasing))
                ).togetherWith(
                    slideOutHorizontally(
                        targetOffsetX = { -it / 2 },
                        animationSpec = tween(AnimationSpecs.DURATION_INSTANT, easing = AnimationSpecs.EmphasizedAccelerate)
                    ) + fadeOut(tween(AnimationSpecs.DURATION_INSTANT, easing = AnimationSpecs.ExitEasing))
                )
            },
            label = "CapsuleStateTransition"
        ) { running ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (running) Yume.Activity else Yume.Rocket,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = if (running) MLang.Home.Status.Running else MLang.Home.Status.TapToStart,
                    style = MiuixTheme.textStyles.footnote1.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = primary
                )
            }
        }
    }
}

private fun TunnelState.Mode?.toDisplayName(): String = when (this) {
    TunnelState.Mode.Direct -> "Direct"
    TunnelState.Mode.Global -> "Global"
    TunnelState.Mode.Rule -> "Rule"
    else -> "Rule"
}
