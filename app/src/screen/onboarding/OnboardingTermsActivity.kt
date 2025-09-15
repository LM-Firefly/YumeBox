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

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.ShieldCheck
import com.github.yumelira.yumebox.screen.settings.AppSettingsViewModel
import dev.oom_wg.purejoy.mlang.MLang
import org.koin.androidx.compose.koinViewModel

internal class OnboardingTermsActivity : OnboardingBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBackwardTo(OnboardingPermissionsActivity::class.java)
            }
        })

        setOnboardingContent {
            val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
            val privacyState = rememberPrivacyAcceptedState(appSettingsViewModel)
            val showPrivacySheet = remember { mutableStateOf(false) }

            ProvisionDetailShell(
                previewIcon = Yume.ShieldCheck,
                title = MLang.Onboarding.Privacy.Title,
                subtitle = MLang.Onboarding.Privacy.Subtitle,
                primaryText = MLang.Onboarding.Navigation.Next,
                primaryEnabled = privacyState.accepted,
                onPrimaryClick = {
                    if (privacyState.accepted) {
                        navigateForwardTo(OnboardingPersonalizeActivity::class.java)
                    }
                },
                onBack = {
                    navigateBackwardTo(OnboardingPermissionsActivity::class.java)
                },
            ) {
                TermsContent(
                    accepted = privacyState.accepted,
                    onAcceptedChange = privacyState.onAcceptedChange,
                    onPrivacySheetRequest = {
                        showPrivacySheet.value = true
                    },
                )
            }

            PrivacyPolicySheet(show = showPrivacySheet)
        }
    }
}
