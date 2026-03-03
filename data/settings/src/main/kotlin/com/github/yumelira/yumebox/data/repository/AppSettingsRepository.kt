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
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import com.github.yumelira.yumebox.data.store.Preference

class AppSettingsRepository(
    private val storage: AppSettingsStorage,
) {
    val onboardingCompleted: Preference<Boolean> = storage.onboardingCompleted
    val privacyPolicyAccepted: Preference<Boolean> = storage.privacyPolicyAccepted

    val themeMode: Preference<ThemeMode> = storage.themeMode
    val colorTheme: Preference<AppColorTheme> = storage.colorTheme
    val themeSeedColorArgb: Preference<Long> = storage.themeSeedColorArgb
    val automaticRestart: Preference<Boolean> = storage.automaticRestart
    val autoUpdateCurrentProfileOnStart: Preference<Boolean> = storage.autoUpdateCurrentProfileOnStart
    val hideAppIcon: Preference<Boolean> = storage.hideAppIcon
    val excludeFromRecents: Preference<Boolean> = storage.excludeFromRecents
    val showTrafficNotification: Preference<Boolean> = storage.showTrafficNotification
    val bottomBarAutoHide: Preference<Boolean> = storage.bottomBarAutoHide
    val topBarBlurEnabled: Preference<Boolean> = storage.topBarBlurEnabled
    val pageScale: Preference<Float> = storage.pageScale

    val customUserAgent: Preference<String> = storage.customUserAgent

    fun applyCustomUserAgent(userAgent: String) {
        customUserAgent.set(userAgent)
        Clash.setCustomUserAgent(userAgent)
    }
}
