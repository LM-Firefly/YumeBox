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



package com.github.yumelira.yumebox.screen.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.data.store.LogStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogViewModel(
    private val repository: LogStore,
) : ViewModel() {

    private val _isRecording = MutableStateFlow(repository.isRecording())
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _logFiles = MutableStateFlow<List<LogStore.LogFileInfo>>(emptyList())
    val logFiles: StateFlow<List<LogStore.LogFileInfo>> = _logFiles.asStateFlow()

    init {
        refreshRecordingState()
        refreshLogFiles()
    }

    fun startRecording() {
        repository.startRecording()
        refreshRecordingState()
        viewModelScope.launch {
            delay(300)
            refreshRecordingState()
            refreshLogFiles()
        }
    }

    fun stopRecording() {
        repository.stopRecording()
        refreshRecordingState()
        viewModelScope.launch {
            delay(300)
            refreshRecordingState()
            refreshLogFiles()
        }
    }

    fun refreshRecordingState() {
        _isRecording.value = repository.isRecording()
    }

    fun refreshLogFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            _logFiles.value = repository.listLogFiles()
        }
    }

    fun isCurrentFileRecording(fileName: String): Boolean {
        return repository.isCurrentRecordingFile(fileName)
    }

    suspend fun readLogContent(fileName: String): List<LogStore.LogEntry> = withContext(Dispatchers.IO) {
        repository.readLogEntries(fileName)
    }

    suspend fun exportMergedLog(fileName: String): String? = withContext(Dispatchers.IO) {
        repository.exportMergedLog(fileName)
    }

    fun deleteLogFile(fileName: String) {
        viewModelScope.launch {
            val deleted = repository.deleteLogFile(fileName)
            if (!deleted) return@launch
            refreshLogFiles()
        }
    }

    fun deleteAllLogs() {
        viewModelScope.launch {
            if (repository.isRecording()) {
                repository.stopRecording()
                refreshRecordingState()
                delay(300)
            }
            repository.deleteAllLogs()
            refreshRecordingState()
            refreshLogFiles()
        }
    }

    suspend fun exportLogToUri(fileName: String, targetUri: android.net.Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            repository.exportLogFile(fileName, targetUri)
            true
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            false
        }
    }
}
