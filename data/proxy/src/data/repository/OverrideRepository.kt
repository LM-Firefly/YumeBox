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



package com.github.yumelira.yumebox.data.repository

import android.content.Context
import com.github.yumelira.yumebox.core.model.ConfigurationOverride
import com.github.yumelira.yumebox.domain.model.OverrideConfig
import com.github.yumelira.yumebox.remote.ServiceClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class OverrideRepository(
    private val context: Context,
    private val configRepository: OverrideConfigRepository,
) {
    suspend fun updateProfile(
        transform: (ConfigurationOverride) -> ConfigurationOverride,
    ): Result<ConfigurationOverride> = updateInternal(transform)

    private suspend fun loadInternal(): Result<ConfigurationOverride> = runCatching {
        val activeProfile = queryActiveProfile() ?: return@runCatching ConfigurationOverride()
        configRepository.getById(runtimeOverrideId(activeProfile.uuid))
            ?.config
            ?: ConfigurationOverride()
    }

    private suspend fun saveInternal(override: ConfigurationOverride): Result<Unit> = runCatching {
        if (override == ConfigurationOverride()) {
            clearInternal().getOrThrow()
            return@runCatching
        }
        val activeProfile = requireActiveProfile()
        val configId = runtimeOverrideId(activeProfile.uuid)
        val existing = configRepository.getById(configId)
        configRepository.save(
            OverrideConfig(
                id = configId,
                name = INTERNAL_RUNTIME_NAME,
                description = "internal runtime override for ${activeProfile.uuid}",
                config = override,
                isSystem = false,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun clearInternal(): Result<Unit> = runCatching {
        val activeProfile = queryActiveProfile() ?: return@runCatching
        configRepository.delete(runtimeOverrideId(activeProfile.uuid))
    }

    private suspend fun updateInternal(
        transform: (ConfigurationOverride) -> ConfigurationOverride,
    ): Result<ConfigurationOverride> {
        val current = loadInternal().getOrElse { return Result.failure(it) }
        val updated = transform(current)
        val saveResult = saveInternal(updated)
        if (saveResult.isFailure) {
            return Result.failure(saveResult.exceptionOrNull() ?: IllegalStateException("保存运行时覆写失败"))
        }
        return Result.success(updated)
    }

    private suspend fun requireActiveProfile(): com.github.yumelira.yumebox.service.runtime.entity.Profile {
        return queryActiveProfile() ?: error("No active profile selected")
    }

    private suspend fun queryActiveProfile(): com.github.yumelira.yumebox.service.runtime.entity.Profile? {
        return withContext(Dispatchers.IO) {
            ServiceClient.connect(context)
            ServiceClient.profile().queryActive()
        }
    }

    private fun runtimeOverrideId(profileUuid: UUID): String {
        return "${OverrideConfigRepository.INTERNAL_RUNTIME_PREFIX}-profile-$profileUuid"
    }

    private companion object {
        private const val INTERNAL_RUNTIME_NAME = "运行时覆写"
    }
}
