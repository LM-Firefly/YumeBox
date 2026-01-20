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

package com.github.yumelira.yumebox.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

enum class AppColorTheme {
    ClassicMonochrome,
    Baima,
    GlacierAzure,
    VerdantSilk,
    RoseVeil,
    AuroraAmethyst,
    AmberAtelier,
}

private data class ThemeColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryVariant: Color,
    val onPrimaryVariant: Color,
    val disabledPrimary: Color,
    val disabledOnPrimary: Color,
    val disabledPrimaryButton: Color,
    val disabledOnPrimaryButton: Color,
    val disabledPrimarySlider: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val tertiaryContainerVariant: Color,
    val onBackgroundVariant: Color,
)

private data class ThemePalette(
    val light: ThemeColors,
    val dark: ThemeColors,
)

const val DEFAULT_THEME_SEED_ARGB: Long = 0xFFFFFFFFL

private val DefaultColorTheme = AppColorTheme.ClassicMonochrome

private fun ThemeColors.toLightScheme() = lightColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryVariant = primaryVariant,
    onPrimaryVariant = onPrimaryVariant,
    disabledPrimary = disabledPrimary,
    disabledOnPrimary = disabledOnPrimary,
    disabledPrimaryButton = disabledPrimaryButton,
    disabledOnPrimaryButton = disabledOnPrimaryButton,
    disabledPrimarySlider = disabledPrimarySlider,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    tertiaryContainerVariant = tertiaryContainerVariant,
    onBackgroundVariant = onBackgroundVariant,
    onSurfaceContainer = primary,
)

private fun ThemeColors.toDarkScheme() = darkColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryVariant = primaryVariant,
    onPrimaryVariant = onPrimaryVariant,
    disabledPrimary = disabledPrimary,
    disabledOnPrimary = disabledOnPrimary,
    disabledPrimaryButton = disabledPrimaryButton,
    disabledOnPrimaryButton = disabledOnPrimaryButton,
    disabledPrimarySlider = disabledPrimarySlider,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    tertiaryContainerVariant = tertiaryContainerVariant,
    onBackgroundVariant = onBackgroundVariant,
    onSurfaceContainer = primary,
)

private fun ThemePalette.toColorScheme(isDark: Boolean) =
    if (isDark) dark.toDarkScheme() else light.toLightScheme()

