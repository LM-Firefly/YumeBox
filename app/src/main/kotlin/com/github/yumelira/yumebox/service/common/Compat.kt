package com.github.yumelira.yumebox.service.common

import android.app.ActivityThread
import android.app.Application
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

/**
 * Android compat extensions for YumeBox
 */

/**
 * Get current process name with compat for older Android versions
 */
val Application.currentProcessName: String
    get() {
        if (Build.VERSION.SDK_INT >= 28)
            return Application.getProcessName()

        return try {
            ActivityThread.currentProcessName()
        } catch (throwable: Throwable) {
            Log.w("Resolve process name: $throwable")
            packageName
        }
    }

/**
 * Get foreground drawable from adaptive icon
 */
fun Drawable.foreground(): Drawable {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        this is AdaptiveIconDrawable && this.background == null
    ) {
        return this.foreground
    }
    return this
}

/**
 * Get appropriate PendingIntent flags based on Android version
 */
val pendingIntentFlags: Int
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    } else {
        PendingIntent.FLAG_UPDATE_CURRENT
    }

/**
 * Start foreground service with compat
 */
fun Service.startForegroundCompat(id: Int, notification: NotificationCompat.Builder) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        startForeground(id, notification.build())
    } else {
        startForeground(id, notification.build())
    }
}

/**
 * Register broadcast receiver with compat for Android 13+
 */
fun Context.registerReceiverCompat(
    receiver: BroadcastReceiver,
    filter: IntentFilter,
    exported: Boolean = false
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val flags = if (exported) {
            Context.RECEIVER_EXPORTED
        } else {
            Context.RECEIVER_NOT_EXPORTED
        }
        registerReceiver(receiver, filter, flags)
    } else {
        registerReceiver(receiver, filter)
    }
}
