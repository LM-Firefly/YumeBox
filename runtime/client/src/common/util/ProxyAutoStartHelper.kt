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



package com.github.yumelira.yumebox.common.util

import android.content.Context
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import com.github.yumelira.yumebox.data.store.NetworkSettingsStorage
import com.github.yumelira.yumebox.runtime.client.ProfilesRepository
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
import com.github.yumelira.yumebox.service.StatusProvider
import com.github.yumelira.yumebox.service.runtime.entity.Profile
import com.tencent.mmkv.MMKV
import timber.log.Timber

object ProxyAutoStartHelper {

    private const val TAG = "ProxyAutoStartHelper"

    suspend fun checkAndAutoStart(
        context: Context,
        proxyFacade: ProxyFacade,
        profilesRepository: ProfilesRepository,
        appSettingsStorage: AppSettingsStorage,
        networkSettingsStorage: NetworkSettingsStorage,
        serviceCache: MMKV
    ) {
        val activeProfile = try {
            profilesRepository.queryActiveProfile()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to load active profile")
            return
        }

        tryUpdateActiveProfileOnStart(
            appSettingsStorage = appSettingsStorage,
            profilesRepository = profilesRepository,
            activeProfile = activeProfile
        )

        val automaticRestart = appSettingsStorage.automaticRestart.value
        if (!automaticRestart) {
            return
        }

        if (proxyFacade.runtimeSnapshot.value.running || StatusProvider.serviceRunning) {
            return
        }

        if (activeProfile == null) {
            Timber.tag(TAG).w("No active profile for auto start")
            return
        }

        val mode = networkSettingsStorage.proxyMode.value

        try {
            profilesRepository.setActiveProfile(activeProfile.uuid)
            proxyFacade.startProxy(mode)
            Timber.tag(TAG).i("Auto start ok: profile=${activeProfile.uuid}, mode=$mode")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Auto start failed: ${e.message}")
        }
    }

    private suspend fun tryUpdateActiveProfileOnStart(
        appSettingsStorage: AppSettingsStorage,
        profilesRepository: ProfilesRepository,
        activeProfile: Profile?
    ) {
        if (!appSettingsStorage.autoUpdateCurrentProfileOnStart.value) {
            return
        }

        if (activeProfile == null) {
            Timber.tag(TAG).d("Skip auto update: no active profile")
            return
        }

        if (activeProfile.type != Profile.Type.Url) {
            Timber.tag(TAG).d("Skip auto update: unsupported profile type=${activeProfile.type}")
            return
        }

        try {
            profilesRepository.updateProfile(activeProfile.uuid)
            Timber.tag(TAG).i("Auto update on start ok: ${activeProfile.uuid}")
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Auto update on start failed")
        }
    }
}
