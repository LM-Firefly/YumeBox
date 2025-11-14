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



package com.github.yumelira.yumebox.screen.settings

import androidx.lifecycle.ViewModel
import com.github.yumelira.yumebox.common.util.AppLanguageManager
import com.github.yumelira.yumebox.data.model.AppColorTheme
import com.github.yumelira.yumebox.data.model.AppLanguage
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.github.yumelira.yumebox.data.repository.AppSettingsRepository
import com.github.yumelira.yumebox.data.repository.FeatureSettingsRepository
import com.github.yumelira.yumebox.data.store.Preference

class AppSettingsViewModel(
    private val repository: AppSettingsRepository,
    private val featureRepository: FeatureSettingsRepository,
) : ViewModel() {

    val initialSetupCompleted: Preference<Boolean> = repository.initialSetupCompleted
    val privacyPolicyAccepted: Preference<Boolean> = repository.privacyPolicyAccepted

    val themeMode: Preference<ThemeMode> = repository.themeMode
    val appLanguage: Preference<AppLanguage> = repository.appLanguage
    val colorTheme: Preference<AppColorTheme> = repository.colorTheme
    val themeSeedColorArgb: Preference<Long> = repository.themeSeedColorArgb
    val automaticRestart: Preference<Boolean> = repository.automaticRestart
    val autoUpdateCurrentProfileOnStart: Preference<Boolean> = repository.autoUpdateCurrentProfileOnStart
    val hideAppIcon: Preference<Boolean> = repository.hideAppIcon
    val excludeFromRecents: Preference<Boolean> = repository.excludeFromRecents
    val showTrafficNotification: Preference<Boolean> = repository.showTrafficNotification
    val bottomBarAutoHide: Preference<Boolean> = repository.bottomBarAutoHide
    val topBarBlurEnabled: Preference<Boolean> = repository.topBarBlurEnabled
    val acgMainUiEnabled: Preference<Boolean> = repository.acgMainUiEnabled
    val acgWallpaperUri: Preference<String> = repository.acgWallpaperUri
    val acgWallpaperZoom: Preference<Float> = repository.acgWallpaperZoom
    val acgWallpaperBiasX: Preference<Float> = repository.acgWallpaperBiasX
    val acgWallpaperBiasY: Preference<Float> = repository.acgWallpaperBiasY
    val acgHomeQuote: Preference<String> = repository.acgHomeQuote
    val acgHomeQuoteAuthor: Preference<String> = repository.acgHomeQuoteAuthor
    val acgSidebarExpanded: Preference<Boolean> = repository.acgSidebarExpanded
    val pageScale: Preference<Float> = repository.pageScale
    val singleNodeTest: Preference<Boolean> = repository.singleNodeTest
    val screenshotProtectionEnabled: Preference<Boolean> = repository.screenshotProtectionEnabled
    val biometricUnlockEnabled: Preference<Boolean> = repository.biometricUnlockEnabled
    val exitUiWhenBackground: Preference<Boolean> = featureRepository.exitUiWhenBackground

    val customUserAgent: Preference<String> = repository.customUserAgent

    fun onThemeModeChange(mode: ThemeMode) = themeMode.set(mode)
    fun onAppLanguageChange(language: AppLanguage) {
        appLanguage.set(language)
        AppLanguageManager.apply(language)
    }
    fun onColorThemeChange(theme: AppColorTheme) = colorTheme.set(theme)
    fun onThemeSeedColorChange(argb: Long) = themeSeedColorArgb.set(argb)
    fun resetThemeSeedColor() = themeSeedColorArgb.set(0xFF138A74L)
    fun onBottomBarAutoHideChange(enabled: Boolean) = bottomBarAutoHide.set(enabled)
    fun onTopBarBlurEnabledChange(enabled: Boolean) = topBarBlurEnabled.set(enabled)
    fun onAcgMainUiEnabledChange(enabled: Boolean) = acgMainUiEnabled.set(enabled)
    fun onAcgWallpaperUriChange(uri: String) = acgWallpaperUri.set(uri)
    fun onAcgWallpaperCropChange(zoom: Float, biasX: Float, biasY: Float) {
        acgWallpaperZoom.set(zoom.coerceIn(1f, 5f))
        acgWallpaperBiasX.set(biasX.coerceIn(-1f, 1f))
        acgWallpaperBiasY.set(biasY.coerceIn(-1f, 1f))
    }
    fun onAcgHomeQuoteChange(quote: String) = acgHomeQuote.set(quote)
    fun onAcgHomeQuoteAuthorChange(author: String) = acgHomeQuoteAuthor.set(author)
    fun onAcgSidebarExpandedChange(expanded: Boolean) = acgSidebarExpanded.set(expanded)
    fun clearAcgWallpaperUri() {
        acgWallpaperUri.set("")
        onAcgWallpaperCropChange(zoom = 1f, biasX = 0f, biasY = 0f)
    }
    fun onPageScaleChange(scale: Float) = pageScale.set(scale)
    fun onAutomaticRestartChange(enabled: Boolean) = automaticRestart.set(enabled)
    fun onAutoUpdateCurrentProfileOnStartChange(enabled: Boolean) = autoUpdateCurrentProfileOnStart.set(enabled)
    fun onHideAppIconChange(hide: Boolean) = hideAppIcon.set(hide)
    fun onExcludeFromRecentsChange(exclude: Boolean) = excludeFromRecents.set(exclude)
    fun onShowTrafficNotificationChange(show: Boolean) = showTrafficNotification.set(show)
    fun onSingleNodeTestChange(enabled: Boolean) = singleNodeTest.set(enabled)
    fun onScreenshotProtectionEnabledChange(enabled: Boolean) = screenshotProtectionEnabled.set(enabled)
    fun onBiometricUnlockEnabledChange(enabled: Boolean) = biometricUnlockEnabled.set(enabled)
    fun onExitUiWhenBackgroundChange(enabled: Boolean) = exitUiWhenBackground.set(enabled)

    fun applyCustomUserAgent(userAgent: String) = repository.applyCustomUserAgent(userAgent)

    fun setInitialSetupCompleted(completed: Boolean) = initialSetupCompleted.set(completed)
    fun setPrivacyPolicyAccepted(accepted: Boolean) = privacyPolicyAccepted.set(accepted)
}
