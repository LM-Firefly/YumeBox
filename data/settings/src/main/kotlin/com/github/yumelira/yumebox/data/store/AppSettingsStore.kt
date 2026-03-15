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

package com.github.yumelira.yumebox.data.store

import com.github.yumelira.yumebox.data.model.AppColorTheme
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.tencent.mmkv.MMKV

class AppSettingsStorage(externalMmkv: MMKV) : MMKVPreference(externalMmkv = externalMmkv) {

    val onboardingCompleted by boolFlow(false)
    val privacyPolicyAccepted by boolFlow(false)

    val themeMode by enumFlow(ThemeMode.Auto)
    val colorTheme by enumFlow(AppColorTheme.ClassicMonochrome)
    val themeSeedColorArgb by longFlow(0xFFFFFFFFL)
    val automaticRestart by boolFlow(false)
    val autoUpdateCurrentProfileOnStart by boolFlow(true)
    val hideAppIcon by boolFlow(false)
    val excludeFromRecents by boolFlow(false)
    val showTrafficNotification by boolFlow(true)
    val bottomBarAutoHide by boolFlow(true)
    val topBarBlurEnabled by boolFlow(true)
    val pageScale by floatFlow(1.0f)
    val singleNodeTest by boolFlow(true)

    val customUserAgent by strFlow("")

}
