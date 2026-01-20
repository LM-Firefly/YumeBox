package com.github.yumelira.yumebox.data.model

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import com.github.yumelira.yumebox.service.LogRecordService
import java.io.File
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess
import timber.log.Timber

object AppLogBuffer {
    private const val MAX_SIZE = 1000
    private val buffer = ArrayDeque<String>(MAX_SIZE)
    @SuppressLint("ConstantLocale")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    @Volatile
    var minLogLevel: Int = Log.DEBUG
    @Synchronized
    fun add(priority: Int, tag: String?, message: String) {
        if (priority < minLogLevel) return
        val time = dateFormat.format(Date())
        val level = when (priority) {
            Log.VERBOSE -> "VERBOSE"
            Log.DEBUG -> "DEBUG"
            Log.INFO -> "INFO"
            Log.WARN -> "WARN"
            Log.ERROR -> "ERROR"
            Log.ASSERT -> "ASSERT"
            else -> "UNKNOWN"
        }
        val logLine = "[$time] [$level] ${tag?.let { "$it: " } ?: ""}$message"
        if (buffer.size >= MAX_SIZE) {
            buffer.removeFirst()
        }
        buffer.addLast(logLine)
        // 如果正在录制，尝试写入 Service
        if (LogRecordService.isRecording) {
            LogRecordService.writeLog(logLine)
        }
    }
    @Synchronized
    fun getSnapshot(): List<String> {
        return buffer.toList()
    }
}

class AppLogTree : Timber.DebugTree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // 调用父类 log 以保持 Logcat 输出
        super.log(priority, tag, message, t)
        // 写入内存 Buffer
        AppLogBuffer.add(priority, tag, message)
        if (t != null) {
            AppLogBuffer.add(priority, tag, Log.getStackTraceString(t))
        }
    }
}

@SuppressLint("StaticFieldLeak")
object CrashHandler : Thread.UncaughtExceptionHandler {
    private var context: Context? = null
    private val defaultHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()
    fun init(context: Context) {
        this.context = context.applicationContext
        Thread.setDefaultUncaughtExceptionHandler(this)
    }
    override fun uncaughtException(thread: Thread, ex: Throwable) {
        if (!handleException(ex) && defaultHandler != null) {
            defaultHandler.uncaughtException(thread, ex)
        } else {
            exitProcess(1)
        }
    }
    private fun handleException(ex: Throwable?): Boolean {
        if (ex == null) return false
        saveCrashInfo2File(ex)
        return true
    }
    private fun saveCrashInfo2File(ex: Throwable) {
        val ctx = context ?: return
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "crash_$timestamp.log"
            val logDir = LogRecordService.getLogDir(ctx)
            val file = File(logDir, fileName)
            file.printWriter().use { writer ->
                collectDeviceInfo(ctx).forEach { (k, v) ->
                    writer.println("$k=$v")
                }
                ex.printStackTrace(writer)
                try {
                    val recentLogs = AppLogBuffer.getSnapshot()
                    if (recentLogs.isNotEmpty()) {
                        writer.println("\n--- Recent App Logs ---")
                        recentLogs.forEach { writer.println(it) }
                        writer.println("--- End Recent App Logs ---")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error appending recent logs")
                }
            }
            Timber.i("Crash log saved to ${file.absolutePath}")
        } catch (e: Exception) {
            Timber.e(e, "Error saving crash log")
        }
    }
    private fun collectDeviceInfo(context: Context): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        map["MANUFACTURER"] = Build.MANUFACTURER
        map["BRAND"] = Build.BRAND
        map["MODEL"] = Build.MODEL
        map["SDK_INT"] = Build.VERSION.SDK_INT.toString()
        map["RELEASE"] = Build.VERSION.RELEASE
        map["packageName"] = context.packageName
        return map
    }
}
