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
 * Copyright (c)  YumeLira 2025.
 *
 */

package com.github.yumelira.yumebox.presentation.screen.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.common.AppConstants
import com.github.yumelira.yumebox.domain.model.TrafficData
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SpeedChart(
    speedHistory: List<TrafficData>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val maxSpeed = speedHistory.maxOfOrNull { maxOf(it.upload, it.download) } ?: 0L
    val minDisplayMax = 1024 * 1024L
    val targetMax = maxOf(maxSpeed, minDisplayMax).toFloat()
    val animatedMax by animateFloatAsState(
        targetValue = targetMax,
        animationSpec = tween(durationMillis = 400),
        label = "maxSpeed"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppConstants.UI.CARD_CORNER_RADIUS))
            .height(AppConstants.UI.SPEED_CHART_HEIGHT)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val downloadColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.6f)
        val uploadColor = Color(0xFF52c41a).copy(alpha = 0.6f)

        speedHistory.forEach { sample ->
            val uploadFraction = (sample.upload.toFloat() / animatedMax).coerceIn(0f, 1f)
            val downloadFraction = (sample.download.toFloat() / animatedMax).coerceIn(0f, 1f)
            val minHeight = 0.02f
            val displayUploadFraction = if (sample.upload > 0) maxOf(uploadFraction, minHeight) else minHeight
            val displayDownloadFraction = if (sample.download > 0) maxOf(downloadFraction, minHeight) else minHeight
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(displayDownloadFraction)
                        .background(downloadColor, RoundedCornerShape(8.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(displayUploadFraction)
                        .background(uploadColor, RoundedCornerShape(8.dp))
                )
            }
        }
    }
}
