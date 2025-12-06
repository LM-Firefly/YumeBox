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



package com.github.yumelira.yumebox.data.controller

import com.github.yumelira.yumebox.core.model.ConfigurationOverride
import com.github.yumelira.yumebox.data.store.OverrideConfigProvider
import com.github.yumelira.yumebox.data.store.OverrideConfigStore
import com.github.yumelira.yumebox.data.store.ProfileBindingProvider
import com.github.yumelira.yumebox.data.model.OverrideConfig
import com.github.yumelira.yumebox.data.model.OverrideMetadata
import com.github.yumelira.yumebox.data.model.ProfileBinding
import java.util.*

class OverrideResolver(
    private val configProvider: OverrideConfigProvider,
    private val bindingProvider: ProfileBindingProvider,
) {

    suspend fun resolveIds(profileId: UUID): List<String> {
        val binding = bindingProvider.getBinding(profileId.toString())
        return resolveBindingIds(binding)
    }

    suspend fun resolveIds(profileId: String): List<String> {
        val binding = bindingProvider.getBinding(profileId)
        return resolveBindingIds(binding)
    }

    suspend fun resolveConfigs(overrideIds: List<String>): List<ConfigurationOverride> {
        return resolveOrderedConfigs(overrideIds).map(OverrideConfig::config)
    }

    suspend fun getProfilesUsingOverride(overrideId: String): List<String> {
        return bindingProvider.getProfilesUsingOverride(overrideId)
    }

    suspend fun isOverrideInUse(overrideId: String): Boolean {
        return bindingProvider.isOverrideInUse(overrideId)
    }

    suspend fun getOverrideUsageCount(overrideId: String): Int {
        return bindingProvider.getOverrideUsageCount(overrideId)
    }

    suspend fun getBinding(profileId: String) = bindingProvider.getBinding(profileId)

    suspend fun bindOverride(profileId: String, overrideId: String, index: Int? = null) {
        bindingProvider.addOverride(profileId, overrideId, index)
    }

    suspend fun setOverrides(profileId: String, overrideIds: List<String>) {
        val binding = bindingProvider.getBinding(profileId)
        if (binding != null) {
            bindingProvider.setBinding(binding.setOverrides(overrideIds))
        } else {
            bindingProvider.setBinding(ProfileBinding.withOverrides(profileId, overrideIds))
        }
    }

    suspend fun clearBinding(profileId: String) {
        bindingProvider.removeBinding(profileId)
    }

    suspend fun enableOverride(profileId: String) {
        bindingProvider.enableOverride(profileId)
    }

    suspend fun disableOverride(profileId: String) {
        bindingProvider.disableOverride(profileId)
    }

    private fun resolveBindingIds(binding: ProfileBinding?): List<String> {
        if (binding == null) {
            return emptyList()
        }
        return buildList {
            if (binding.enabled) {
                add(OverrideMetadata.SYSTEM_PRESET_ID)
            }
            addAll(binding.overrideIds.filterNot(OverrideConfigStore::isInternalRuntimeConfig))
        }.distinct()
    }

    private suspend fun resolveOrderedConfigs(
        overrideIds: List<String>,
    ): List<OverrideConfig> {
        return overrideIds.mapNotNull { overrideId ->
            configProvider.getById(overrideId)
        }
    }
}
