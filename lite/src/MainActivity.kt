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
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.github.yumelira.yumebox.common.runtime.StartupGate
import com.github.yumelira.yumebox.common.util.AppLanguageManager
import com.github.yumelira.yumebox.data.store.AppSettingsStore
import com.github.yumelira.yumebox.runtime.client.common.util.IntentController
import com.github.yumelira.yumebox.presentation.theme.NavigationTransitions
import com.github.yumelira.yumebox.presentation.theme.ProvideAndroidPlatformTheme
import com.github.yumelira.yumebox.presentation.theme.YumeTheme
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.ImportConfigScreenDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.android.ext.android.inject
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.theme.MiuixTheme

class MainActivity : FragmentActivity() {
    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 1001

        private val _pendingImportUrl = MutableStateFlow<String?>(null)
        val pendingImportUrl: StateFlow<String?> = _pendingImportUrl.asStateFlow()

        fun clearPendingImportUrl() {
            _pendingImportUrl.value = null
        }
    }

    private val appSettingsStorage: AppSettingsStore by inject()
    private lateinit var intentController: IntentController

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        StartupGate.loadPrimary(this)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        super.onCreate(savedInstanceState)
        intentController = IntentController(lifecycleScope, packageName)
        handleIntent(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATION_PERMISSION,
            )
        }

        setContent {
            val themeMode by appSettingsStorage.themeMode.state.collectAsStateWithLifecycle()
            val themeSeedColorArgb by appSettingsStorage.themeAccentColorArgb.state.collectAsStateWithLifecycle()
            val invertOnPrimaryColors by appSettingsStorage.invertOnPrimaryColors.state.collectAsStateWithLifecycle()
            val smoothCornerEnabled by appSettingsStorage.smoothCornerEnabled.state.collectAsStateWithLifecycle()
            val pendingImportValue by pendingImportUrl.collectAsStateWithLifecycle()
            val navController = rememberNavController()

            LaunchedEffect(pendingImportValue) {
                val url = pendingImportValue ?: return@LaunchedEffect
                navController.navigate(ImportConfigScreenDestination(prefillUrl = url).route) {
                    launchSingleTop = true
                }
                clearPendingImportUrl()
            }

            ProvideAndroidPlatformTheme {
                YumeTheme(
                    themeMode = themeMode,
                    themeSeedColorArgb = themeSeedColorArgb,
                    invertOnPrimaryColors = invertOnPrimaryColors,
                    smoothCornerEnabled = smoothCornerEnabled,
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MiuixTheme.colorScheme.surface,
                    ) {
                        Scaffold {
                            DestinationsNavHost(
                                navGraph = NavGraphs.root,
                                navController = navController,
                                defaultTransitions = NavigationTransitions.defaultStyle,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val safeIntent = intent ?: return
        safeIntent.data?.let { uri ->
            val scheme = uri.scheme
            if ((scheme == "clash" || scheme == "clashmeta") && uri.host == "install-config") {
                uri.getQueryParameter("url")
                    ?.takeIf { it.isNotBlank() }
                    ?.let { _pendingImportUrl.value = it }
            }
        }

        intentController.handleIntent(safeIntent)
    }
}
