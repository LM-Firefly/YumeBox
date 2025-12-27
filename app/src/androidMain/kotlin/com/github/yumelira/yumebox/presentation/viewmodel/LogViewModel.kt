package com.github.yumelira.yumebox.presentation.viewmodel

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.data.model.AppLogBuffer
import com.github.yumelira.yumebox.service.LogRecordService
import dev.oom_wg.purejoy.mlang.MLang
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import timber.log.Timber

class LogViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _logFiles = MutableStateFlow<List<LogFileInfo>>(emptyList())
    val logFiles: StateFlow<List<LogFileInfo>> = _logFiles.asStateFlow()

    private val logDir: File
        get() = LogRecordService.getLogDir(getApplication())

    init {
        refreshLogFiles()
    }

    fun saveAppLog() {
        viewModelScope.launch(Dispatchers.IO) {
            val logs = AppLogBuffer.getSnapshot()
            if (logs.isEmpty()) return@launch
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "app_log_$timestamp.log"
            val file = File(logDir, fileName)
            try {
                file.writeText(logs.joinToString("\n"))
                refreshLogFiles()
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(getApplication(), MLang.Log.Message.Saved.format(file.name), android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Timber.e(e, MLang.Log.Message.SaveFailed.format(e.message ?: ""))
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(getApplication(), MLang.Log.Message.SaveFailed.format(e.message ?: ""), android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun startRecording() {
        LogRecordService.start(getApplication())
        _isRecording.value = true
        viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            refreshLogFiles()
        }
    }

    fun stopRecording() {
        LogRecordService.stop(getApplication())
        _isRecording.value = false
        viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            refreshLogFiles()
        }
    }

    fun refreshLogFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val isCurrentlyRecording = LogRecordService.isRecording
            val currentFileName = LogRecordService.currentLogFileName

            _isRecording.value = isCurrentlyRecording

            val files = logDir.listFiles { file ->
                file.isFile && file.name.startsWith(LogRecordService.LOG_PREFIX) && file.name.endsWith(LogRecordService.LOG_SUFFIX)
            }?.sortedByDescending { it.lastModified() } ?: emptyList()

            val fileInfos = files.map { file ->
                LogFileInfo(
                    file = file,
                    name = file.name,
                    createdAt = file.lastModified(),
                    size = file.length(),
                    isRecording = isCurrentlyRecording && file.name == currentFileName
                )
            }
            _logFiles.value = fileInfos
        }
    }

    fun deleteLogFile(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentFileName = LogRecordService.currentLogFileName
            if (LogRecordService.isRecording && file.name == currentFileName) {
                LogRecordService.stop(getApplication())
                _isRecording.value = false
                kotlinx.coroutines.delay(500)
            }

            if (file.exists()) {
                val deleted = file.delete()
                if (!deleted) {
                    timber.log.Timber.e(MLang.Log.Message.DeleteFailed.format(file.absolutePath))
                }
            }
            refreshLogFiles()
        }
    }

    fun deleteAllLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            if (LogRecordService.isRecording) {
                LogRecordService.stop(getApplication())
                _isRecording.value = false
                kotlinx.coroutines.delay(500)
            }

            logDir.listFiles()?.forEach { file ->
                if (file.exists()) {
                    file.delete()
                }
            }
            refreshLogFiles()
        }
    }

    suspend fun readLogContent(file: File): List<LogEntry> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) return@withContext emptyList()
            file.readLines().mapNotNull { line ->
                parseLogLine(line)
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, MLang.Log.Message.ReadFailed.format(e.message ?: ""))
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
        return try {
            val context = getApplication<Application>()
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } catch (e: Exception) {
            Timber.e(e, MLang.Log.Message.ShareFailed.format(e.message ?: ""))
            null
        }
    }

    data class LogFileInfo(
        val file: File, val name: String, val createdAt: Long, val size: Long, val isRecording: Boolean
    )

    data class LogEntry(
        val time: String, val level: LogMessage.Level, val message: String
    )
}
