/*
 * This file is part of FlyCat.
 *
 * FlyCat is free software: you can redistribute it and/or modify
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
 * Copyright (c)  YumeYucca 2025 - Present
 *
 */

package com.github.yumelira.yumebox

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.github.yumelira.yumebox.common.util.AppLanguageManager
import com.github.yumelira.yumebox.platform.runtime.StartupGate
import com.github.yumelira.yumebox.presentation.component.LocalTopBarHazeState
import com.github.yumelira.yumebox.presentation.component.LocalTopBarHazeStyle
import com.github.yumelira.yumebox.presentation.component.ToastDialogHost
import com.github.yumelira.yumebox.presentation.navigation.AppNavContainer
import com.github.yumelira.yumebox.presentation.theme.ProvideAndroidPlatformTheme
import com.github.yumelira.yumebox.presentation.theme.YumeTheme
import com.github.yumelira.yumebox.runtime.client.common.util.extractPendingImportUrl
import com.github.yumelira.yumebox.runtime.client.common.util.IntentController
import com.github.yumelira.yumebox.screen.onboarding.OnboardingLauncher
import com.github.yumelira.yumebox.screen.settings.AppSettingsViewModel
import com.tencent.mmkv.MMKV
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.theme.MiuixTheme

class MainActivity : FragmentActivity() {

    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 1001
        private const val EXTRA_EXIT_UI_WHEN_BACKGROUND = "exit_ui_when_background"
        private val _pendingImportUrl = MutableStateFlow<String?>(null)
        val pendingImportUrl: StateFlow<String?> = _pendingImportUrl.asStateFlow()

        fun clearPendingImportUrl() {
            _pendingImportUrl.value = null
        }

        private val _pendingDeepLink = MutableStateFlow<String?>(null)
        val pendingDeepLink: StateFlow<String?> = _pendingDeepLink.asStateFlow()

        fun clearPendingDeepLink() {
            _pendingDeepLink.value = null
        }
    }

    private val appSettingsReader: com.github.yumelira.yumebox.core.data.AppSettingsReader by
        inject()
    private val featureStoreReader: com.github.yumelira.yumebox.core.data.FeatureStoreReader by inject()
    private val proxyFacade: com.github.yumelira.yumebox.runtime.client.ProxyFacade by inject()

    private lateinit var intentController: IntentController

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        StartupGate.loadPrimary()
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        super.onCreate(savedInstanceState)
        applyExcludeFromRecents(appSettingsReader.excludeFromRecents.value)

        intentController = IntentController(lifecycleScope, packageName)
        handleIntent(intent)

        if (!appSettingsReader.initialSetupCompleted.value) {
            OnboardingLauncher.start(this, previewMode = false)
            finish()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION,
                )
            }
        }

        setContent {
            val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
            val themeMode = appSettingsViewModel.themeMode.state.collectAsStateWithLifecycle().value
            val themeSeedColorArgb =
                appSettingsViewModel.themeSeedColorArgb.state.collectAsStateWithLifecycle().value
            val invertOnPrimaryColors =
                appSettingsViewModel.invertOnPrimaryColors.state.collectAsStateWithLifecycle().value
            val excludeFromRecents =
                appSettingsViewModel.excludeFromRecents.state.collectAsStateWithLifecycle().value
            val topBarBlurEnabled =
                appSettingsViewModel.topBarBlurEnabled.state.collectAsStateWithLifecycle().value
            val pageScale = appSettingsViewModel.pageScale.state.collectAsStateWithLifecycle().value

            LaunchedEffect(excludeFromRecents) {
                this@MainActivity.applyExcludeFromRecents(excludeFromRecents)
            }

            ProvideAndroidPlatformTheme {
                val systemDensity = LocalDensity.current
                val scaledDensity =
                    remember(systemDensity, pageScale) {
                        Density(systemDensity.density * pageScale, systemDensity.fontScale)
                    }
                CompositionLocalProvider(LocalDensity provides scaledDensity) {
                    YumeTheme(
                        themeMode = themeMode,
                        themeSeedColorArgb = themeSeedColorArgb,
                        invertOnPrimaryColors = invertOnPrimaryColors,
                    ) {
                        val topBarHazeState = remember { HazeState() }
                        val topBarBackground = MiuixTheme.colorScheme.surface
                        val topBarHazeStyle =
                            remember(topBarBackground) {
                                HazeBlurStyle(
                                    backgroundColor = topBarBackground,
                                    colorEffects = listOf(HazeColorEffect.tint(topBarBackground.copy(0.8f))),
                                )
                            }
                        CompositionLocalProvider(
                            LocalTopBarHazeState provides
                                if (topBarBlurEnabled) topBarHazeState else null,
                            LocalTopBarHazeStyle provides
                                if (topBarBlurEnabled) topBarHazeStyle else null,
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MiuixTheme.colorScheme.surface,
                            ) {
                                AppNavContainer()
                                ToastDialogHost()
                            }
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

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level < TRIM_MEMORY_UI_HIDDEN || isFinishing) {
            return
        }
        if (!featureStoreReader.exitUiWhenBackground.value) {
            return
        }
        if (proxyFacade.runtimeSnapshot.value.running) {
            finishAndRemoveTask()
        }
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let { safeIntent ->
            if (safeIntent.getBooleanExtra(EXTRA_EXIT_UI_WHEN_BACKGROUND, false)) {
                finishAndRemoveTask()
                return
            }
            extractPendingImportUrl(safeIntent)?.let { _pendingImportUrl.value = it }
            safeIntent.data?.let { uri ->
                val scheme = uri.scheme
                if (scheme == "clash" || scheme == "clashmeta") {
                    val host = uri.host
                    if (host == "install-config") {
                        val configUrl = uri.getQueryParameter("url")
                        if (!configUrl.isNullOrBlank()) {
                            _pendingImportUrl.value = configUrl
                        }
                    }
                } else if (scheme == "yumebox") {
                    _pendingDeepLink.value = uri.toString()
                }
            }

            intentController.handleIntent(safeIntent)
        }
    }

    @Suppress("DEPRECATION")
    private fun applyExcludeFromRecents(exclude: Boolean) {
        runCatching {
            val am = getSystemService(ActivityManager::class.java) ?: return@runCatching
            val currentTaskId = taskId
            val task =
                am.appTasks.firstOrNull { appTask: ActivityManager.AppTask ->
                    val taskInfo = appTask.taskInfo ?: return@firstOrNull false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        taskInfo.taskId == currentTaskId
                    } else {
                        taskInfo.id == currentTaskId
                    }
                }
            task?.setExcludeFromRecents(exclude)
        }
    }
}
