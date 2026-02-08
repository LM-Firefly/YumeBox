package com.github.yumelira.yumebox.service.common.log

// Re-export from common Log
import com.github.yumelira.yumebox.service.common.Log as CommonLog

object Log {
    fun d(message: String) = CommonLog.d(message)
    fun i(message: String) = CommonLog.i(message)
    fun w(message: String, throwable: Throwable? = null) = CommonLog.w(message, throwable)
    fun e(message: String, throwable: Throwable? = null) = CommonLog.e(message, throwable)
}
