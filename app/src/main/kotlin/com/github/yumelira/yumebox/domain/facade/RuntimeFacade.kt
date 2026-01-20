package com.github.yumelira.yumebox.domain.facade

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.github.yumelira.yumebox.data.model.AppLogBuffer
import com.github.yumelira.yumebox.data.model.DailyTrafficSummary
import com.github.yumelira.yumebox.data.model.ProfileTrafficUsage
import com.github.yumelira.yumebox.data.model.TrafficSlotData
import com.github.yumelira.yumebox.data.repository.OverrideRepository
import com.github.yumelira.yumebox.data.store.TrafficStatisticsStorage
import com.github.yumelira.yumebox.service.LogRecordService
import com.github.yumelira.yumebox.core.model.ConfigurationOverride
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber

class RuntimeFacade(
    private val context: Context,
    private val overrideRepository: OverrideRepository,
    private val trafficStatisticsStore: TrafficStatisticsStorage
) {
    data class LogFileInfo(
        val name: String,
        val size: Long,
        val lastModified: Long,
        val file: File
    )

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val logDir: File
        get() = LogRecordService.getLogDir(context)

    fun startRecording() {
        LogRecordService.start(context)
        _isRecording.value = true
    }

    fun stopRecording() {
        LogRecordService.stop(context)
        _isRecording.value = false
    }

    suspend fun saveAppLog(): Result<File> = withContext(Dispatchers.IO) {
        try {
            val logs = AppLogBuffer.getSnapshot()
            if (logs.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("No logs to save"))
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "app_log_$timestamp.log"
            val file = File(logDir, fileName)

            file.writeText(logs.joinToString("\n"))
            Result.success(file)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save app log")
            Result.failure(e)
        }
    }

    suspend fun getLogFiles(): List<LogFileInfo> = withContext(Dispatchers.IO) {
        try {
            if (!logDir.exists()) {
                logDir.mkdirs()
            }

            logDir.listFiles()
                ?.filter { it.isFile && it.extension == "log" }
                ?.map { file ->
                    LogFileInfo(
                        name = file.name,
                        size = file.length(),
                        lastModified = file.lastModified(),
                        file = file
                    )
                }
                ?.sortedByDescending { it.lastModified }
                ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get log files")
            emptyList()
        }
    }

    suspend fun deleteLogFile(file: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (file.exists() && file.delete()) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("File does not exist or could not be deleted"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete log file: ${file.name}")
            Result.failure(e)
        }
    }

    fun shareLogFile(file: File): Intent? {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to share log file: ${file.name}")
            null
        }
    }

    suspend fun clearAllLogs(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val files = logDir.listFiles()?.filter { it.isFile && it.extension == "log" } ?: emptyList()
            var deletedCount = 0

            files.forEach { file ->
                if (file.delete()) {
                    deletedCount++
                }
            }

            Result.success(deletedCount)
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear all logs")
            Result.failure(e)
        }
    }

    fun loadPersist(): Result<ConfigurationOverride> = overrideRepository.loadPersist()

    fun savePersist(override: ConfigurationOverride): Result<Unit> = overrideRepository.savePersist(override)

    fun clearPersist(): Result<Unit> = overrideRepository.clearPersist()

    fun loadSession(): Result<ConfigurationOverride> = overrideRepository.loadSession()

    fun saveSession(override: ConfigurationOverride): Result<Unit> = overrideRepository.saveSession(override)

    fun updatePersist(transform: (ConfigurationOverride) -> ConfigurationOverride): Result<ConfigurationOverride> {
        return overrideRepository.updatePersist(transform)
    }

    fun updateSession(transform: (ConfigurationOverride) -> ConfigurationOverride): Result<ConfigurationOverride> {
        return overrideRepository.updateSession(transform)
    }

    val dailySummaries: StateFlow<Map<Long, DailyTrafficSummary>> = trafficStatisticsStore.dailySummaries
    val profileUsages: StateFlow<Map<String, ProfileTrafficUsage>> = trafficStatisticsStore.profileUsages

    fun getTodaySummary(): DailyTrafficSummary = trafficStatisticsStore.getTodaySummary()

    fun getYesterdaySummary(): DailyTrafficSummary = trafficStatisticsStore.getYesterdaySummary()

    fun getDailySummaries(days: Int): List<DailyTrafficSummary> = trafficStatisticsStore.getDailySummaries(days)

    fun getTodayHourlyData(): List<TrafficSlotData> = trafficStatisticsStore.getTodayHourlyData()
}
