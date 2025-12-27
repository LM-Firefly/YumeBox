package com.github.yumelira.yumebox.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Spacing(
    val none: Dp = 0.dp,
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp,
    val gutter: Dp = 16.dp,
    val screenH: Dp = 12.dp,
    val screenV: Dp = 12.dp,
)

data class Radii(
    val none: Dp = 0.dp,
    val sm: Dp = 4.dp,
    val md: Dp = 8.dp,
    val lg: Dp = 12.dp,
    val pill: Dp = 999.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
val LocalRadii = staticCompositionLocalOf { Radii() }

object AppTheme {
    val spacing: Spacing
        @Composable get() = LocalSpacing.current

    val radii: Radii
        @Composable get() = LocalRadii.current
}
