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
 * Copyright (c)  YumeYucca 2025 - Present
 *
 */

package com.github.yumelira.yumebox.screen.moe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.presentation.theme.Opacity
import com.github.yumelira.yumebox.presentation.theme.Radii
import com.github.yumelira.yumebox.presentation.theme.Sizes
import com.github.yumelira.yumebox.presentation.theme.Spacing
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.Calendar
import kotlin.math.abs

private val moeSpacing = Spacing()
private val moeRadii = Radii()
private val moeSizes = Sizes()
private val moeOpacity = Opacity()

internal object MoeUi {
    object Shape {
        val hero = RoundedCornerShape(moeSpacing.space28)
        val launchButton = RoundedCornerShape(moeRadii.full)
    }

    object Sidebar {
        const val fraction = 0.25f
        val contentOverlap = moeSpacing.space28
        val innerHorizontalPadding = moeSpacing.space12
        val topInset = moeSizes.heroStartButtonSize - moeSpacing.space2
        val bottomInset = moeSpacing.space32 + moeSpacing.space6
        val timeGap = moeSpacing.space12
        val timeTopGap = moeSpacing.space18
        val dividerWidth = moeSizes.settingsIconGlyphSize
        val dividerHeight = moeSpacing.space2
        val iconSpacing = moeSpacing.space32 + moeSpacing.space6
        val iconSize = moeSpacing.space28
        val digitLetterSpacing = 1.6.sp
        const val timeAlpha = 0.96f
        const val dividerAlpha = 0.62f
        const val iconAlpha = 0.88f
        val collapsedVisibleWidth = moeSpacing.space8
    }

    object Hero {
        val containerHorizontalInset = moeSpacing.space12
        val contentHorizontalInset = moeSpacing.space12
        val trafficRowGap = moeSpacing.space28
        val trafficBottomInset = moeSpacing.space12
        val runtimeInfoTopGap = moeSpacing.space16
        val delayWidth = moeSizes.nodeDelayColumnWidth
        val belowHeroTopGap = moeSpacing.space8
        val belowHeroContentGap = moeSpacing.space12
        val infoPlaceholderAlpha = moeOpacity.surfaceSoft
        val infoRowMinHeight = moeSpacing.space24
        val infoPlaceholderNodeWidth = moeSizes.homeIdleTopPadding - moeSpacing.space8
    }

    object Button {
        val bottomInset = moeSpacing.space28
        val fixedWidth = moeSizes.homeIdleTopPadding + moeSpacing.space4
        val horizontalPadding = moeSizes.settingsIconGlyphSize
        val verticalPadding = moeSpacing.space14 + moeSpacing.space2 / 2
        const val pressedScale = 0.94f
    }

    object Quote {
        val contentGap = moeSpacing.space12
        val authorTopGap = moeSpacing.space14
        val textSize = 23.sp
        val lineHeight = 31.sp
        val authorSize = 16.sp
        val authorAlpha = moeOpacity.elevatedSurface
    }

    object Traffic {
        val itemGap = moeSpacing.space6
        val labelBottomPadding = moeSpacing.space3
    }

    object Info {
        val trailingPadding = moeSpacing.space16
        val blockGap = moeSpacing.space8
    }
}

/**
 * 让水平排版的文本块整体旋转 90° 后仍正确参与布局：先在 [layout] 中交换测量框的宽高，再 [rotate]。
 * [clockwise] 为 true 时顺时针 +90°（顶→底读），false 时逆时针 -90°（底→顶读）。
 */
internal fun Modifier.rotateVertical(clockwise: Boolean = true): Modifier =
    this.layout { measurable, _ ->
            // 短标签按自然尺寸测量，避免被窄轨宽度裁断/换行
            val placeable = measurable.measure(Constraints())
            layout(placeable.height, placeable.width) {
                placeable.place(
                    x = -(placeable.width / 2 - placeable.height / 2),
                    y = -(placeable.height / 2 - placeable.width / 2),
                )
            }
        }
        .rotate(if (clockwise) 90f else -90f)

