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
 * Copyright (c)  YumeLira 2025.
 *
 */

package com.github.yumelira.yumebox.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import com.github.yumelira.yumebox.data.store.ProfilesStore
import com.github.yumelira.yumebox.service.delegate.ClashServiceDelegate
import com.github.yumelira.yumebox.service.notification.ServiceNotificationManager
import timber.log.Timber

class ClashHttpService : Service() {

    companion object {
        private const val TAG = "ClashHttpService"
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        private const val EXTRA_PROFILE_ID = "profile_id"

        fun start(context: Context, profileId: String) {
            val intent = Intent(context, ClashHttpService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PROFILE_ID, profileId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(Intent(context, ClashHttpService::class.java).apply {
                action = ACTION_STOP
            })
        }
    }

    private val clashManager: ClashManager by inject()
    private val profilesStore: ProfilesStore by inject()
    private val appSettingsStorage: AppSettingsStorage by inject()

    private val delegate by lazy {
        ClashServiceDelegate(
            this, clashManager, profilesStore, appSettingsStorage,
            ServiceNotificationManager.HTTP_CONFIG
        )
    }

    override fun onCreate() {
        super.onCreate()
        delegate.initialize()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            ServiceNotificationManager.HTTP_CONFIG.notificationId,
            delegate.notificationManager.create("正在连接...", "正在启动代理", false)
        )

        when (intent?.action) {
            ACTION_START -> {
                val profileId = intent.getStringExtra(EXTRA_PROFILE_ID)
                if (profileId != null) {
                    startHttpProxy(profileId)
                } else {
                    Timber.tag(TAG).e("未提供配置文件 ID")
                    stopSelf()
                }
            }
            ACTION_STOP -> stopHttpProxy()
            else -> stopSelf()
        }

        return START_STICKY
    }

    private fun startHttpProxy(profileId: String) {
        delegate.serviceScope.launch {
            try {
                val startTime = System.currentTimeMillis()

                val profile = delegate.loadProfileIfNeeded(
                    profileId, willUseTunMode = false, quickStart = true
                ).getOrElse { error ->
                    Timber.tag(TAG).e("配置加载失败: ${error.message}")
                    delegate.showErrorNotification("启动失败", error.message ?: "配置加载失败")
                    return@launch
                }
                
                val loadTime = System.currentTimeMillis() - startTime
                Timber.tag(TAG).d("配置加载完成: ${loadTime}ms")

                val address = clashManager.startHttpMode().getOrNull() ?: run {
                    Timber.tag(TAG).e("HTTP 代理启动失败")
                    delegate.showErrorNotification("启动失败", "无法启动 HTTP 代理")
                    return@launch
                }

                val totalTime = System.currentTimeMillis() - startTime
                Timber.tag(TAG).d("HTTP 代理启动完成: ${totalTime}ms, 地址: $address")
                
                delegate.startNotificationUpdate()
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "HTTP 代理启动失败")
                delegate.showErrorNotification("启动失败", e.message ?: "未知错误")
            }
        }
    }

    private fun stopHttpProxy() {
        delegate.stopNotificationUpdate()
        clashManager.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopHttpProxy()
        delegate.cleanup()
        super.onDestroy()
    }
}
