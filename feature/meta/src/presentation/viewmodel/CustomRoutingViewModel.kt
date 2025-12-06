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

package com.github.yumelira.yumebox.feature.meta.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.core.model.ConfigurationOverride
import com.github.yumelira.yumebox.core.model.OverrideInternalConstants
import com.github.yumelira.yumebox.data.controller.ActiveProfileOverrideReloader
import com.github.yumelira.yumebox.data.store.OverrideConfigStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CustomRoutingViewModel(
    private val overrideConfigRepository: OverrideConfigStore,
    private val activeProfileOverrideReloader: ActiveProfileOverrideReloader,
) : ViewModel() {

    private val _config = MutableStateFlow(ConfigurationOverride())
    val config: StateFlow<ConfigurationOverride> = _config.asStateFlow()

    init {
        viewModelScope.launch {
            val customConfig = overrideConfigRepository.loadCustomRouting()
            _config.value = customConfig ?: ConfigurationOverride()
        }
    }

    fun updateConfig(updatedConfig: ConfigurationOverride) {
        _config.value = updatedConfig
    }

    suspend fun saveConfig(updatedConfig: ConfigurationOverride): Boolean {
        return runCatching {
            _config.value = updatedConfig
            overrideConfigRepository.saveCustomRouting(updatedConfig)
            activeProfileOverrideReloader.reapplyActiveProfileIfUsingOverride(
                OverrideInternalConstants.CUSTOM_ROUTING_OVERRIDE_ID,
            )
        }.getOrElse {
            false
        }
    }
}
