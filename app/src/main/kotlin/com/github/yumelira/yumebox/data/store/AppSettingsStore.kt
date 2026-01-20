package com.github.yumelira.yumebox.data.store

import com.github.yumelira.yumebox.data.model.AppLanguage
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.github.yumelira.yumebox.presentation.theme.AppColorTheme
import com.tencent.mmkv.MMKV

class AppSettingsStorage(externalMmkv: MMKV) : MMKVPreference(externalMmkv = externalMmkv) {

    val onboardingCompleted by boolFlow(false)
    val privacyPolicyAccepted by boolFlow(false)

    val themeMode by enumFlow(ThemeMode.Auto)
    val colorTheme by enumFlow(AppColorTheme.ClassicMonochrome)
    val appLanguage by enumFlow(AppLanguage.System)
    val automaticRestart by boolFlow(false)
    val hideAppIcon by boolFlow(false)
    val showTrafficNotification by boolFlow(true)
    val bottomBarFloating by boolFlow(false)
    val showDivider by boolFlow(true)
    val bottomBarAutoHide by boolFlow(true)
    val oneWord by strFlow("一个人走 默守一隅清欢")
    val oneWordAuthor by strFlow("Firefly")
    val customUserAgent by strFlow("")
    val logLevel by intFlow(android.util.Log.INFO)
}
