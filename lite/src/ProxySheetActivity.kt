package com.github.yumelira.yumebox

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.yumelira.yumebox.common.util.AppLanguageManager
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import com.github.yumelira.yumebox.presentation.theme.ProvideAndroidPlatformTheme
import com.github.yumelira.yumebox.presentation.theme.YumeTheme
import org.koin.android.ext.android.inject

class ProxySheetActivity : ComponentActivity() {
    private val appSettingsStorage: AppSettingsStorage by inject()

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

            ProvideAndroidPlatformTheme {
                YumeTheme(
                    themeMode = themeMode,
                    themeSeedColorArgb = themeSeedColorArgb,
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