private val themePalettes = mapOf(
    AppColorTheme.ClassicMonochrome to ThemePalette(
        light = ThemeColors(
            primary = Color(0xFF000000),
            onPrimary = Color.White,
            primaryVariant = Color(0xFF222222),
            onPrimaryVariant = Color(0xFFAAAAAA),
            disabledPrimary = Color(0xFFBDBDBD),
            disabledOnPrimary = Color(0xFFE0E0E0),
            disabledPrimaryButton = Color(0xFFBDBDBD),
            disabledOnPrimaryButton = Color(0xFFEEEEEE),
            disabledPrimarySlider = Color(0xFFDCDCDC),
            primaryContainer = Color(0xFFF0F0F0),
            onPrimaryContainer = Color(0xFF000000),
            tertiaryContainer = Color(0xFFF8F8F8),
            onTertiaryContainer = Color(0xFF000000),
            tertiaryContainerVariant = Color(0xFFF8F8F8),
            onBackgroundVariant = Color(0xFF000000),
        ),
        dark = ThemeColors(
            primary = Color.White,
            onPrimary = Color(0xFF000000),
            primaryVariant = Color(0xFFE0E0E0),
            onPrimaryVariant = Color(0xFF555555),
            disabledPrimary = Color(0xFF333333),
            disabledOnPrimary = Color(0xFF757575),
            disabledPrimaryButton = Color(0xFF333333),
            disabledOnPrimaryButton = Color(0xFF757575),
            disabledPrimarySlider = Color(0xFF444444),
            primaryContainer = Color(0xFF252525),
            onPrimaryContainer = Color.White,
            tertiaryContainer = Color(0xFF1C1C1C),
            onTertiaryContainer = Color.White,
            tertiaryContainerVariant = Color(0xFF303030),
            onBackgroundVariant = Color(0xFFE0E0E0),
        ),
    ),
    AppColorTheme.Baima to ThemePalette(
        light = ThemeColors(
            primary = Color(0xFFF67373),
            onPrimary = Color(0xFFFFFFFF),
            primaryVariant = Color(0xFFFFB3B3),
            onPrimaryVariant = Color(0xFF5C5C5C),
            disabledPrimary = Color(0xFFF9E6E6),
            disabledOnPrimary = Color(0xFFB6B6B6),
            disabledPrimaryButton = Color(0xFFE7E5E5),
            disabledOnPrimaryButton = Color(0xFFF8F7F7),
            disabledPrimarySlider = Color(0xFFE3E1E1),
            primaryContainer = Color(0xFFFFF6F6),
            onPrimaryContainer = Color(0xFF2F2F2F),
            tertiaryContainer = Color(0xFFFFFDF9),
            onTertiaryContainer = Color(0xFFF58F8F),
            tertiaryContainerVariant = Color(0xFFFFFDF9),
            onBackgroundVariant = Color(0xFFE45F5F),
        ),
        dark = ThemeColors(
            primary = Color(0xFFFF8A98),
            onPrimary = Color(0xFF1F1F1F),
            primaryVariant = Color(0xFFFFBCC6),
            onPrimaryVariant = Color(0xFF171717),
            disabledPrimary = Color(0xFF524949),
            disabledOnPrimary = Color(0xFFBDBDBD),
            disabledPrimaryButton = Color(0xFF474343),
            disabledOnPrimaryButton = Color(0xFF9E9E9E),
            disabledPrimarySlider = Color(0xFF585858),
            primaryContainer = Color(0xFF251F20),
            onPrimaryContainer = Color(0xFFFFC3CF),
            tertiaryContainer = Color(0xFF1B1818),
            onTertiaryContainer = Color(0xFFFFA4B0),
            tertiaryContainerVariant = Color(0xFF231F1F),
            onBackgroundVariant = Color(0xFFFF96A2),
        ),
    ),
    AppColorTheme.GlacierAzure to ThemePalette(
        light = ThemeColors(
            primary = Color(0xFF67AFF6),
            onPrimary = Color(0xFFFFFFFF),
            primaryVariant = Color(0xFF8EC3FA),
            onPrimaryVariant = Color(0xFF0C2437),
            disabledPrimary = Color(0xFFCDE1F6),
            disabledOnPrimary = Color(0xFF5A6C80),
            disabledPrimaryButton = Color(0xFFBAD0ED),
            disabledOnPrimaryButton = Color(0xFFF3F6FC),
            disabledPrimarySlider = Color(0xFFD8E6F8),
            primaryContainer = Color(0xFFEFF4FA),
            onPrimaryContainer = Color(0xFF102036),
            tertiaryContainer = Color(0xFFE1EEFD),
            onTertiaryContainer = Color(0xFF2E6EDB),
            tertiaryContainerVariant = Color(0xFFD6E7FD),
            onBackgroundVariant = Color(0xFF3C6FDB),
        ),
        dark = ThemeColors(
            primary = Color(0xFF9ED4FF),
            onPrimary = Color(0xFF061220),
            primaryVariant = Color(0xFFCBE5FF),
            onPrimaryVariant = Color(0xFF03101B),
            disabledPrimary = Color(0xFF295072),
            disabledOnPrimary = Color(0xFF8AAED4),
            disabledPrimaryButton = Color(0xFF1F3D55),
            disabledOnPrimaryButton = Color(0xFF7CA4D0),
            disabledPrimarySlider = Color(0xFF2F4F6D),
            primaryContainer = Color(0xFF12243D),
            onPrimaryContainer = Color(0xFFD8EDFF),
            tertiaryContainer = Color(0xFF0C182A),
            onTertiaryContainer = Color(0xFF83B3FF),
            tertiaryContainerVariant = Color(0xFF152543),
            onBackgroundVariant = Color(0xFF7DB2FF),
        ),
    ),
    AppColorTheme.VerdantSilk to ThemePalette(
        light = ThemeColors(
            primary = Color(0xFF3FB53F),
            onPrimary = Color(0xFFF5FFF5),
            primaryVariant = Color(0xFF5DC35D),
            onPrimaryVariant = Color(0xFF0B2A0B),
            disabledPrimary = Color(0xFFC9E6C9),
            disabledOnPrimary = Color(0xFF5E805E),
            disabledPrimaryButton = Color(0xFFB4DAB4),
            disabledOnPrimaryButton = Color(0xFFF2FFF2),
            disabledPrimarySlider = Color(0xFFD6EDDA),
            primaryContainer = Color(0xFFF2FFF2),
            onPrimaryContainer = Color(0xFF0C230C),
            tertiaryContainer = Color(0xFFE1FFE1),
            onTertiaryContainer = Color(0xFF3B9F46),
            tertiaryContainerVariant = Color(0xFFD8F5DB),
            onBackgroundVariant = Color(0xFF46A846),
        ),
        dark = ThemeColors(
            primary = Color(0xFF7EE483),
            onPrimary = Color(0xFF061C08),
            primaryVariant = Color(0xFFADEFB0),
            onPrimaryVariant = Color(0xFF041106),
            disabledPrimary = Color(0xFF27432B),
            disabledOnPrimary = Color(0xFF8EC597),
            disabledPrimaryButton = Color(0xFF1E3522),
            disabledOnPrimaryButton = Color(0xFF7EB58A),
            disabledPrimarySlider = Color(0xFF2B4D32),
            primaryContainer = Color(0xFF142616),
            onPrimaryContainer = Color(0xFFCFFAD4),
            tertiaryContainer = Color(0xFF0F1E11),
            onTertiaryContainer = Color(0xFF95F69D),
            tertiaryContainerVariant = Color(0xFF1A2F1D),
            onBackgroundVariant = Color(0xFF9BFF9F),
        ),
    ),
    AppColorTheme.RoseVeil to ThemePalette(
        light = ThemeColors(
            primary = Color(0xFFFF82AB),
            onPrimary = Color(0xFFFFF7FA),
            primaryVariant = Color(0xFFEF8EB3),
            onPrimaryVariant = Color(0xFF462033),
            disabledPrimary = Color(0xFFFAD2DD),
            disabledOnPrimary = Color(0xFFC18495),
            disabledPrimaryButton = Color(0xFFF0C4D2),
            disabledOnPrimaryButton = Color(0xFFFFF0F5),
            disabledPrimarySlider = Color(0xFFFFE0EA),
            primaryContainer = Color(0xFFFFE7EF),
            onPrimaryContainer = Color(0xFF3A0F1F),
            tertiaryContainer = Color(0xFFFFD7E5),
            onTertiaryContainer = Color(0xFFE45E8E),
            tertiaryContainerVariant = Color(0xFFFFE4EE),
            onBackgroundVariant = Color(0xFFD45A8A),
        ),
        dark = ThemeColors(
            primary = Color(0xFFFFB1CE),
            onPrimary = Color(0xFF250915),
            primaryVariant = Color(0xFFFFD8E8),
            onPrimaryVariant = Color(0xFF12040A),
            disabledPrimary = Color(0xFF593246),
            disabledOnPrimary = Color(0xFFC799B1),
            disabledPrimaryButton = Color(0xFF472635),
            disabledOnPrimaryButton = Color(0xFFB87F9B),
            disabledPrimarySlider = Color(0xFF5B3145),
            primaryContainer = Color(0xFF2A1520),
            onPrimaryContainer = Color(0xFFFFDDEA),
            tertiaryContainer = Color(0xFF1F0F17),
            onTertiaryContainer = Color(0xFFFFAFDA),
            tertiaryContainerVariant = Color(0xFF2F1A25),
            onBackgroundVariant = Color(0xFFFF9AC4),
        ),
    ),
    AppColorTheme.AuroraAmethyst to ThemePalette(
        light = ThemeColors(
            primary = Color(0xFFA976F6),
            onPrimary = Color(0xFFFDF8FF),
            primaryVariant = Color(0xFFBD96F3),
            onPrimaryVariant = Color(0xFF331243),
            disabledPrimary = Color(0xFFDFCCF8),
            disabledOnPrimary = Color(0xFF705788),
            disabledPrimaryButton = Color(0xFFCFB7F0),
            disabledOnPrimaryButton = Color(0xFFFAF5FF),
            disabledPrimarySlider = Color(0xFFE8DBFB),
            primaryContainer = Color(0xFFF2E8FB),
            onPrimaryContainer = Color(0xFF2B0F3A),
            tertiaryContainer = Color(0xFFE5D5FB),
            onTertiaryContainer = Color(0xFF7749C6),
            tertiaryContainerVariant = Color(0xFFEEE4FD),
            onBackgroundVariant = Color(0xFF8454D4),
        ),
        dark = ThemeColors(
            primary = Color(0xFFD8B4FF),
            onPrimary = Color(0xFF1C0828),
            primaryVariant = Color(0xFFECD8FF),
            onPrimaryVariant = Color(0xFF100417),
            disabledPrimary = Color(0xFF3D2B51),
            disabledOnPrimary = Color(0xFFBFA1DA),
            disabledPrimaryButton = Color(0xFF2F1F40),
            disabledOnPrimaryButton = Color(0xFFAD8CCF),
            disabledPrimarySlider = Color(0xFF3E2D52),
            primaryContainer = Color(0xFF261531),
            onPrimaryContainer = Color(0xFFF6EBFF),
            tertiaryContainer = Color(0xFF1D1027),
            onTertiaryContainer = Color(0xFFD1A9FF),
            tertiaryContainerVariant = Color(0xFF2C1834),
            onBackgroundVariant = Color(0xFFE0C0FF),
        ),
    ),
    AppColorTheme.AmberAtelier to ThemePalette(
        light = ThemeColors(
            primary = Color(0xFFF3AF02),
            onPrimary = Color(0xFFFFF7EB),
            primaryVariant = Color(0xFFFFBE45),
            onPrimaryVariant = Color(0xFF462600),
            disabledPrimary = Color(0xFFF6D9A8),
            disabledOnPrimary = Color(0xFF8A733F),
            disabledPrimaryButton = Color(0xFFF0C97C),
            disabledOnPrimaryButton = Color(0xFFFFF8E5),
            disabledPrimarySlider = Color(0xFFFFE5B8),
            primaryContainer = Color(0xFFFFF4DF),
            onPrimaryContainer = Color(0xFF3E2400),
            tertiaryContainer = Color(0xFFFFE6B3),
            onTertiaryContainer = Color(0xFFC06E0D),
            tertiaryContainerVariant = Color(0xFFFFF1CC),
            onBackgroundVariant = Color(0xFFD78816),
        ),
        dark = ThemeColors(
            primary = Color(0xFFF7C655),
            onPrimary = Color(0xFF261500),
            primaryVariant = Color(0xFFFFD990),
            onPrimaryVariant = Color(0xFF140900),
            disabledPrimary = Color(0xFF4A3714),
            disabledOnPrimary = Color(0xFFCFAA68),
            disabledPrimaryButton = Color(0xFF3C2B0F),
            disabledOnPrimaryButton = Color(0xFFB98F47),
            disabledPrimarySlider = Color(0xFF4F3812),
            primaryContainer = Color(0xFF2C1D06),
            onPrimaryContainer = Color(0xFFFFECB9),
            tertiaryContainer = Color(0xFF231706),
            onTertiaryContainer = Color(0xFFF9C572),
            tertiaryContainerVariant = Color(0xFF33230B),
            onBackgroundVariant = Color(0xFFFFB554),
        ),
    ),
)

