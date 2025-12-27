package com.github.yumelira.yumebox.presentation.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.common.AppConstants
import com.github.yumelira.yumebox.presentation.theme.TrafficChartConfig
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SpeedChart(
    speedHistory: List<Long>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppConstants.UI.CARD_CORNER_RADIUS))
            .height(AppConstants.UI.SPEED_CHART_HEIGHT)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val barColor = MiuixTheme.colorScheme.primary

        speedHistory.forEach { sample ->
            val fraction = TrafficChartConfig.calculateBarFraction(sample)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(fraction)
                    .background(barColor, RoundedCornerShape(8.dp))
            )
        }
    }
}
