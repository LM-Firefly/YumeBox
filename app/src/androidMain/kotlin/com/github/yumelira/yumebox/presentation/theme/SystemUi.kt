package com.github.yumelira.yumebox.presentation.theme

import android.app.Activity
import android.os.Build
import android.view.WindowInsetsController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AndroidSystemUiEffect: @Composable () -> Unit = {
    val navBarColor = Color.Transparent
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.setSystemBarsAppearance(
                    if (navBarColor.luminance() > 0.5f) WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS else 0,
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                )
            } else {
                @Suppress("DEPRECATION")
                window.navigationBarColor = navBarColor.toArgb()
                WindowCompat.getInsetsController(window, view)
                    .isAppearanceLightNavigationBars = navBarColor.luminance() > 0.5f
            }
        }
    }
}

@Composable
fun ProvideAndroidPlatformTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPlatformSystemUiEffect provides AndroidSystemUiEffect) {
        content()
    }
}
