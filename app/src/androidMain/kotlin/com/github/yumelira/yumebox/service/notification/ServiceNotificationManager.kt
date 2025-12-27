package com.github.yumelira.yumebox.service.notification

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.common.util.formatBytes
import com.github.yumelira.yumebox.common.util.formatSpeed
import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import com.github.yumelira.yumebox.domain.model.TrafficData
import com.github.yumelira.yumebox.MainActivity
import com.github.yumelira.yumebox.R
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ServiceNotificationManager(
    private val service: Service,
    private val config: Config
) {
    private data class NotificationData(
        val now: TrafficData,
        val total: TrafficData,
        val currentProfile: Profile?,
        val showTraffic: Boolean
    )

    data class Config(
        val notificationId: Int,
        val channelId: String,
        val channelName: String,
        val stopAction: String
    )

    private val notificationManager: NotificationManager by lazy {
        service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                config.channelId,
                config.channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = MLang.Service.Notification.ChannelDescription
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun create(
        title: String,
        content: String,
        isConnected: Boolean,
        smallIconRes: Int = R.drawable.ic_logo_service
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            service, 0,
            Intent(service, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPendingIntent = PendingIntent.getService(
            service, 1,
            Intent(service, service.javaClass).apply { action = config.stopAction },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(service, config.channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(smallIconRes)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(isConnected)
            .apply {
                if (isConnected) {
                    addAction(android.R.drawable.ic_menu_close_clear_cancel, MLang.Service.Notification.ActionDisconnect, stopPendingIntent)
                }
            }
            .build()
    }

    fun update(title: String, content: String, isConnected: Boolean) {
        notificationManager.notify(config.notificationId, create(title, content, isConnected))
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun startTrafficUpdate(
        scope: CoroutineScope,
        clashManager: ClashManager,
        appSettings: AppSettingsStorage
    ): Job = scope.launch {
        appSettings.showTrafficNotification.state.flatMapLatest { showTraffic ->
            if (showTraffic) {
                val trafficFlow = clashManager.createTrafficFlow()
                combine(
                    trafficFlow,
                    clashManager.currentProfile
                ) { (now, total), currentProfile ->
                    NotificationData(now, total, currentProfile, true)
                }
            } else {
                clashManager.currentProfile.map { currentProfile ->
                    NotificationData(TrafficData.ZERO, TrafficData.ZERO, currentProfile, false)
                }
            }
        }.collect { (now, total, currentProfile, showTraffic) ->
            val profileName = currentProfile?.name ?: MLang.ProfilesPage.Message.UnknownProfile
            if (showTraffic) {
                val speedStr = "↓ ${formatSpeed(now.download)} ↑ ${formatSpeed(now.upload)}"
                val totalStr = MLang.Service.Notification.TrafficFormat.format(formatBytes(total.download + total.upload))
                update(MLang.Service.Notification.ConnectedWithProfile.format(profileName), "$speedStr | $totalStr", true)
            } else {
                update(MLang.Service.Notification.Connected, profileName, true)
            }
        }
    }

    private fun Context.screenStateFlow() = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(intent.action == Intent.ACTION_SCREEN_ON)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(receiver, filter)
        }
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        trySend(pm.isInteractive)
        awaitClose { unregisterReceiver(receiver) }
    }

    companion object {
        val VPN_CONFIG = Config(
            notificationId = 1001,
            channelId = "clash_vpn_service",
            channelName = "Clash VPN Service",
            stopAction = "STOP"
        )

        val HTTP_CONFIG = Config(
            notificationId = 1002,
            channelId = "clash_http_service",
            channelName = "Clash HTTP Service",
            stopAction = "STOP"
        )
    }
}
