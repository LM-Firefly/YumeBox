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
 * Copyright (c)  YumeLira 2025.
 *
 */

package com.github.yumelira.yumebox.screen.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.yumelira.yumebox.viewmodel.AppSettingsViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ActivationWizardScreenDestination
import com.ramcosta.composedestinations.generated.destinations.MainScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Scaffold

@Composable
@Destination<RootGraph>
fun ActivationWizardScreen(
    navigator: DestinationsNavigator,
    previewMode: Boolean = false,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()

    val wizardState = rememberWizardState(
        context = context,
        lifecycleOwner = lifecycleOwner,
        appSettingsViewModel = appSettingsViewModel,
        resetPrivacyAcceptedOnOpen = !previewMode,
    )

    val showPrivacySheet = remember { mutableStateOf(false) }

    Scaffold {
        OnboardingWizard(
            state = wizardState,
            onPrivacySheetRequest = { showPrivacySheet.value = true },
            onComplete = {
                if (previewMode) {
                    navigator.popBackStack()
                } else {
                    appSettingsViewModel.setOnboardingCompleted(true)
                    navigator.navigate(MainScreenDestination(initialPage = 0)) {
                        popUpTo(ActivationWizardScreenDestination) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            },
        )
    }

    PrivacyPolicySheet(show = showPrivacySheet)
}
