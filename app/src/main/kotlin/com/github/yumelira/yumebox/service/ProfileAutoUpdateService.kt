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
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import timber.log.Timber

class ProfileAutoUpdateService : Service() {
    companion object {
        private const val TAG = "ProfileAutoUpdateService"
        const val EXTRA_PROFILE_ID = "profile_id"
        private const val CHANNEL_ID_SERVICE = "profile_auto_update_service"
        private const val CHANNEL_ID_PROGRESS = "profile_update_progress"
        private const val NOTIF_ID_FOREGROUND = 2001
        private const val NOTIF_ID_PROGRESS_BASE = 3000
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
    private var updateJob: Job? = null
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        Timber.tag(TAG).d("ProfileAutoUpdateService created")
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val profileId = intent?.getStringExtra(EXTRA_PROFILE_ID)
        if (profileId.isNullOrBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (updateJob?.isActive == true) {
            Timber.tag(TAG).d("Update already in progress, ignoring new request for %s", profileId)
            return START_NOT_STICKY
        }
        serviceScope?.cancel()
        startForeground(NOTIF_ID_FOREGROUND, buildForegroundNotification())
        serviceScope = CoroutineScope(Dispatchers.IO)
        updateJob = serviceScope?.launch {
            performProfileUpdate(profileId)
            withContext(NonCancellable) {
                Timber.tag(TAG).d("Profile update completed for %s, stopping service", profileId)
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }
    override fun onDestroy() {
        updateJob?.cancel()
        serviceScope?.cancel()
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancelAll()
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to cancel notifications")
        }
        Timber.tag(TAG).d("ProfileAutoUpdateService destroyed")
        super.onDestroy()
    }
    private suspend fun performProfileUpdate(profileId: String) {
        try {
            Timber.tag(TAG).d("Starting update for profile %s", profileId)
            val profileName = try {
                val store = org.koin.core.context.GlobalContext.getOrNull()?.get<com.github.yumelira.yumebox.data.store.ProfilesStore>()
                store?.getAllProfiles()?.firstOrNull { it.id == profileId }?.name ?: profileId
            } catch (e: Exception) {
                profileId
            }
            showProgressNotification("正在更新配置", profileName, profileId)
            val result = com.github.yumelira.yumebox.clash.performUpdate(profileId)
            if (result.isSuccess) {
                Timber.tag(TAG).i("Profile %s auto-update succeeded", profileId)
                showProgressNotification("配置更新成功", profileName, profileId)
                delay(2000L)
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                Timber.tag(TAG).w(error, "Profile %s auto-update failed: %s", profileId, error)
                showProgressNotification("配置更新失败", error, profileId)
                delay(2000L)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            Timber.tag(TAG).d("Profile %s update cancelled", profileId)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Profile %s auto-update exception", profileId)
            showProgressNotification("配置更新失败", e.message ?: "Unknown error", profileId)
            delay(2000L)
        } finally {
            try {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val progressNotifId = NOTIF_ID_PROGRESS_BASE + profileId.hashCode()
                nm.cancel(progressNotifId)
                Timber.tag(TAG).d("Cleared progress notification for %s", profileId)
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Failed to cancel progress notification")
            }
        }
    }
    private fun showProgressNotification(title: String, text: String, profileId: String) {
        try {
            val pending = PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(this, CHANNEL_ID_PROGRESS)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_logo_service)
                .setContentIntent(pending)
                .setOngoing(false)
                .setProgress(0, 0, true)
                .setAutoCancel(true)
                .build()
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val progressNotifId = NOTIF_ID_PROGRESS_BASE + profileId.hashCode()
            nm.notify(progressNotifId, notification)
            Timber.tag(TAG).d("Showed progress notification: %s - %s (ID: %d)", title, text, progressNotifId)
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to show progress notification")
        }
    }
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val serviceChannel = NotificationChannel(
                CHANNEL_ID_SERVICE,
                "配置自动更新服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            nm.createNotificationChannel(serviceChannel)
            val progressChannel = NotificationChannel(
                CHANNEL_ID_PROGRESS,
                "配置更新进度",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setShowBadge(true)
                enableVibration(true)
            }
            nm.createNotificationChannel(progressChannel)
        }
    }
    private fun buildForegroundNotification(): Notification {
        val pending = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID_SERVICE)
            .setContentTitle("配置自动更新")
            .setContentText("正在进行中...")
            .setSmallIcon(R.drawable.ic_logo_service)
            .setContentIntent(pending)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}
