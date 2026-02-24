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

package com.github.yumelira.yumebox.viewmodel

import androidx.lifecycle.ViewModel
import com.github.yumelira.yumebox.data.model.AppColorTheme
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.github.yumelira.yumebox.data.repository.AppSettingsRepository
import com.github.yumelira.yumebox.data.store.Preference


class AppSettingsViewModel(
    private val repository: AppSettingsRepository,
) : ViewModel() {

    val onboardingCompleted: Preference<Boolean> = repository.onboardingCompleted
    val privacyPolicyAccepted: Preference<Boolean> = repository.privacyPolicyAccepted

    val themeMode: Preference<ThemeMode> = repository.themeMode
    val colorTheme: Preference<AppColorTheme> = repository.colorTheme
    val themeSeedColorArgb: Preference<Long> = repository.themeSeedColorArgb
    val automaticRestart: Preference<Boolean> = repository.automaticRestart
    val hideAppIcon: Preference<Boolean> = repository.hideAppIcon
    val excludeFromRecents: Preference<Boolean> = repository.excludeFromRecents
    val showTrafficNotification: Preference<Boolean> = repository.showTrafficNotification
    val bottomBarAutoHide: Preference<Boolean> = repository.bottomBarAutoHide
    val topBarBlurEnabled: Preference<Boolean> = repository.topBarBlurEnabled
    val pageScale: Preference<Float> = repository.pageScale

    val customUserAgent: Preference<String> = repository.customUserAgent


    fun onThemeModeChange(mode: ThemeMode) = themeMode.set(mode)
    fun onColorThemeChange(theme: AppColorTheme) = colorTheme.set(theme)
    fun onThemeSeedColorChange(argb: Long) = themeSeedColorArgb.set(argb)
    fun resetThemeSeedColor() = themeSeedColorArgb.set(0xFFFFFFFFL)
    fun onBottomBarAutoHideChange(enabled: Boolean) = bottomBarAutoHide.set(enabled)
    fun onTopBarBlurEnabledChange(enabled: Boolean) = topBarBlurEnabled.set(enabled)
    fun onPageScaleChange(scale: Float) = pageScale.set(scale)
    fun onAutomaticRestartChange(enabled: Boolean) = automaticRestart.set(enabled)
    fun onHideAppIconChange(hide: Boolean) = hideAppIcon.set(hide)
    fun onExcludeFromRecentsChange(exclude: Boolean) = excludeFromRecents.set(exclude)
    fun onShowTrafficNotificationChange(show: Boolean) = showTrafficNotification.set(show)

    fun applyCustomUserAgent(userAgent: String) = repository.applyCustomUserAgent(userAgent)

    fun setOnboardingCompleted(completed: Boolean) = onboardingCompleted.set(completed)
    fun setPrivacyPolicyAccepted(accepted: Boolean) = privacyPolicyAccepted.set(accepted)
}
