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

package com.github.yumelira.yumebox.screen.settings.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.github.yumelira.yumebox.presentation.component.EnumSelector
import com.github.yumelira.yumebox.presentation.theme.colorFromArgb
import com.github.yumelira.yumebox.presentation.theme.colorToArgbLong
import com.github.yumelira.yumebox.presentation.theme.isDefaultThemeSeedArgb
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.ColorPicker
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.WindowBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ThemeModeAndColorItems(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    themeSeedColorArgb: Long,
    onThemeSeedColorChange: (Long) -> Unit,
    onResetThemeSeedColor: () -> Unit,
) {
    ThemeModeSelectorItem(
        themeMode = themeMode,
        onThemeModeChange = onThemeModeChange,
    )
    ThemeColorPickerItem(
        themeSeedColorArgb = themeSeedColorArgb,
        onThemeSeedColorChange = onThemeSeedColorChange,
        onResetThemeSeedColor = onResetThemeSeedColor,
    )
}

@Composable
internal fun ThemeModeSelectorItem(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    EnumSelector(
        title = MLang.AppSettings.Interface.ThemeModeTitle,
        summary = MLang.AppSettings.Interface.ThemeModeSummary,
        currentValue = themeMode,
        items = listOf(
            MLang.AppSettings.Interface.ThemeModeSystem,
            MLang.AppSettings.Interface.ThemeModeLight,
            MLang.AppSettings.Interface.ThemeModeDark,
        ),
        values = ThemeMode.entries,
        onValueChange = onThemeModeChange,
    )
}

@Composable
internal fun ThemeColorPickerItem(
    themeSeedColorArgb: Long,
    onThemeSeedColorChange: (Long) -> Unit,
    onResetThemeSeedColor: () -> Unit,
) {
    val showThemeColorPicker = remember { mutableStateOf(false) }
    val editingThemeSeedColor = remember(themeSeedColorArgb) {
        mutableStateOf(runCatching { colorFromArgb(themeSeedColorArgb) }.getOrDefault(Color.White))
    }
    val editingThemeSeedHex = remember(themeSeedColorArgb) {
        mutableStateOf(formatThemeSeedHex(themeSeedColorArgb))
    }

    BasicComponent(
        title = MLang.AppSettings.Interface.ColorThemeTitle,
        summary = if (isDefaultThemeSeedArgb(themeSeedColorArgb)) {
            MLang.AppSettings.Interface.ColorThemeDefaultSummary
        } else {
            MLang.AppSettings.Interface.ColorThemeCustomSummary.format(
                formatThemeSeedHex(themeSeedColorArgb)
            )
        },
        onClick = {
            editingThemeSeedColor.value = runCatching { colorFromArgb(themeSeedColorArgb) }
                .getOrDefault(Color.White)
            editingThemeSeedHex.value = formatThemeSeedHex(themeSeedColorArgb)
            showThemeColorPicker.value = true
        },
        endActions = {
            val previewColor = remember(themeSeedColorArgb) {
                runCatching { colorFromArgb(themeSeedColorArgb) }.getOrDefault(Color.White)
            }
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(26.dp)
                    .background(
                        color = previewColor,
                        shape = RoundedCornerShape(50),
                    )
            )
        },
    )

    WindowBottomSheet(
        show = showThemeColorPicker,
        title = MLang.AppSettings.Interface.ColorThemePickerTitle,
        onDismissRequest = { showThemeColorPicker.value = false },
        insideMargin = DpSize(24.dp, 16.dp),
    ) {
        ColorPicker(
            color = editingThemeSeedColor.value,
            onColorChanged = {
                editingThemeSeedColor.value = it
                editingThemeSeedHex.value = formatThemeSeedHex(colorToArgbLong(it))
            },
            modifier = Modifier.fillMaxWidth(),
        )
        TextField(
            value = editingThemeSeedHex.value,
            onValueChange = { raw ->
                val normalized = normalizeThemeHexInput(raw)
                editingThemeSeedHex.value = normalized
                parseThemeHexColorOrNull(normalized)?.let {
                    editingThemeSeedColor.value = it
                }
            },
            label = MLang.AppSettings.Interface.ColorThemeCodeLabel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = {
                    onResetThemeSeedColor()
                    editingThemeSeedColor.value = Color.White
                    editingThemeSeedHex.value = formatThemeSeedHex(DEFAULT_THEME_SEED_ARGB)
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(MLang.AppSettings.Interface.ColorThemeResetDefault)
            }
            Button(
                onClick = {
                    val argb = colorToArgbLong(editingThemeSeedColor.value)
                    if (isDefaultThemeSeedArgb(argb)) {
                        onResetThemeSeedColor()
                    } else {
                        onThemeSeedColorChange(argb)
                    }
                    showThemeColorPicker.value = false
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(MLang.AppSettings.Button.Apply, color = MiuixTheme.colorScheme.background)
            }
        }
    }
}

private fun formatThemeSeedHex(argb: Long): String {
    val rgb = (argb and 0x00FFFFFFL).toString(16).uppercase().padStart(6, '0')
    return "#$rgb"
}

private fun normalizeThemeHexInput(input: String): String {
    val body = input
        .uppercase()
        .filter { it in '0'..'9' || it in 'A'..'F' }
        .take(6)
    return "#$body"
}

private fun parseThemeHexColorOrNull(input: String): Color? {
    val body = input.removePrefix("#")
    if (body.length != 6) return null
    val rgb = body.toLongOrNull(16) ?: return null
    return colorFromArgb(0xFF000000L or rgb)
}

private const val DEFAULT_THEME_SEED_ARGB = 0xFFFFFFFFL
