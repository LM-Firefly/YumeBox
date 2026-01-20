package com.github.yumelira.yumebox.domain.facade

import com.github.yumelira.yumebox.data.model.AccessControlMode
import com.github.yumelira.yumebox.data.model.AppLanguage
import com.github.yumelira.yumebox.data.model.AutoCloseMode
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.github.yumelira.yumebox.data.model.TunStack
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import com.github.yumelira.yumebox.data.store.FeatureStorage
import com.github.yumelira.yumebox.data.store.NetworkSettingsStorage
import com.github.yumelira.yumebox.data.store.Preference
import com.github.yumelira.yumebox.presentation.theme.AppColorTheme
import com.github.yumelira.yumebox.substore.SubStoreService
import kotlinx.coroutines.flow.StateFlow

class SettingsFacade(
    private val appSettingsStorage: AppSettingsStorage,
    private val networkSettingsStorage: NetworkSettingsStorage,
    private val featureStore: FeatureStorage
) {
    // App settings
    val onboardingCompleted: Preference<Boolean> = appSettingsStorage.onboardingCompleted
    val privacyPolicyAccepted: Preference<Boolean> = appSettingsStorage.privacyPolicyAccepted

    val themeMode: Preference<ThemeMode> = appSettingsStorage.themeMode
    val colorTheme: Preference<AppColorTheme> = appSettingsStorage.colorTheme
    val appLanguage: Preference<AppLanguage> = appSettingsStorage.appLanguage

    val automaticRestart: Preference<Boolean> = appSettingsStorage.automaticRestart
    val hideAppIcon: Preference<Boolean> = appSettingsStorage.hideAppIcon
    val showTrafficNotification: Preference<Boolean> = appSettingsStorage.showTrafficNotification
    val bottomBarFloating: Preference<Boolean> = appSettingsStorage.bottomBarFloating
    val showDivider: Preference<Boolean> = appSettingsStorage.showDivider
    val bottomBarAutoHide: Preference<Boolean> = appSettingsStorage.bottomBarAutoHide

    val oneWord: StateFlow<String> = appSettingsStorage.oneWord.state
    val oneWordAuthor: StateFlow<String> = appSettingsStorage.oneWordAuthor.state
    val customUserAgent: Preference<String> = appSettingsStorage.customUserAgent
    val logLevel: Preference<Int> = appSettingsStorage.logLevel

    // Network settings
    val proxyMode: Preference<ProxyMode> = networkSettingsStorage.proxyMode
    val bypassPrivateNetwork: Preference<Boolean> = networkSettingsStorage.bypassPrivateNetwork
    val dnsHijack: Preference<Boolean> = networkSettingsStorage.dnsHijack
    val allowBypass: Preference<Boolean> = networkSettingsStorage.allowBypass
    val enableIPv6: Preference<Boolean> = networkSettingsStorage.enableIPv6
    val systemProxy: Preference<Boolean> = networkSettingsStorage.systemProxy

    val tunStack: Preference<TunStack> = networkSettingsStorage.tunStack

    val accessControlMode: Preference<AccessControlMode> = networkSettingsStorage.accessControlMode
    val accessControlPackages: Preference<Set<String>> = networkSettingsStorage.accessControlPackages
    val accessControlShowSystemApps: Preference<Boolean> = networkSettingsStorage.accessControlShowSystemApps

    // Feature settings
    val allowLanAccess: Preference<Boolean> = featureStore.allowLanAccess
    val backendPort: Preference<Int> = featureStore.backendPort
    val frontendPort: Preference<Int> = featureStore.frontendPort
    val selectedPanelType: Preference<Int> = featureStore.selectedPanelType
    val showWebControlInProxy: Preference<Boolean> = featureStore.showWebControlInProxy
    val autoCloseMode: Preference<AutoCloseMode> = featureStore.autoCloseMode

    fun isSubStoreRunning(): Boolean = SubStoreService.isRunning

    fun startSubStore(frontendPort: Int, backendPort: Int, allowLan: Boolean) {
        SubStoreService.startService(frontendPort, backendPort, allowLan)
    }

    fun stopSubStore() {
        SubStoreService.stopService()
    }

    internal fun getAppSettingsStorage(): AppSettingsStorage = appSettingsStorage
}