@Composable
internal fun MoeSidebarRail(
    topValue: String,
    bottomValue: String,
    icons: List<MoeSidebarIconItem>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(MoeUi.Sidebar.topInset))

        MoeSidebarValueStack(
            topValue = topValue,
            bottomValue = bottomValue,
            modifier = Modifier.padding(top = MoeUi.Sidebar.timeTopGap),
        )

        Spacer(modifier = Modifier.weight(1f))

        MoeSidebarIconRail(icons = icons)

        Spacer(modifier = Modifier.height(MoeUi.Sidebar.bottomInset))
    }
}

@Composable
private fun MoeSidebarValueStack(
    topValue: String,
    bottomValue: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MoeUi.Sidebar.timeGap),
    ) {
        MoeSidebarTimeValue(value = topValue)
        Box(
            modifier =
                Modifier.width(MoeUi.Sidebar.dividerWidth)
                    .height(MoeUi.Sidebar.dividerHeight)
                    .background(Color.White.copy(alpha = MoeUi.Sidebar.dividerAlpha))
        )
        MoeSidebarTimeValue(value = bottomValue)
    }
}

@Composable
private fun MoeSidebarIconRail(icons: List<MoeSidebarIconItem>) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MoeUi.Sidebar.iconSpacing),
    ) {
        icons.forEach { item -> MoeSidebarIconItemView(item = item) }
    }
}

@Composable
private fun MoeSidebarIconItemView(item: MoeSidebarIconItem) {
    Icon(
        imageVector = item.icon,
        contentDescription = null,
        tint = Color.White.copy(alpha = MoeUi.Sidebar.iconAlpha),
        modifier =
            Modifier.size(MoeUi.Sidebar.iconSize)
                .clickable(
                    interactionSource =
                        remember {
                            androidx.compose.foundation.interaction.MutableInteractionSource()
                        },
                    indication = null,
                    onClick = item.onClick,
                ),
    )
}

@Composable
private fun MoeSidebarTimeValue(value: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = value,
            modifier = Modifier.rotateVertical(),
            color = Color.White.copy(alpha = MoeUi.Sidebar.timeAlpha),
            style = MiuixTheme.textStyles.title1,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            fontSize = 43.sp,
            letterSpacing = MoeUi.Sidebar.digitLetterSpacing,
            softWrap = false,
        )
    }
}

internal enum class MoeWallpaperQualityMode {
    Foreground,
    BackgroundBlur,
}

internal fun lerpFloat(start: Float, stop: Float, progress: Float): Float =
    start + (stop - start) * progress

internal fun lerpDp(start: Dp, stop: Dp, progress: Float): Dp = start + (stop - start) * progress

internal fun calculateHomeVisibility(currentPage: Int, currentPageOffsetFraction: Float): Float {
    val offset = abs(currentPage.toFloat() + currentPageOffsetFraction)
    return 1f - offset.coerceIn(0f, 1f)
}

internal data class MoeSidebarIconItem(
    val icon: ImageVector,
    val onClick: () -> Unit,
)

internal data class MoeQuote(
    val text: String,
    val author: String,
)

internal data class MoeDurationPair(
    val top: String = "00",
    val bottom: String = "00",
)

/**
 * Wall-clock variant used while idle: maps an epoch timestamp to the current local hour/minute so
 * the rail shows the real time (top = HH, bottom = mm) instead of a frozen 00 / 00.
 */
internal fun formatMoeClock(nowMillis: Long): MoeDurationPair {
    val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
    return MoeDurationPair(
        top = calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0'),
        bottom = calendar.get(Calendar.MINUTE).toString().padStart(2, '0'),
    )
}

internal fun formatMoeDuration(elapsedMillis: Long): MoeDurationPair {
    val totalSeconds = (elapsedMillis / 1000L).coerceAtLeast(0L)
    val totalMinutes = totalSeconds / 60L
    val totalHours = totalMinutes / 60L
    return if (totalMinutes < 60L) {
        MoeDurationPair(
            top = totalMinutes.toString().padStart(2, '0'),
            bottom = (totalSeconds % 60L).toString().padStart(2, '0'),
        )
    } else {
        MoeDurationPair(
            top = totalHours.toString().padStart(2, '0'),
            bottom = (totalMinutes % 60L).toString().padStart(2, '0'),
        )
    }
}
