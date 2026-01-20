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

package com.github.yumelira.yumebox.presentation.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.domain.facade.RuntimeFacade
import dev.oom_wg.purejoy.mlang.MLang
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class LogViewModel(
    application: Application,
    private val runtimeFacade: RuntimeFacade
) : AndroidViewModel(application) {

    val isRecording: StateFlow<Boolean> = runtimeFacade.isRecording

    private val _logFiles = MutableStateFlow<List<LogFileInfo>>(emptyList())
    val logFiles: StateFlow<List<LogFileInfo>> = _logFiles.asStateFlow()

    init {
        refreshLogFiles()
    }

    fun saveAppLog() {
        viewModelScope.launch {
            val result = runtimeFacade.saveAppLog()
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    val file = result.getOrNull()!!
                    android.widget.Toast.makeText(
                        getApplication(),
                        MLang.Log.Message.Saved.format(file.name),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    refreshLogFiles()
                } else {
                    val error = result.exceptionOrNull()
                    android.widget.Toast.makeText(
                        getApplication(),
                        MLang.Log.Message.SaveFailed.format(error?.message ?: ""),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun startRecording() {
        runtimeFacade.startRecording()
        viewModelScope.launch {
            delay(300)
            refreshLogFiles()
        }
    }

    fun stopRecording() {
        runtimeFacade.stopRecording()
        viewModelScope.launch {
            delay(300)
            refreshLogFiles()
        }
    }

    fun refreshLogFiles() {
        viewModelScope.launch {
            val files = runtimeFacade.getLogFiles()
            val isCurrentlyRecording = isRecording.value

            _logFiles.value = files.map { logFileInfo ->
                LogFileInfo(
                    file = logFileInfo.file,
                    name = logFileInfo.name,
                    createdAt = logFileInfo.lastModified,
                    size = logFileInfo.size,
                    isRecording = isCurrentlyRecording && logFileInfo.file.name.contains("recording")
                )
            }
        }
    }

    fun deleteLogFile(file: File) {
        viewModelScope.launch {
            val result = runtimeFacade.deleteLogFile(file)
            if (result.isSuccess) {
                refreshLogFiles()
            } else {
                Timber.e("Failed to delete log file: ${file.name}")
            }
        }
    }

    fun deleteAllLogs() {
        viewModelScope.launch {
            if (isRecording.value) {
                runtimeFacade.stopRecording()
                delay(500)
            }

            val result = runtimeFacade.clearAllLogs()
            if (result.isSuccess) {
                refreshLogFiles()
            } else {
                Timber.e("Failed to clear all logs")
            }
        }
    }

    suspend fun readLogContent(file: File): List<LogEntry> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) return@withContext emptyList()
            file.readLines().mapNotNull { line ->
                parseLogLine(line)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to read log file")
            emptyList()
        }
    }

    private fun parseLogLine(line: String): LogEntry? {
        if (line.isBlank()) return null
        val regex = """\[(.+?)] \[(.+?)] (.+)""".toRegex()
        val match = regex.find(line) ?: return null
        val (timeStr, levelStr, message) = match.destructured

        val level = try {
            LogMessage.Level.valueOf(levelStr)
        } catch (_: Exception) {
            LogMessage.Level.Unknown
        }

        return LogEntry(time = timeStr, level = level, message = message)
    }

    fun createShareIntent(file: File): Intent? {
        return runtimeFacade.shareLogFile(file)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    data class LogFileInfo(
        val file: File,
        val name: String,
        val createdAt: Long,
        val size: Long,
        val isRecording: Boolean
    )

    data class LogEntry(
        val time: String,
        val level: LogMessage.Level,
        val message: String
    )
}
