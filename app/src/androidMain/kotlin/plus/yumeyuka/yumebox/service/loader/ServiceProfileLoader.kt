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

package com.github.yumelira.yumebox.service.loader

import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.store.ProfilesStore
import timber.log.Timber

class ServiceProfileLoader(
    private val clashManager: ClashManager,
    private val profilesStore: ProfilesStore
) {
    companion object {
        private const val TAG = "ServiceProfileLoader"
    }
    
    suspend fun loadIfNeeded(
        profileId: String,
        willUseTunMode: Boolean,
        quickStart: Boolean = false
    ): Result<Profile> {
        val profile = profilesStore.getAllProfiles().find { it.id == profileId }
            ?: return Result.failure(ProfileNotFoundException(profileId))

        val currentProfile = clashManager.currentProfile.value
        if (currentProfile != null && currentProfile.id == profile.id) {
            Timber.tag(TAG).d("当前配置已加载: ${profile.name}")
            return Result.success(profile)
        }
        
        val startTime = System.currentTimeMillis()
        Timber.tag(TAG).d("正在加载配置: ${profile.name}, quickStart=$quickStart")

        val loadResult = clashManager.loadProfile(
            profile,
            forceDownload = false,
            willUseTunMode = willUseTunMode,
            quickStart = quickStart
        )
        
        val elapsed = System.currentTimeMillis() - startTime
        
        return if (loadResult.isSuccess) {
            Timber.tag(TAG).d("配置加载成功: ${profile.name}, 耗时: ${elapsed}ms")
            Result.success(profile)
        } else {
            val error = loadResult.exceptionOrNull()
            Timber.tag(TAG).e("配置加载失败: ${profile.name}, 原因: ${error?.message}")
            Result.failure(
                ProfileLoadException(
                    profileId,
                    error?.message ?: "未知错误"
                )
            )
        }
    }
}

class ProfileNotFoundException(profileId: String) : Exception("未找到配置文件: $profileId")

class ProfileLoadException(profileId: String, reason: String) : Exception("配置加载失败 [$profileId]: $reason")
