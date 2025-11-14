package com.github.yumelira.yumebox.screen.acg

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.data.model.ProxyMode
import dev.oom_wg.purejoy.mlang.MLang
import kotlin.math.abs

internal object AcgUi {
    object Shape {
        val hero = RoundedCornerShape(28.dp)
        val launchButton = RoundedCornerShape(999.dp)
    }

    object Sidebar {
        const val fraction = 0.25f
        val contentOverlap = 28.dp
        val innerHorizontalPadding = 12.dp
        val visibleOpticalOffset = (-4).dp
        val topInset = 70.dp
        val bottomInset = 34.dp
        val statsWidth = 72.dp
        val timeGap = 12.dp
        val timeValueHeight = 46.dp
        val modeTopGap = 18.dp
        val modeFontSize = 15.sp
        val dividerWidth = 22.dp
        val dividerHeight = 2.dp
        val iconSpacing = 34.dp
        val iconPillHorizontalPadding = 14.dp
        val iconPillVerticalPadding = 18.dp
        const val iconPillAlpha = 0.14f
        val digitLetterSpacing = 1.6.sp
        const val timeAlpha = 0.96f
        const val dividerAlpha = 0.62f
        const val iconAlpha = 0.88f
        val collapsedVisibleWidth = 8.dp
    }

    object Hero {
        val containerHorizontalInset = 12.dp
        val contentHorizontalInset = 12.dp
        val trafficRowGap = 28.dp
        val trafficBottomInset = 12.dp
        val runtimeInfoTopGap = 16.dp
        val delayWidth = 72.dp
        val belowHeroTopGap = 8.dp
        val belowHeroContentGap = 12.dp
        const val infoPlaceholderAlpha = 0.24f
        val infoRowMinHeight = 24.dp
        val infoPlaceholderNodeWidth = 116.dp
    }

    object Button {
        val bottomInset = 28.dp
        val fixedWidth = 126.dp
        val horizontalPadding = 22.dp
        val verticalPadding = 15.dp
        const val pressedScale = 0.94f
    }

    object Quote {
        val contentGap = 12.dp
        val authorTopGap = 14.dp
        val textSize = 23.sp
        val authorSize = 16.sp
        const val authorAlpha = 0.68f
    }
}

internal enum class AcgWallpaperQualityMode {
    Foreground,
    BackgroundBlur,
}

internal fun lerpFloat(
    start: Float,
    stop: Float,
    progress: Float,
): Float = start + (stop - start) * progress

internal fun lerpDp(
    start: Dp,
    stop: Dp,
    progress: Float,
): Dp = start + (stop - start) * progress

internal fun calculateHomeVisibility(
    currentPage: Int,
    currentPageOffsetFraction: Float,
): Float {
    val offset = abs(currentPage.toFloat() + currentPageOffsetFraction)
    return 1f - offset.coerceIn(0f, 1f)
}

internal data class AcgSidebarIconItem(
    val icon: ImageVector,
    val onClick: () -> Unit,
)

internal data class AcgQuote(
    val text: String,
    val author: String,
)

internal fun ProxyMode.toAcgDisplayName(): String = when (this) {
    ProxyMode.Tun -> MLang.Home.ProxyMode.Vpn
    ProxyMode.RootTun -> MLang.Home.ProxyMode.Tun
    ProxyMode.Http -> MLang.Home.ProxyMode.Http
}

internal data class AcgDurationPair(
    val top: String = "00",
    val bottom: String = "00",
)

internal fun formatAcgDuration(elapsedMillis: Long): AcgDurationPair {
    val totalSeconds = (elapsedMillis / 1000L).coerceAtLeast(0L)
    val totalMinutes = totalSeconds / 60L
    val totalHours = totalMinutes / 60L
    return if (totalMinutes < 60L) {
        AcgDurationPair(
            top = totalMinutes.toString().padStart(2, '0'),
            bottom = (totalSeconds % 60L).toString().padStart(2, '0'),
        )
    } else {
        AcgDurationPair(
            top = totalHours.toString().padStart(2, '0'),
            bottom = (totalMinutes % 60L).toString().padStart(2, '0'),
        )
    }
}
