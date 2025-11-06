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



package com.github.yumelira.yumebox.screen.onboarding

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.github.yumelira.yumebox.MainActivity
import com.github.yumelira.yumebox.R
import com.github.yumelira.yumebox.common.runtime.StartupGate
import com.github.yumelira.yumebox.presentation.theme.ProvideAndroidPlatformTheme
import com.github.yumelira.yumebox.presentation.theme.YumeTheme
import com.github.yumelira.yumebox.screen.settings.AppSettingsViewModel
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal abstract class OnboardingBaseActivity : ComponentActivity() {

    protected val previewMode: Boolean
        get() = OnboardingLauncher.isPreviewMode(intent)

    override fun onCreate(savedInstanceState: Bundle?) {
        StartupGate.loadPrimary()
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        super.onCreate(savedInstanceState)
    }

    protected fun setOnboardingContent(
        content: @Composable () -> Unit,
    ) {
        setContent {
            OnboardingActivityTheme {
                content()
            }
        }
    }

    protected fun buildOnboardingIntent(
        target: Class<out ComponentActivity>,
        resetPrivacy: Boolean = false,
    ): Intent {
        return OnboardingLauncher.createIntent(
            context = this,
            target = target,
            previewMode = previewMode,
            resetPrivacy = resetPrivacy,
        )
    }

    protected fun navigateForwardTo(
        target: Class<out ComponentActivity>,
    ) {
        startActivity(buildOnboardingIntent(target))
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                R.anim.onboarding_slide_in_right,
                R.anim.onboarding_slide_out_left,
            )
        } else {
            overridePendingTransition(
                R.anim.onboarding_slide_in_right,
                R.anim.onboarding_slide_out_left,
            )
        }
        finish()
    }

    protected fun navigateBackwardTo(
        target: Class<out ComponentActivity>,
    ) {
        startActivity(buildOnboardingIntent(target))
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                R.anim.onboarding_slide_in_left,
                R.anim.onboarding_slide_out_right,
            )
        } else {
            overridePendingTransition(
                R.anim.onboarding_slide_in_left,
                R.anim.onboarding_slide_out_right,
            )
        }
        finish()
    }

    protected fun finishOnboarding() {
        if (previewMode) {
            finish()
            return
        }
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }
}

@Composable
private fun OnboardingActivityTheme(
    content: @Composable () -> Unit,
) {
    val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
    val themeMode by appSettingsViewModel.themeMode.state.collectAsState()
    val themeSeedColorArgb by appSettingsViewModel.themeSeedColorArgb.state.collectAsState()
    val pageScale by appSettingsViewModel.pageScale.state.collectAsState()

    ProvideAndroidPlatformTheme {
        val systemDensity = LocalDensity.current
        val scaledDensity = Density(
            density = systemDensity.density * pageScale,
            fontScale = systemDensity.fontScale,
        )
        CompositionLocalProvider(LocalDensity provides scaledDensity) {
            YumeTheme(
                themeMode = themeMode,
                themeSeedColorArgb = themeSeedColorArgb,
            ) {
                Scaffold { _ ->
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MiuixTheme.colorScheme.surface,
                        content = content,
                    )
                }
            }
        }
    }
}
