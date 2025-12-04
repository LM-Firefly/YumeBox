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

package com.github.yumelira.yumebox.clash.loader

import kotlinx.coroutines.*
import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.store.ProfilesStore
import com.github.yumelira.yumebox.domain.model.RunningMode
import timber.log.Timber

class ConfigAutoLoader(
    private val clashManager: ClashManager,
    private val profilesStore: ProfilesStore
) {
    companion object { private const val TAG = "ConfigAutoLoader" }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private suspend fun getRecommendedProfile(): Profile? {
        val allProfiles = profilesStore.getAllProfiles()
        val lastUsedId = profilesStore.lastUsedProfileId
        if (lastUsedId.isNotEmpty()) {
            allProfiles.find { it.id == lastUsedId }?.let { return it }
        }
        allProfiles.find { it.enabled }?.let { return it }
        return allProfiles.firstOrNull()
    }

    suspend fun reloadConfig(profileId: String? = null): Result<String> {
        return try {
            val profile = if (profileId != null) {
                profilesStore.getAllProfiles().find { it.id == profileId }
            } else {
                getRecommendedProfile()
            }

            if (profile == null) return Result.failure(IllegalArgumentException("配置不存在"))

            Timber.tag(TAG).d("手动重新加载配置: ${profile.name}")

            val willUseTun = clashManager.runningMode.value is RunningMode.Tun

            val result = clashManager.loadProfile(
                profile = profile,
                forceDownload = false,
                willUseTunMode = willUseTun,
                quickStart = true
            )

            if (result.isSuccess) {
                delay(500)
                clashManager.refreshProxyGroups()
                Timber.tag(TAG).d("配置重新加载成功")
            }

            result
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "手动重新加载配置失败: ${e.message}")
            Result.failure(e)
        }
    }

    fun cleanup() { 
        scope.cancel()
        Timber.tag(TAG).d("ConfigAutoLoader 已清理") 
    }
}
