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



package com.github.yumelira.yumebox.feature.editor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class ConfigEditorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _previewContent = MutableStateFlow("")
    val previewContent: StateFlow<String> = _previewContent.asStateFlow()

    private var currentConfigId: String? = null
    private var currentConfigType: ConfigType? = null

    fun loadConfig(configId: String, configType: ConfigType) {
        viewModelScope.launch {
            _isLoading.value = true
            currentConfigId = configId
            currentConfigType = configType

            try {

                Timber.d("Config loaded: id=$configId, type=$configType")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load config: $configId")
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadPreviewContent(previewType: PreviewType) {
        viewModelScope.launch {
            _isLoading.value = true

            try {

                Timber.d("Preview content loaded: type=$previewType")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load preview content")
                _previewContent.value = ""
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveConfig(content: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {

                Timber.d("Config saved: id=$currentConfigId")
                _uiState.value = _uiState.value.copy(isSaved = true)
            } catch (e: Exception) {
                Timber.e(e, "Failed to save config")
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateDraft(content: String) {
        _uiState.value = _uiState.value.copy(
            draftContent = content,
            isSaved = false
        )
    }

    fun formatContent(content: String): String? {
        return try {

            content.trimIndent()
        } catch (e: Exception) {
            Timber.e(e, "Failed to format content")
            null
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class EditorUiState(
    val content: String = "",
    val draftContent: String = "",
    val isSaved: Boolean = true,
    val error: String? = null
)

enum class ConfigType {
    Override,
    Profile
}

enum class PreviewType {
    RuntimeConfig,
    OverridePreview
}
