/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c)  YumeLira 2025 - Present
 *
 */



package com.github.yumelira.yumebox.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.yumelira.yumebox.common.util.formatBytes
import com.github.yumelira.yumebox.common.util.formatSpeed
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.runtime.service.R
import com.github.yumelira.yumebox.service.common.constants.Components
import com.github.yumelira.yumebox.service.common.util.appContextOrSelf
import com.github.yumelira.yumebox.service.root.RootTunServiceBridge
import com.github.yumelira.yumebox.service.root.RootTunState
import com.github.yumelira.yumebox.service.root.RootTunStateStore
import com.github.yumelira.yumebox.service.root.RootTunStatus
import com.github.yumelira.yumebox.service.runtime.util.sendClashStarted
import com.github.yumelira.yumebox.service.runtime.util.sendClashStopped
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.milliseconds

class RootTunService : BaseService() {
    private val stateStore by lazy { RootTunStateStore(appContextOrSelf) }
    private val notificationManager by lazy { NotificationManagerCompat.from(this) }
    private var notificationJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                launch {
                    runCatching { RootTunServiceBridge.stop(appContextOrSelf) }
                }
                return START_NOT_STICKY
            }

            ACTION_START, null -> {
                val cachedStatus = stateStore.snapshot()
                if (!cachedStatus.state.isActive && !cachedStatus.state.isRecovering) {
                    stopSelf()
                    return START_NOT_STICKY
                }

                startForeground(
                    NOTIFICATION_ID,
                    buildNotification(
                        cachedStatus.profileName ?: MLang.Service.Notification.UnknownProfile,
                        describeStatus(cachedStatus),
                    ),
                )

                if (notificationJob?.isActive != true) {
                    notificationJob = launch(Dispatchers.Default) {
                        var startedBroadcastSent = false
                        var unreachableCount = 0
                        var lastStatus = cachedStatus

                        while (isActive) {
                            val snapshotResult = runCatching {
                                RootTunServiceBridge.queryStatus(appContextOrSelf)
                            }
                            val snapshot = snapshotResult.getOrNull()
                            if (snapshot == null) {
                                unreachableCount++
                                val error = snapshotResult.exceptionOrNull()
                                val fallbackStatus = stateStore.snapshot().takeIf {
                                    it.state != RootTunState.Idle || !it.profileName.isNullOrBlank() || !it.lastError.isNullOrBlank()
                                } ?: lastStatus
                                val title = fallbackStatus.profileName ?: MLang.Service.Notification.UnknownProfile
                                val content = if (unreachableCount >= 3) {
                                    describeStatus(
                                        fallbackStatus.copy(
                                            lastError = fallbackStatus.lastError ?: error?.message ?: "State unavailable",
                                        ),
                                    )
                                } else {
                                    error?.message ?: "Waiting for reconnect"
                                }
                                notificationManager.notify(NOTIFICATION_ID, buildNotification(title, content))
                                if (!fallbackStatus.state.isActive && !fallbackStatus.state.isRecovering) {
                                    stopSelf()
                                    break
                                }
                                delay(1000L.milliseconds)
                                continue
                            }

                            unreachableCount = 0
                            lastStatus = snapshot
                            syncStatus(snapshot)

                            if (snapshot.state == RootTunState.Running && !startedBroadcastSent) {
                                sendClashStarted()
                                startedBroadcastSent = true
                            }

                            if (snapshot.state == RootTunState.Idle || snapshot.state == RootTunState.Failed) {
                                notificationManager.notify(
                                    NOTIFICATION_ID,
                                    buildNotification(
                                        snapshot.profileName ?: MLang.Service.Notification.UnknownProfile,
                                        describeStatus(snapshot),
                                    ),
                                )
                                stopSelf()
                                break
                            }

                            val profileName = snapshot.profileName ?: MLang.Service.Notification.UnknownProfile
                            val content = if (snapshot.state == RootTunState.Running) {
                                buildTrafficContent()
                            } else {
                                describeStatus(snapshot)
                            }
                            notificationManager.notify(NOTIFICATION_ID, buildNotification(profileName, content))
                            delay(1000L.milliseconds)
                        }
                    }
                }

                return START_STICKY
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        notificationJob?.cancel()
        notificationJob = null

        val snapshot = stateStore.snapshot()
        if (!snapshot.state.isActive) {
            StatusProvider.markRuntimeStopped(ProxyMode.RootTun)
            sendClashStopped(snapshot.lastError)
        }

        super.onDestroy()
    }

    private suspend fun buildTrafficContent(): String {
        val now = runCatching { RootTunServiceBridge.queryTrafficNow(appContextOrSelf) }.getOrDefault(0L)
        val total = runCatching { RootTunServiceBridge.queryTrafficTotal(appContextOrSelf) }.getOrDefault(0L)

        val upNow = decodeTrafficHalf(now ushr 32)
        val downNow = decodeTrafficHalf(now and 0xFFFFFFFFL)
        val upTotal = decodeTrafficHalf(total ushr 32)
        val downTotal = decodeTrafficHalf(total and 0xFFFFFFFFL)

        val speedStr = "↓ ${formatSpeed(downNow)} ↑ ${formatSpeed(upNow)}"
        val totalStr = MLang.Service.Notification.TrafficFormat.format(formatBytes(upTotal + downTotal))
        return "$speedStr | $totalStr"
    }

    private fun buildNotification(title: CharSequence, content: CharSequence): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent().apply {
                component = Components.PROXY_SHEET_ACTIVITY
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, RootTunService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_logo_service)
            .setColor(getColor(R.color.color_clash))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, MLang.Service.Tile.ClickToStopProxy, stopIntent)
            .build()
    }

    private fun createChannel() {
        notificationManager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(CHANNEL_NAME)
                .build(),
        )
    }

    private fun decodeTrafficHalf(encoded: Long): Long {
        val type = (encoded ushr 30) and 0x3L
        val data = encoded and 0x3FFFFFFFL
        return when (type.toInt()) {
            0 -> data
            1 -> (data * 1024L) / 100L
            2 -> (data * 1024L * 1024L) / 100L
            3 -> (data * 1024L * 1024L * 1024L) / 100L
            else -> 0L
        }
    }

    private fun syncStatus(status: RootTunStatus) {
        if (status.state.isActive) {
            StatusProvider.markRuntimeStarted(ProxyMode.RootTun)
        } else {
            StatusProvider.markRuntimeStopped(ProxyMode.RootTun)
        }
    }

    private fun describeStatus(status: RootTunStatus): String {
        return when (status.state) {
            RootTunState.Starting -> "Starting..."
            RootTunState.Running -> MLang.Service.Notification.Running
            RootTunState.Stopping -> "Stopping..."
            RootTunState.Failed -> "Failed: ${status.lastError ?: "unknown error"}"
            RootTunState.Idle -> "Stopped"
        }
    }

    companion object {
        private const val ACTION_START = "com.github.yumelira.yumebox.ROOT_TUN_SERVICE_START"
        private const val ACTION_STOP = "com.github.yumelira.yumebox.ROOT_TUN_SERVICE_STOP"
        private const val NOTIFICATION_ID = 1003
        private const val CHANNEL_ID = "clash_root_tun_service"
        private const val CHANNEL_NAME = "Clash RootTun Service"

        fun start(context: Context) {
            val intent = Intent(context, RootTunService::class.java).setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RootTunService::class.java))
        }
    }
}
