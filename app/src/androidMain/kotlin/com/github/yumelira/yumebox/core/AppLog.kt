package com.github.yumelira.yumebox.core

import android.util.Log
import com.github.yumelira.yumebox.service.LogRecordService
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

object AppLogBuffer {
    private const val MAX_SIZE = 1000
    private val buffer = ArrayDeque<String>(MAX_SIZE)
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
    
    @Synchronized
    fun clear() {
        buffer.clear()
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
