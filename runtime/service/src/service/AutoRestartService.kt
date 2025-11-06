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
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import com.github.yumelira.yumebox.data.store.MMKVProvider
import com.github.yumelira.yumebox.data.store.NetworkSettingsStorage
import com.github.yumelira.yumebox.runtime.service.R
import com.github.yumelira.yumebox.service.root.RootTunServiceBridge
import com.github.yumelira.yumebox.service.runtime.entity.Profile
import com.github.yumelira.yumebox.service.runtime.session.RuntimeServiceLauncher
import kotlinx.coroutines.*
import timber.log.Timber

class AutoRestartService : Service() {

    companion object {
        private const val TAG = "AutoRestartService"
        private const val NOTIFICATION_ID = 1101
        private const val CHANNEL_ID = "auto_restart_channel"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mmkvProvider by lazy { MMKVProvider() }
    private val appSettingsStorage by lazy { AppSettingsStorage(mmkvProvider.getMMKV("settings")) }
    private val networkSettingsStorage by lazy { NetworkSettingsStorage(mmkvProvider.getMMKV("network_settings")) }
    private val profileManager by lazy { ProfileManager(applicationContext) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
            val notification = createNotification()
            val foregroundFlags = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                else -> 0
            }
            startForeground(NOTIFICATION_ID, notification, foregroundFlags)
        }

        serviceScope.launch {
            runCatching {
                checkAndAutoStart()
            }.onFailure { e ->
                Timber.tag(TAG).e(e, "Auto start failed: ${e.message}")
            }
            ServiceCompat.stopForeground(this@AutoRestartService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private suspend fun checkAndAutoStart() {
        if (!appSettingsStorage.automaticRestart.value) return

        val activeProfile = profileManager.queryActive()
        if (activeProfile == null) {
            Timber.tag(TAG).w("No active profile for auto start")
            return
        }

        tryUpdateActiveProfileOnStart(activeProfile)

        when (networkSettingsStorage.proxyMode.value) {
            ProxyMode.Tun -> {
                RuntimeServiceLauncher.start(this, ProxyMode.Tun, RuntimeServiceLauncher.SOURCE_AUTO_RESTART)
            }
            ProxyMode.RootTun -> {
                val result = RootTunServiceBridge.start(this)
                if (!result.success) {
                    error(result.error ?: "RootTun auto start failed")
                }
            }
            ProxyMode.Http -> {
                RuntimeServiceLauncher.start(this, ProxyMode.Http, RuntimeServiceLauncher.SOURCE_AUTO_RESTART)
            }
        }

        Timber.tag(TAG).i("Auto start triggered: profile=${activeProfile.name}, mode=${networkSettingsStorage.proxyMode.value}")
    }

    private suspend fun tryUpdateActiveProfileOnStart(activeProfile: Profile) {
        if (!appSettingsStorage.autoUpdateCurrentProfileOnStart.value) {
            return
        }

        if (activeProfile.type != Profile.Type.Url) {
            Timber.tag(TAG).d("Skip boot update: unsupported profile type=${activeProfile.type}")
            return
        }

        try {
            profileManager.update(activeProfile.uuid, null)
            Timber.tag(TAG).i("Boot update ok: ${activeProfile.uuid}")
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Boot update failed")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Auto Restart Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Used to restart proxy service automatically"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("YumeBox")
            .setContentText("Checking auto-start...")
            .setSmallIcon(R.drawable.ic_logo_service)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