private val defaultPalette = themePalettes.getValue(DefaultColorTheme)

private val presetThemeSeedArgb = mapOf(
    AppColorTheme.ClassicMonochrome to DEFAULT_THEME_SEED_ARGB,
    AppColorTheme.Baima to 0xFFF67373L,
    AppColorTheme.GlacierAzure to 0xFF67AFF6L,
    AppColorTheme.VerdantSilk to 0xFF3FB53FL,
    AppColorTheme.RoseVeil to 0xFFFF82ABL,
    AppColorTheme.AuroraAmethyst to 0xFFA976F6L,
    AppColorTheme.AmberAtelier to 0xFFF3AF02L,
)

fun colorSchemeForTheme(theme: AppColorTheme, isDark: Boolean) =
    if (theme == AppColorTheme.ClassicMonochrome) {
        defaultPalette.toColorScheme(isDark)
    } else {
        val seedArgb = presetThemeSeedArgb[theme] ?: DEFAULT_THEME_SEED_ARGB
        colorSchemeFromSeed(colorFromArgb(seedArgb), isDark)
    }

fun colorSchemeFromSeed(seed: Color, isDark: Boolean) =
    derivePaletteFromSeed(seed).toColorScheme(isDark)

fun colorFromArgb(argb: Long): Color = Color(argb.toInt())

