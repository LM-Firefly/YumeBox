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

package com.github.yumelira.yumebox

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.yumelira.yumebox.common.util.AppLanguageManager
import com.github.yumelira.yumebox.data.store.AppSettingsStore
import com.github.yumelira.yumebox.presentation.theme.ProvideAndroidPlatformTheme
import com.github.yumelira.yumebox.presentation.theme.YumeTheme
import org.koin.android.ext.android.inject

class ProxySheetActivity : ComponentActivity() {
    private val appSettingsStorage: AppSettingsStore by inject()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFinishOnTouchOutside(true)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)

        setContent {
            val themeMode by appSettingsStorage.themeMode.state.collectAsState()
            val themeSeedColorArgb by appSettingsStorage.themeAccentColorArgb.state.collectAsState()
            val invertOnPrimaryColors by appSettingsStorage.invertOnPrimaryColors.state.collectAsState()

            ProvideAndroidPlatformTheme {
                YumeTheme(
                    themeMode = themeMode,
                    themeSeedColorArgb = themeSeedColorArgb,
                    invertOnPrimaryColors = invertOnPrimaryColors,
                ) {
                    ProxySheetContent(
                        onDismiss = {
                            finish()
                            @Suppress("DEPRECATION")
                            overridePendingTransition(0, 0)
                        },
                    )
                }
            }
        }
    }
}
