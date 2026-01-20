package com.github.yumelira.yumebox.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.common.util.ProxyAutoStartHelper
import com.github.yumelira.yumebox.data.repository.ProxyConnectionService
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import com.github.yumelira.yumebox.data.store.NetworkSettingsStorage
import com.github.yumelira.yumebox.data.store.ProfilesStorage
import com.github.yumelira.yumebox.service.notification.ServiceNotificationManager
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import timber.log.Timber

class AutoRestartService : Service() {

    companion object {
        private const val TAG = "AutoRestartService"
        const val ACTION_START = "START"
        fun start(context: Context) {
            val intent = Intent(context, AutoRestartService::class.java).apply { action = ACTION_START }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private val appSettingsStorage: AppSettingsStorage by inject()
    private val networkSettingsStorage: NetworkSettingsStorage by inject()
    private val profilesStore: ProfilesStorage by inject()
    private val clashManager: ClashManager by inject()
    private val proxyConnectionService: ProxyConnectionService by inject()

    private val notificationManager by lazy {
        ServiceNotificationManager(
            this,
            ServiceNotificationManager.Config(
                notificationId = 1003,
                channelId = "auto_restart_service",
                channelName = "Auto Restart Service",
                stopAction = "STOP"
            )
        )
    }
    private var serviceScope: CoroutineScope? = null
    override fun onCreate() {
        super.onCreate()
        notificationManager.createChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            1003,
            notificationManager.create(MLang.Service.Status.StartingProxy, MLang.Service.Status.StartingProxy, false)
        )
        when (intent?.action) {
            ACTION_START -> startAutoRestartTask()
            else -> stopSelf()
        }

        return START_NOT_STICKY
    }
    private fun startAutoRestartTask() {
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        serviceScope?.launch {
            var retryCount = 0
            val maxRetries = 3
            var lastError: Exception? = null
            while (retryCount < maxRetries) {
                try {
                    Timber.tag(TAG).d("尝试自动启动代理（第 ${retryCount + 1}/$maxRetries 次）")
                    ProxyAutoStartHelper.checkAndAutoStart(
                        proxyConnectionService = proxyConnectionService,
                        appSettingsStorage = appSettingsStorage,
                        networkSettingsStorage = networkSettingsStorage,
                        profilesStore = profilesStore,
                        clashManager = clashManager,
                        isBootCompleted = true
                    )
                    // 启动成功，退出循环
                    Timber.tag(TAG).d("自动启动代理成功")
                    break
                } catch (e: Exception) {
                    lastError = e
                    Timber.tag(TAG).w(e, "自动启动代理失败，准备重试（第 ${retryCount + 1}/$maxRetries 次）")
                    retryCount++
                    // 不是最后一次重试时，等待后重试
                    if (retryCount < maxRetries) {
                        delay(1000L * retryCount) // 延迟重试，1秒、2秒、3秒
                    }
                }
            }
            if (lastError != null && retryCount >= maxRetries) {
                Timber.tag(TAG).e(lastError, MLang.Service.Message.AutoStartFailed.format(lastError.message ?: ""))
                // 显示错误通知（仅在所有重试都失败时）
                startForeground(1003, notificationManager.create("启动失败", lastError.message ?: "启动失败", false))
                delay(3000)
            }
            stopSelf()
        }
    }

    override fun onDestroy() {
        serviceScope?.cancel()
        super.onDestroy()
    }
}
