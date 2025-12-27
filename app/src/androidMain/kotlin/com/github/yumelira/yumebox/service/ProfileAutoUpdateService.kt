package com.github.yumelira.yumebox.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.github.yumelira.yumebox.MainActivity
import com.github.yumelira.yumebox.R
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber

class ProfileAutoUpdateService : Service() {

    companion object {
        private const val TAG = "ProfileAutoUpdateService"
        const val EXTRA_PROFILE_ID = "profile_id"
        private const val CHANNEL_ID = "profile_auto_update_service"
        private const val NOTIF_ID = 2001

        fun start(context: Context, profileId: String) {
            val intent = Intent(context, ProfileAutoUpdateService::class.java).apply {
                putExtra(EXTRA_PROFILE_ID, profileId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private var serviceScope: CoroutineScope? = null
    private var serviceJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val profileId = intent?.getStringExtra(EXTRA_PROFILE_ID)
        if (profileId.isNullOrBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification()
        startForeground(NOTIF_ID, notification)

        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        serviceJob = serviceScope?.launch {
            try {
                val result = com.github.yumelira.yumebox.clash.performUpdate(profileId)
                if (result.isSuccess) {
                    Timber.tag(TAG).d("Profile %s auto-update succeeded", profileId)
                } else {
                    Timber.tag(TAG).e(result.exceptionOrNull(), "Profile %s auto-update failed", profileId)
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Profile %s auto-update exception", profileId)
            } finally {
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceJob?.cancel()
        serviceScope?.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(CHANNEL_ID, "Profile Auto Update", NotificationManager.IMPORTANCE_LOW)
            channel.setShowBadge(false)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pending = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(MLang.Service.Status.Connecting)
            .setContentText(MLang.Service.Status.StartingProxy)
            .setSmallIcon(R.drawable.ic_logo_service)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }
}
