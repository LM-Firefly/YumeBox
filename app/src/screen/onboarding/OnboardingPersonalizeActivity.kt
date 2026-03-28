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



package com.github.yumelira.yumebox.screen.onboarding

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.Palette
import com.github.yumelira.yumebox.presentation.theme.colorFromArgb
import com.github.yumelira.yumebox.presentation.theme.colorToArgbLong
import com.github.yumelira.yumebox.screen.settings.AppSettingsViewModel
import com.github.yumelira.yumebox.screen.settings.component.ThemeColorPickerSheet
import dev.oom_wg.purejoy.mlang.MLang
import org.koin.androidx.compose.koinViewModel

internal class OnboardingPersonalizeActivity : OnboardingBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBackwardTo(OnboardingTermsActivity::class.java)
            }
        })

        setOnboardingContent {
            val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
            val themeState = rememberThemeCustomizationState(appSettingsViewModel)
            var showThemeColorPicker by remember { mutableStateOf(false) }
            var editingThemeSeedColor by remember(themeState.themeSeedColorArgb) {
                mutableStateOf(
                    runCatching { colorFromArgb(themeState.themeSeedColorArgb) }
                        .getOrDefault(Color.White)
                )
            }
            var editingThemeSeedHex by remember(themeState.themeSeedColorArgb) {
                mutableStateOf(
                    "#${(themeState.themeSeedColorArgb and 0x00FFFFFFL).toString(16).uppercase().padStart(6, '0')}"
                )
            }

            ProvisionDetailShell(
                previewIcon = Yume.Palette,
                title = MLang.Onboarding.Personalize.Title,
                subtitle = MLang.Onboarding.Personalize.Subtitle,
                primaryText = MLang.Onboarding.Navigation.Next,
                primaryEnabled = true,
                onPrimaryClick = {
                    navigateForwardTo(OnboardingFinishActivity::class.java)
                },
                onBack = {
                    navigateBackwardTo(OnboardingTermsActivity::class.java)
                },
            ) {
                PersonalizeContent(
                    themeMode = themeState.themeMode,
                    onThemeModeChange = themeState.onThemeModeChange,
                    themeSeedColorArgb = themeState.themeSeedColorArgb,
                    onShowThemeColorPickerChange = { show ->
                        if (show) {
                            editingThemeSeedColor =
                                runCatching { colorFromArgb(themeState.themeSeedColorArgb) }
                                    .getOrDefault(Color.White)
                            editingThemeSeedHex =
                                "#${(themeState.themeSeedColorArgb and 0x00FFFFFFL).toString(16).uppercase().padStart(6, '0')}"
                        }
                        showThemeColorPicker = show
                    },
                )
            }

            ThemeColorPickerSheet(
                show = showThemeColorPicker,
                editingThemeSeedColor = editingThemeSeedColor,
                editingThemeSeedHex = editingThemeSeedHex,
                onDismissRequest = { showThemeColorPicker = false },
                onEditingThemeSeedColorChange = {
                    editingThemeSeedColor = it
                    editingThemeSeedHex =
                        "#${(colorToArgbLong(it) and 0x00FFFFFFL).toString(16).uppercase().padStart(6, '0')}"
                },
                onEditingThemeSeedHexChange = { raw ->
                    val normalized = "#${raw.uppercase().filter { ch -> ch in '0'..'9' || ch in 'A'..'F' }.take(6)}"
                    editingThemeSeedHex = normalized
                    if (normalized.length == 7) {
                        normalized.removePrefix("#").toLongOrNull(16)?.let {
                            editingThemeSeedColor = colorFromArgb(0xFF000000L or it)
                        }
                    }
                },
                onConfirm = {
                    val argb = colorToArgbLong(editingThemeSeedColor)
                    themeState.onThemeSeedColorChange(argb)
                    showThemeColorPicker = false
                },
            )
        }
    }
}
