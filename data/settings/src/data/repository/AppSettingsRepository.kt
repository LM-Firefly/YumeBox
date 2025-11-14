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



package com.github.yumelira.yumebox.data.repository

import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.data.model.AppColorTheme
import com.github.yumelira.yumebox.data.model.AppLanguage
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import com.github.yumelira.yumebox.data.store.Preference

class AppSettingsRepository(
    private val storage: AppSettingsStorage,
) {
    val initialSetupCompleted: Preference<Boolean> = storage.initialSetupCompleted
    val privacyPolicyAccepted: Preference<Boolean> = storage.privacyPolicyAccepted

    val themeMode: Preference<ThemeMode> = storage.themeMode
    val appLanguage: Preference<AppLanguage> = storage.appLanguage
    val colorTheme: Preference<AppColorTheme> = storage.colorTheme
    val themeSeedColorArgb: Preference<Long> = storage.themeAccentColorArgb
    val automaticRestart: Preference<Boolean> = storage.automaticRestart
    val autoUpdateCurrentProfileOnStart: Preference<Boolean> = storage.autoUpdateCurrentProfileOnStart
    val hideAppIcon: Preference<Boolean> = storage.hideAppIcon
    val excludeFromRecents: Preference<Boolean> = storage.excludeFromRecents
    val showTrafficNotification: Preference<Boolean> = storage.showTrafficNotification
    val bottomBarAutoHide: Preference<Boolean> = storage.bottomBarAutoHide
    val topBarBlurEnabled: Preference<Boolean> = storage.topBarBlurEnabled
    val acgMainUiEnabled: Preference<Boolean> = storage.acgMainUiEnabled
    val acgWallpaperUri: Preference<String> = storage.acgWallpaperUri
    val acgWallpaperZoom: Preference<Float> = storage.acgWallpaperZoom
    val acgWallpaperBiasX: Preference<Float> = storage.acgWallpaperBiasX
    val acgWallpaperBiasY: Preference<Float> = storage.acgWallpaperBiasY
    val acgHomeQuote: Preference<String> = storage.acgHomeQuote
    val acgHomeQuoteAuthor: Preference<String> = storage.acgHomeQuoteAuthor
    val acgSidebarExpanded: Preference<Boolean> = storage.acgSidebarExpanded
    val pageScale: Preference<Float> = storage.pageScale
    val singleNodeTest: Preference<Boolean> = storage.singleNodeTest
    val screenshotProtectionEnabled: Preference<Boolean> = storage.screenshotProtectionEnabled
    val biometricUnlockEnabled: Preference<Boolean> = storage.biometricUnlockEnabled

    val customUserAgent: Preference<String> = storage.customUserAgent

    fun applyCustomUserAgent(userAgent: String) {
        customUserAgent.set(userAgent)
        Clash.setCustomUserAgent(userAgent)
    }
}
