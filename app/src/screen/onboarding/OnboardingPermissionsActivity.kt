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
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.UserKey
import dev.oom_wg.purejoy.mlang.MLang

internal class OnboardingPermissionsActivity : OnboardingBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setOnboardingContent {
            val lifecycleOwner = LocalLifecycleOwner.current
            val permissionState = rememberPermissionState(
                context = this,
                lifecycleOwner = lifecycleOwner,
            )

            ProvisionDetailShell(
                previewIcon = Yume.UserKey,
                title = MLang.Onboarding.Permission.Title,
                subtitle = MLang.Onboarding.Permission.Subtitle,
                primaryText = MLang.Onboarding.Navigation.Next,
                primaryEnabled = true,
                onPrimaryClick = {
                    navigateForwardTo(OnboardingTermsActivity::class.java)
                },
                onBack = {
                    navigateBackwardTo(OnboardingStartupActivity::class.java)
                },
            ) {
                PermissionContent(permissionState)
            }
        }
    }

    override fun onBackPressed() {
        navigateBackwardTo(OnboardingStartupActivity::class.java)
    }
}
