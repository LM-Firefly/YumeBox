package com.github.yumelira.yumebox.data.repository

import android.app.Application
import android.net.Uri
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.service.LogRecordService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class LogRepository(
    private val application: Application,
) {
    companion object {
        private const val STOP_WAIT_MS = 300L
        private val LOG_LINE_REGEX = """\[(.+?)] \[(.+?)] (.+)""".toRegex()
        private val LOG_LEVELS = enumValues<LogMessage.Level>().associateBy { it.name }
    }

    private val logDir: File
        get() = LogRecordService.getLogDir(application)

    fun startRecording() {
        LogRecordService.start(application)
    }

    fun stopRecording() {
        LogRecordService.stop(application)
    }

    fun isRecording(): Boolean {
        return LogRecordService.isRecording
    }

    fun isCurrentRecordingFile(fileName: String): Boolean {
        return isRecording() && LogRecordService.currentLogFileName == fileName
    }

    fun listLogFiles(): List<LogFileInfo> {
        val currentlyRecording = isRecording()
        val currentFileName = LogRecordService.currentLogFileName
        val files = logDir.listFiles(::isManagedLogFile)?.sortedByDescending { it.lastModified() } ?: emptyList()
        return files.map { file ->
            LogFileInfo(
                name = file.name,
                createdAt = file.lastModified(),
                size = file.length(),
                isRecording = currentlyRecording && file.name == currentFileName,
            )
        }
    }

    suspend fun getLogFileSize(fileName: String): Long? = withContext(Dispatchers.IO) {
        resolveLogFile(fileName)?.length()
    }

    suspend fun readLogEntries(fileName: String): List<LogEntry> = withContext(Dispatchers.IO) {
        val file = resolveLogFile(fileName) ?: return@withContext emptyList()
        try {
            file.useLines { lines -> lines.mapNotNull(::parseLogLine).toList() }
        } catch (_: IOException) {
            emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    suspend fun exportLogFile(fileName: String, targetUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val source = resolveLogFile(fileName) ?: return@withContext false
        try {
            application.contentResolver.openOutputStream(targetUri)?.use { output ->
                source.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: return@withContext false
            true
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    suspend fun deleteLogFile(fileName: String): Boolean = withContext(Dispatchers.IO) {
        val file = resolveLogFile(fileName) ?: return@withContext false
        if (isCurrentRecordingFile(file.name)) {
            stopRecording()
            delay(STOP_WAIT_MS)
        }
        file.delete()
    }

    suspend fun deleteAllLogs() = withContext(Dispatchers.IO) {
        if (isRecording()) {
            stopRecording()
            delay(STOP_WAIT_MS)
        }
        val files = logDir.listFiles(::isManagedLogFile) ?: return@withContext
        files.forEach { it.delete() }
    }

    private fun parseLogLine(line: String): LogEntry? {
        if (line.isBlank()) return null
        val match = LOG_LINE_REGEX.find(line) ?: return null
        val (timeStr, levelStr, message) = match.destructured
        val level = LOG_LEVELS[levelStr] ?: LogMessage.Level.Unknown
        return LogEntry(time = timeStr, level = level, message = message)
    }

    private fun resolveLogFile(fileName: String): File? {
        if (!isSafeLogFileName(fileName)) return null
        val file = File(logDir, fileName)
        return file.takeIf(::isManagedLogFile)
    }

    private fun isManagedLogFile(file: File): Boolean {
        if (!file.exists() || !file.isFile) return false
        if (!file.name.endsWith(LogRecordService.LOG_SUFFIX)) return false
        val prefix = LogRecordService.LOG_PREFIX
        return prefix.isBlank() || file.name.startsWith(prefix)
    }

    private fun isSafeLogFileName(fileName: String): Boolean {
        if (fileName.isBlank()) return false
        if (fileName.contains('/') || fileName.contains('\\') || fileName.contains("..")) return false
        if (!fileName.endsWith(LogRecordService.LOG_SUFFIX)) return false
        val prefix = LogRecordService.LOG_PREFIX
        return prefix.isBlank() || fileName.startsWith(prefix)
    }

    data class LogFileInfo(
        val name: String,
        val createdAt: Long,
        val size: Long,
        val isRecording: Boolean,
    )

    data class LogEntry(
        val time: String,
        val level: LogMessage.Level,
        val message: String,
    )
}
