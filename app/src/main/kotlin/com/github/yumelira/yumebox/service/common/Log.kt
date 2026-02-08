package com.github.yumelira.yumebox.service.common

import android.util.Log as AndroidLog

/**
 * Simple logging wrapper for YumeBox
 */
object Log {
    private const val TAG = "YumeBox"

    fun i(message: String, throwable: Throwable? = null) =
        AndroidLog.i(TAG, message, throwable)

    fun w(message: String, throwable: Throwable? = null) =
        AndroidLog.w(TAG, message, throwable)

    fun e(message: String, throwable: Throwable? = null) =
        AndroidLog.e(TAG, message, throwable)

    fun d(message: String, throwable: Throwable? = null) =
        AndroidLog.d(TAG, message, throwable)

    fun v(message: String, throwable: Throwable? = null) =
        AndroidLog.v(TAG, message, throwable)

    fun wtf(message: String, throwable: Throwable? = null) =
        AndroidLog.wtf(TAG, message, throwable)
}