fun colorToArgbLong(color: Color): Long = color.toArgb().toLong()

fun isDefaultThemeSeedArgb(argb: Long): Boolean {
    val rgb = argb and 0x00FFFFFFL
    return rgb == 0x000000L || rgb == 0xFFFFFFL
}

private fun derivePaletteFromSeed(seed: Color): ThemePalette {
    val lightBase = defaultPalette.light
    val darkBase = defaultPalette.dark
    return ThemePalette(
        light = deriveThemeColors(base = lightBase, seed = seed, dark = false),
        dark = deriveThemeColors(base = darkBase, seed = seed, dark = true),
    )
}

private fun deriveThemeColors(base: ThemeColors, seed: Color, dark: Boolean): ThemeColors {
    val primary = if (dark) seed.mix(Color.White, 0.20f) else seed.mix(Color.Black, 0.05f)
    val primaryVariant = if (dark) seed.mix(Color.White, 0.36f) else seed.mix(Color.White, 0.25f)
    val onPrimary = Color(0xFFFDFDFD)
    val onPrimaryVariant = if (dark) Color(0xFFF2F2F2) else seed.mix(Color.Black, 0.72f)
    val disabledPrimary = if (dark) {
        base.disabledPrimary.mix(seed, 0.18f)
    } else {
        base.disabledPrimary.mix(seed, 0.14f)
    }
    val disabledOnPrimary = if (dark) {
        base.disabledOnPrimary.mix(seed, 0.08f)
    } else {
        base.disabledOnPrimary.mix(seed, 0.06f)
    }
    val disabledPrimaryButton = if (dark) {
        base.disabledPrimaryButton.mix(seed, 0.16f)
    } else {
        base.disabledPrimaryButton.mix(seed, 0.12f)
    }
    val disabledOnPrimaryButton = if (dark) {
        base.disabledOnPrimaryButton.mix(seed, 0.06f)
    } else {
        base.disabledOnPrimaryButton.mix(seed, 0.05f)
    }
    val disabledPrimarySlider = if (dark) {
        base.disabledPrimarySlider.mix(seed, 0.14f)
    } else {
        base.disabledPrimarySlider.mix(seed, 0.10f)
    }
    val primaryContainer = if (dark) {
        base.primaryContainer.mix(seed, 0.18f)
    } else {
        base.primaryContainer.mix(seed, 0.14f)
    }
    val tertiaryContainer = if (dark) {
        base.tertiaryContainer.mix(seed, 0.13f)
    } else {
        base.tertiaryContainer.mix(seed, 0.16f)
    }
    val tertiaryContainerVariant = if (dark) {
        base.tertiaryContainerVariant.mix(seed, 0.15f)
    } else {
        base.tertiaryContainerVariant.mix(seed, 0.13f)
    }
    val onPrimaryContainer = if (dark) Color(0xFFF2F2F2) else seed.mix(Color.Black, 0.80f)
    val onTertiaryContainer = if (dark) seed.mix(Color.White, 0.22f) else seed.mix(Color.Black, 0.30f)
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryVariant = primaryVariant,
        onPrimaryVariant = onPrimaryVariant,
        disabledPrimary = disabledPrimary,
        disabledOnPrimary = disabledOnPrimary,
        disabledPrimaryButton = disabledPrimaryButton,
        disabledOnPrimaryButton = disabledOnPrimaryButton,
        disabledPrimarySlider = disabledPrimarySlider,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        tertiaryContainerVariant = tertiaryContainerVariant,
        onBackgroundVariant = if (dark) seed.mix(Color.White, 0.25f) else seed.mix(Color.Black, 0.18f),
    )
}

private fun Color.mix(other: Color, ratio: Float): Color {
    val t = ratio.coerceIn(0f, 1f)
    return Color(
        red = red + (other.red - red) * t,
        green = green + (other.green - green) * t,
        blue = blue + (other.blue - blue) * t,
        alpha = alpha + (other.alpha - alpha) * t,
    )
}
