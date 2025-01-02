package com.github.yumelira.yumebox.service.common.log

import android.util.Log as AndroidLog

object Log {
    private const val TAG = "YumeBox"

    fun d(message: String, throwable: Throwable? = null) = AndroidLog.d(TAG, message, throwable)
    fun i(message: String, throwable: Throwable? = null) = AndroidLog.i(TAG, message, throwable)
    fun w(message: String, throwable: Throwable? = null) = AndroidLog.w(TAG, message, throwable)
    fun e(message: String, throwable: Throwable? = null) = AndroidLog.e(TAG, message, throwable)
}
