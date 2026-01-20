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

package com.github.yumelira.yumebox.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.github.yumelira.yumebox.data.store.Preference
import com.github.yumelira.yumebox.domain.facade.SettingsFacade
import com.github.yumelira.yumebox.domain.facade.ProxyFacade
import com.github.yumelira.yumebox.presentation.theme.AppColorTheme


class AppSettingsViewModel(
    private val settingsFacade: SettingsFacade,
    private val proxyFacade: ProxyFacade,
) : ViewModel() {

    // Note: Currently exposing Preference<T> for UI compatibility
    // Future consideration: Migrate to StateFlow for more reactive patterns
    private val storage = settingsFacade.getAppSettingsStorage()

    val onboardingCompleted: Preference<Boolean> = storage.onboardingCompleted
    val privacyPolicyAccepted: Preference<Boolean> = storage.privacyPolicyAccepted

    val themeMode: Preference<ThemeMode> = storage.themeMode
    val colorTheme: Preference<AppColorTheme> = storage.colorTheme
    val automaticRestart: Preference<Boolean> = storage.automaticRestart
    val hideAppIcon: Preference<Boolean> = storage.hideAppIcon
    val showTrafficNotification: Preference<Boolean> = storage.showTrafficNotification
    val bottomBarFloating: Preference<Boolean> = storage.bottomBarFloating
    val showDivider: Preference<Boolean> = storage.showDivider
    val bottomBarAutoHide: Preference<Boolean> = storage.bottomBarAutoHide

    val oneWord: Preference<String> = storage.oneWord
    val oneWordAuthor: Preference<String> = storage.oneWordAuthor
    val customUserAgent: Preference<String> = storage.customUserAgent
    val logLevel: Preference<Int> = storage.logLevel


    fun onThemeModeChange(mode: ThemeMode) = themeMode.set(mode)
    fun onColorThemeChange(theme: AppColorTheme) = colorTheme.set(theme)
    fun onBottomBarAutoHideChange(enabled: Boolean) = bottomBarAutoHide.set(enabled)
    fun onAutomaticRestartChange(enabled: Boolean) = automaticRestart.set(enabled)
    fun onHideAppIconChange(hide: Boolean) = hideAppIcon.set(hide)
    fun onShowTrafficNotificationChange(show: Boolean) = showTrafficNotification.set(show)
    fun onBottomBarFloatingChange(floating: Boolean) = bottomBarFloating.set(floating)
    fun onShowDividerChange(show: Boolean) = showDivider.set(show)
    fun onLogLevelChange(level: Int) {
        logLevel.set(level)
        com.github.yumelira.yumebox.data.model.AppLogBuffer.minLogLevel = level
    }

    fun onOneWordChange(text: String) = oneWord.set(text)
    fun onOneWordAuthorChange(author: String) = oneWordAuthor.set(author)
    fun onCustomUserAgentChange(userAgent: String) = customUserAgent.set(userAgent)
    fun applyCustomUserAgent(userAgent: String) = proxyFacade.setCustomUserAgent(userAgent)

    fun setOnboardingCompleted(completed: Boolean) = onboardingCompleted.set(completed)
    fun setPrivacyPolicyAccepted(accepted: Boolean) = privacyPolicyAccepted.set(accepted)
}
