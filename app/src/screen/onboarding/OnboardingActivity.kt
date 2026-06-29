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

package com.github.yumelira.yumebox.screen.onboarding

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.yumelira.yumebox.data.store.AppSettingsStore
import com.github.yumelira.yumebox.presentation.theme.colorFromArgb
import com.github.yumelira.yumebox.presentation.theme.colorToArgbLong
import com.github.yumelira.yumebox.screen.settings.AppSettingsViewModel
import com.github.yumelira.yumebox.screen.settings.component.ThemeColorPickerSheet
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

internal class OnboardingActivity : OnboardingBaseActivity() {
    private val appSettingsStorage: AppSettingsStore by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!previewMode && OnboardingLauncher.consumeResetPrivacy(intent)) {
            appSettingsStorage.privacyPolicyAccepted.set(false)
        }

        setOnboardingContent {
            OnboardingScreen(
                activity = this,
                onFinish = {
                    if (!previewMode) {
                        appSettingsStorage.initialSetupCompleted.set(true)
                    }
                    finishOnboarding()
                },
            )
        }
    }
}

@Composable
private fun OnboardingScreen(activity: OnboardingActivity, onFinish: () -> Unit) {
    val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionState =
        rememberPermissionState(context = activity, lifecycleOwner = lifecycleOwner)
    val privacyState = rememberPrivacyAcceptedState(appSettingsViewModel)
    val themeState = rememberThemeCustomizationState(appSettingsViewModel)
    val showPrivacySheet = remember { mutableStateOf(false) }
    var showThemeColorPicker by remember { mutableStateOf(false) }

    OnboardingSinglePage(
        permissionState = permissionState,
        privacyAccepted = privacyState.accepted,
        onPrivacyAcceptedChange = privacyState.onAcceptedChange,
        onPrivacySheetRequest = { showPrivacySheet.value = true },
        themeMode = themeState.themeMode,
        onThemeModeChange = themeState.onThemeModeChange,
        themeSeedColorArgb = themeState.themeSeedColorArgb,
        onShowThemeColorPickerChange = { showThemeColorPicker = it },
        onFinish = onFinish,
    )

    PrivacyPolicySheet(show = showPrivacySheet)

    OnboardingThemeColorPickerHost(
        show = showThemeColorPicker,
        themeSeedColorArgb = themeState.themeSeedColorArgb,
        onThemeSeedColorChange = themeState.onThemeSeedColorChange,
        onDismiss = { showThemeColorPicker = false },
    )
}

@Composable
private fun OnboardingThemeColorPickerHost(
    show: Boolean,
    themeSeedColorArgb: Long,
    onThemeSeedColorChange: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var editingColor by
        remember(themeSeedColorArgb) {
            mutableStateOf(
                runCatching { colorFromArgb(themeSeedColorArgb) }.getOrDefault(Color.White)
            )
        }
    var editingHex by
        remember(themeSeedColorArgb) {
            mutableStateOf(
                "#${(themeSeedColorArgb and 0x00FFFFFFL).toString(16).uppercase().padStart(6, '0')}"
            )
        }

    ThemeColorPickerSheet(
        show = show,
        editingThemeSeedColor = editingColor,
        editingThemeSeedHex = editingHex,
        onDismissRequest = onDismiss,
        onEditingThemeSeedColorChange = {
            editingColor = it
            editingHex =
                "#${(colorToArgbLong(it) and 0x00FFFFFFL).toString(16).uppercase().padStart(6, '0')}"
        },
        onEditingThemeSeedHexChange = { raw ->
            val normalized =
                "#${raw.uppercase().filter { ch -> ch in '0'..'9' || ch in 'A'..'F' }.take(6)}"
            editingHex = normalized
            if (normalized.length == 7) {
                normalized.removePrefix("#").toLongOrNull(16)?.let {
                    editingColor = colorFromArgb(0xFF000000L or it)
                }
            }
        },
        onConfirm = {
            onThemeSeedColorChange(colorToArgbLong(editingColor))
            onDismiss()
        },
    )
}
