package com.github.yumelira.yumebox.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import com.github.yumelira.yumebox.data.model.ThemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme


internal val LocalPlatformSystemUiEffect = compositionLocalOf<@Composable () -> Unit> { {} }

@Composable
fun YumeTheme(
    themeMode: ThemeMode? = null,
    colorTheme: AppColorTheme = AppColorTheme.ClassicMonochrome,
    spacing: Spacing = Spacing(),
    radii: Radii = Radii(),
    content: @Composable () -> Unit,
) {
    LocalPlatformSystemUiEffect.current()
    val effectiveThemeMode = themeMode ?: ThemeMode.Auto
    val isDark = when (effectiveThemeMode) {
        ThemeMode.Auto -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val colors = remember(colorTheme, isDark) {
        colorSchemeForTheme(colorTheme, isDark)
    }

    CompositionLocalProvider(
        LocalSpacing provides spacing,
        LocalRadii provides radii,
    ) {
        MiuixTheme(
            colors = colors,
        ) {
            content()
        }
    }
}
