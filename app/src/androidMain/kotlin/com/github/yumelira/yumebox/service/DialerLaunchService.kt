package com.github.yumelira.yumebox.service

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.IBinder
import androidx.core.app.ServiceCompat

class DialerLaunchService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val mainActivityName = ComponentName(
                this@DialerLaunchService,
                "com.github.yumelira.yumebox.MainActivity",
            )

            val launchIntent = Intent(Intent.ACTION_MAIN).apply {
                setComponent(mainActivityName)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED,
                )
            }

            startActivity(launchIntent)

        } catch (_: Exception) {
            try {
                val fallbackIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (fallbackIntent != null) {
                    fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(fallbackIntent)
                }
            } catch (_: Exception) {
            }
        }

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()

        return START_NOT_STICKY
    }
}

