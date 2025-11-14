package com.github.yumelira.yumebox.feature.meta.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.core.model.ConfigurationOverride
import com.github.yumelira.yumebox.core.model.OverrideInternalConstants
import com.github.yumelira.yumebox.data.repository.ActiveProfileOverrideReloader
import com.github.yumelira.yumebox.data.repository.OverrideConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CustomRoutingViewModel(
    private val overrideConfigRepository: OverrideConfigRepository,
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
