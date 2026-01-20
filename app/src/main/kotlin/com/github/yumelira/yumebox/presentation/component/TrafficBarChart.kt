package com.github.yumelira.yumebox.presentation.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.common.util.formatBytes
import com.github.yumelira.yumebox.presentation.theme.AnimationSpecs
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

data class BarChartItem(
    val label: String,
    val upload: Long,
    val download: Long,
    val isHighlighted: Boolean = false
) {
    val value: Long get() = upload + download
}

@Composable
fun TrafficBarChart(
    items: List<BarChartItem>,
    modifier: Modifier = Modifier,
    maxDisplayValue: Long? = null,
    onItemClick: ((Int) -> Unit)? = null,
    selectedIndex: Int = -1,
    uploadColor: Color = Color(0xFF52c41a),
    downloadColor: Color = MiuixTheme.colorScheme.primary,
    slotCount: Int = 12,
    chartHeight: Dp = 140.dp,
    barWidth: Dp = 20.dp
) {
    val computedMaxValue = maxDisplayValue ?: items.maxOfOrNull { it.value } ?: 1L
    val safeMaxValue = if (computedMaxValue <= 0L) 1L else computedMaxValue

    val animatedMaxValue by animateFloatAsState(
        targetValue = safeMaxValue.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = 380f
        ),
        label = "maxValue"
    )

    val displayItems = remember(items) {
        val targetSlots = maxOf(slotCount, items.size)
        if (items.size >= targetSlots) {
            items.take(targetSlots)
        } else {
            val pad = targetSlots - items.size
            val leftPad = pad / 2
            val rightPad = pad - leftPad
            List(leftPad) { BarChartItem("", 0L, 0L) } + items + List(rightPad) { BarChartItem("", 0L, 0L) }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = formatBytes(animatedMaxValue.toLong()),
                style = MiuixTheme.textStyles.footnote1.copy(fontSize = 10.sp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            displayItems.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex || item.isHighlighted
                val isValidItem = item.label.isNotEmpty()

                val targetHeight = if (animatedMaxValue > 0 && item.value > 0) {
                    (item.value.toFloat() / animatedMaxValue).coerceIn(0.04f, 1f)
                } else {
                    0.04f
                }

                val animatedHeight by animateFloatAsState(
                    targetValue = if (isValidItem) targetHeight else 0f,
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = 400f
                    ),
                    label = "barHeight_$index"
                )

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    if (isValidItem && animatedHeight > 0f) {
                        Box(
                            modifier = Modifier
                                .width(barWidth)
                                .fillMaxHeight(animatedHeight)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .then(
                                    if (onItemClick != null) {
                                        Modifier.clickable { onItemClick(index) }
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            val total = item.value.toFloat()
                            val uploadRatio = if (total > 0) item.upload.toFloat() / total else 0f
                            val downloadRatio = if (total > 0) item.download.toFloat() / total else 0f
                            val alpha = if (isSelected) 1f else 0.6f
                            Column(Modifier.fillMaxSize()) {
                                if (uploadRatio > 0f) {
                                    Box(
                                        Modifier
                                            .weight(uploadRatio)
                                            .fillMaxWidth()
                                            .background(uploadColor.copy(alpha = alpha))
                                    )
                                }
                                if (downloadRatio > 0f) {
                                    Box(
                                        Modifier
                                            .weight(downloadRatio)
                                            .fillMaxWidth()
                                            .background(downloadColor.copy(alpha = alpha))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            displayItems.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex || item.isHighlighted
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.label,
                        style = MiuixTheme.textStyles.footnote1.copy(fontSize = 9.sp),
                        color = if (isSelected) {
                            MiuixTheme.colorScheme.primary
                        } else {
                            MiuixTheme.colorScheme.onSurfaceVariantSummary
                        },
                        maxLines = 1
                    )
                }
            }
        }
    }
}
