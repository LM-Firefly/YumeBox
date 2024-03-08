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

package com.github.yumelira.yumebox.common.util

import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.data.repository.ProxyConnectionService
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import com.github.yumelira.yumebox.data.store.NetworkSettingsStorage
import com.github.yumelira.yumebox.data.store.ProfilesStore
import kotlinx.coroutines.delay
import timber.log.Timber

object ProxyAutoStartHelper {

    private const val TAG = "ProxyAutoStartHelper"

    suspend fun checkAndAutoStart(
        proxyConnectionService: ProxyConnectionService,
        appSettingsStorage: AppSettingsStorage,
        networkSettingsStorage: NetworkSettingsStorage,
        profilesStore: ProfilesStore,
        clashManager: ClashManager,
        isBootCompleted: Boolean = false
    ) {
        try {
            val automaticRestart = appSettingsStorage.automaticRestart.value
            if (!automaticRestart) {
                Timber.tag(TAG).d("自动启动已禁用")
                return
            }

            if (clashManager.isRunning.value) {
                Timber.tag(TAG).d("代理已在运行，跳过自动启动")
                return
            }

            val profileId = getProfileToStart(profilesStore)
            if (profileId == null) {
                Timber.tag(TAG).w("没有可用的配置文件，无法自动启动")
                return
            }

            if (isBootCompleted) {
                Timber.tag(TAG).d("开机自启：延迟 3 秒后启动...")
                delay(3000)
            }

            val proxyMode = networkSettingsStorage.proxyMode.value
            Timber.tag(TAG).d("自动启动代理: profileId=$profileId, mode=$proxyMode")

            val result = proxyConnectionService.startDirect(
                profileId = profileId, mode = proxyMode
            )

            if (result.isSuccess) {
                Timber.tag(TAG).d("自动启动代理成功")
            } else {
                Timber.tag(TAG).e("自动启动代理失败: ${result.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "自动启动代理失败: ${e.message}")
        }
    }

    private fun getProfileToStart(profilesStore: ProfilesStore): String? {
        val lastUsedId = profilesStore.lastUsedProfileId
        if (lastUsedId.isNotEmpty()) {
            val lastUsedProfile = profilesStore.getAllProfiles().find { it.id == lastUsedId }
            if (lastUsedProfile != null) {
                Timber.tag(TAG).d("使用上次使用的配置: ${lastUsedProfile.name}")
                return lastUsedId
            }
        }

        val enabledProfile = profilesStore.getAllProfiles().find { it.enabled }
        if (enabledProfile != null) {
            Timber.tag(TAG).d("使用已启用的配置: ${enabledProfile.name}")
            return enabledProfile.id
        }

        val firstProfile = profilesStore.getAllProfiles().firstOrNull()
        if (firstProfile != null) {
            Timber.tag(TAG).d("使用第一个配置: ${firstProfile.name}")
            return firstProfile.id
        }

        return null
    }
}
