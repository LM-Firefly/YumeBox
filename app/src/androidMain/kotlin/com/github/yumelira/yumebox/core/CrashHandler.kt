package com.github.yumelira.yumebox.core

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Looper
import android.widget.Toast
import com.github.yumelira.yumebox.service.LogRecordService
import timber.log.Timber
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

class CrashHandler private constructor(private val context: Context) : Thread.UncaughtExceptionHandler {
    private val defaultHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()
    override fun uncaughtException(thread: Thread, ex: Throwable) {
        if (!handleException(ex) && defaultHandler != null) {
            defaultHandler.uncaughtException(thread, ex)
        } else {
            try {
                Thread.sleep(3000)
            } catch (e: InterruptedException) {
                Timber.e(e)
            }
            exitProcess(1)
        }
    }

    private fun handleException(ex: Throwable?): Boolean {
        if (ex == null) return false
        object : Thread() {
            override fun run() {
                Looper.prepare()
                Toast.makeText(context, "App crashed, log saved.", Toast.LENGTH_LONG).show()
                Looper.loop()
            }
        }.start()
        saveCrashInfo2File(ex)
        return true
    }

    private fun saveCrashInfo2File(ex: Throwable) {
        val sb = StringBuilder()
        val paramsMap = collectDeviceInfo(context)
        for ((key, value) in paramsMap) {
            sb.append(key).append("=").append(value).append("\n")
        }
        val writer = StringWriter()
        val printWriter = PrintWriter(writer)
        ex.printStackTrace(printWriter)
        var cause: Throwable? = ex.cause
        while (cause != null) {
            cause.printStackTrace(printWriter)
            cause = cause.cause
        }
        printWriter.close()
        val result = writer.toString()
        sb.append(result)
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "crash_$timestamp.log"
            val logDir = LogRecordService.getLogDir(context)
            val file = File(logDir, fileName)
            file.writeText(sb.toString())
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

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: CrashHandler? = null
        fun init(context: Context) {
            if (instance == null) {
                instance = CrashHandler(context)
                Thread.setDefaultUncaughtExceptionHandler(instance)
            }
        }
    }
}
