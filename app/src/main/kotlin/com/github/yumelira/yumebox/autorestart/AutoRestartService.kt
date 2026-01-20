package com.github.yumelira.yumebox.autorestart

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.github.yumelira.yumebox.clash.manager.ClashManager
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
        private const val ENQUEUE_TIMEOUT_MS = 20_000L
        const val ACTION_START = "START"
        fun start(context: Context) {
            val intent = Intent(context, AutoRestartService::class.java).apply { 
                action = ACTION_START 
            }
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
    private var autoRestartJob: Job? = null
    override fun onCreate() {
        super.onCreate()
        notificationManager.createChannel()
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            1003,
            notificationManager.create(
                MLang.Service.Status.StartingProxy, 
                MLang.Service.Status.StartingProxy, 
                false
            )
        )
        when (intent?.action) {
            ACTION_START -> {
                if (autoRestartJob?.isActive == true) {
                    Timber.tag(TAG).d("自动重启任务已在执行，忽略重复启动请求")
                } else {
                    startAutoRestartTask()
                }
            }
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }
    private fun startAutoRestartTask() {
        serviceScope?.cancel()
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        autoRestartJob = serviceScope?.launch {
            var lastError: Throwable? = null
            var startSuccess = false
            try {
                val autoStartResult = withTimeoutOrNull(ENQUEUE_TIMEOUT_MS) {
                    AutoRestartCoordinator.enqueue(
                        source = AutoRestartCoordinator.TriggerSource.BootBroadcast,
                        proxyConnectionService = proxyConnectionService,
                        appSettingsStorage = appSettingsStorage,
                        networkSettingsStorage = networkSettingsStorage,
                        profilesStore = profilesStore,
                        clashManager = clashManager
                    )
                }
                when {
                    autoStartResult == null -> {
                        Timber.tag(TAG).w(
                            "等待自动启动结果超时（%dms），降级为直接启动",
                            ENQUEUE_TIMEOUT_MS
                        )
                        val fallbackResult = tryFallbackStartDirect()
                        if (fallbackResult.isSuccess) {
                            Timber.tag(TAG).d("降级直启成功")
                            startSuccess = true
                            delay(2000L)
                        } else {
                            lastError = fallbackResult.exceptionOrNull()
                                ?: IllegalStateException("Fallback start failed")
                        }
                    }
                    else -> when (autoStartResult) {
                        is AutoRestartCoordinator.AutoStartResult.Started -> {
                            Timber.tag(TAG).d("自动启动代理成功，等待代理完全启动...")
                            startSuccess = true
                            delay(2000L)
                        }
                        is AutoRestartCoordinator.AutoStartResult.Skipped -> {
                            Timber.tag(TAG).d(
                                "自动启动被跳过: reason=%s, detail=%s, cooldownSkipped=%d, stateBusySkipped=%d",
                                autoStartResult.reason,
                                autoStartResult.detail ?: "",
                                autoStartResult.stats.cooldownSkippedCount,
                                autoStartResult.stats.stateBusySkippedCount
                            )
                            startSuccess = true
                        }
                        is AutoRestartCoordinator.AutoStartResult.Failed -> {
                            lastError = autoStartResult.error
                        }
                    }
                }
            } catch (e: Exception) {
                lastError = e
            }
            if (!startSuccess && lastError != null) {
                Timber.tag(TAG).e(
                    lastError, 
                    MLang.Service.Message.AutoStartFailed.format(lastError.message ?: "")
                )
                startForeground(
                    1003, 
                    notificationManager.create("启动失败", lastError.message ?: "启动失败", false)
                )
                delay(3000)
            }
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
                Timber.tag(TAG).d("已移除前台通知")
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Failed to stop foreground")
            }
            autoRestartJob = null
            stopSelf()
        }
    }
    private suspend fun tryFallbackStartDirect(): Result<Unit> {
        val profileId = resolveProfileId()
            ?: return Result.failure(IllegalStateException("没有可用的配置文件"))
        val mode = networkSettingsStorage.proxyMode.value
        return proxyConnectionService.startDirect(profileId = profileId, mode = mode)
    }
    private fun resolveProfileId(): String? {
        val profiles = profilesStore.getAllProfiles()
        val lastUsedId = profilesStore.lastUsedProfileId
        if (lastUsedId.isNotEmpty() && profiles.any { it.id == lastUsedId }) {
            return lastUsedId
        }
        return profiles.firstOrNull { it.enabled }?.id ?: profiles.firstOrNull()?.id
    }
    override fun onDestroy() {
        autoRestartJob?.cancel()
        autoRestartJob = null
        serviceScope?.cancel()
        super.onDestroy()
    }
}
