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
import com.github.yumelira.yumebox.common.util.openUrl
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import org.koin.android.ext.android.inject

internal class OnboardingFinishActivity : OnboardingBaseActivity() {

    private val appSettingsStorage: AppSettingsStorage by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBackwardTo(OnboardingPersonalizeActivity::class.java)
            }
        })

        setOnboardingContent {
            FinishHeroShell(
                enabled = true,
                onPrimaryClick = {
                    if (!previewMode) {
                        appSettingsStorage.initialSetupCompleted.set(true)
                    }
                    finishOnboarding()
                },
                onGithubClick = {
                    openUrl(this, "https://github.com/YumeLira/YumeBox")
                },
                onCommunityClick = {
                    openUrl(this, "https://t.me/YumeBox")
                },
            )
        }
    }
}
