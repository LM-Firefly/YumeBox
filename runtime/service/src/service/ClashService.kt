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



package com.github.yumelira.yumebox.runtime.service

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.core.model.ProxyMode
import com.github.yumelira.yumebox.runtime.api.service.common.constants.Intents
import com.github.yumelira.yumebox.runtime.service.common.log.Log
import com.github.yumelira.yumebox.runtime.service.common.util.CoreRuntimeConfig
import com.github.yumelira.yumebox.runtime.api.service.common.util.appContextOrSelf
import com.github.yumelira.yumebox.runtime.service.notification.ServiceNotificationManager
import com.github.yumelira.yumebox.runtime.service.runtime.session.LocalHttpTransport
import com.github.yumelira.yumebox.runtime.service.runtime.session.RuntimeHost
import com.github.yumelira.yumebox.runtime.service.runtime.session.RuntimeSpec
import com.github.yumelira.yumebox.runtime.service.runtime.session.RuntimeStartupLogStore
import com.github.yumelira.yumebox.runtime.service.runtime.session.SessionRuntime
import com.github.yumelira.yumebox.runtime.service.runtime.session.SessionRuntimeSpecFactory
import com.github.yumelira.yumebox.runtime.api.service.runtime.entity.RuntimeSnapshot
import com.github.yumelira.yumebox.runtime.service.runtime.util.sendClashStarted
import com.github.yumelira.yumebox.runtime.service.runtime.util.sendClashStopped
import com.github.yumelira.yumebox.runtime.service.runtime.util.sendProfileLoaded
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

class ClashService : BaseService() {
    private var reason: String? = null
    private val powerController by lazy { ServicePowerController(this) }
    private val notificationManager by lazy {
        ServiceNotificationManager(this, ServiceNotificationManager.HTTP_CONFIG)
    }
    private val startupLogStore by lazy {
        RuntimeStartupLogStore(this, RuntimeStartupLogStore.Scope.LOCAL_HTTP)
    }
    private var notificationJob: Job? = null
    private lateinit var runtime: SessionRuntime
    private var reloadJob: Job? = null

    private val runtimeEventsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action ?: return) {
                Intents.ACTION_PROFILE_CHANGED,
                Intents.ACTION_OVERRIDE_CHANGED -> scheduleReload()

                Intents.ACTION_CLASH_REQUEST_STOP -> {
                    reason = intent.getStringExtra(Intents.EXTRA_STOP_REASON)
                    reloadJob?.cancel()
                    reloadJob = null
                    StatusProvider.markRuntimeStopping(ProxyMode.Http)
                    if (this@ClashService::runtime.isInitialized) {
                        runtime.requestStop(reason)
                    }
                    stopSelf()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        powerController.start()
        runCatching {
            startupLogStore.append("LOCAL_HTTP service: onCreate begin")

            notificationManager.createChannel()
            startForeground(
                ServiceNotificationManager.HTTP_CONFIG.notificationId,
                notificationManager.createInitialNotification(),
            )
            startupLogStore.append("LOCAL_HTTP service: startForeground done")

            StatusProvider.clearLegacyStateFiles()
            StatusProvider.markRuntimeStarting(ProxyMode.Http)
            CoreRuntimeConfig.applyCustomUserAgentIfPresent(this)

            runtime = SessionRuntime(
                screenOn = powerController.screenOn,
                host = object : RuntimeHost {
                    override val context = this@ClashService
                    override val mode: ProxyMode = ProxyMode.Http

                    override fun onStarting(spec: RuntimeSpec) = Unit

                    override fun onStarted(spec: RuntimeSpec) {
                        StatusProvider.markRuntimeRunning(ProxyMode.Http)
                        sendClashStarted()
                    }

                    override fun onStopped(reason: String?) {
                        this@ClashService.reason = reason
                        StatusProvider.markRuntimeIdle(ProxyMode.Http)
                        sendClashStopped(reason)
                    }

                    override fun onProfileLoaded(profileUuid: String) {
                        sendProfileLoaded(UUID.fromString(profileUuid))
                    }

                    override fun onSnapshotChanged(snapshot: RuntimeSnapshot) = Unit

                    override fun onLogReady(ready: Boolean) = Unit

                    override fun onLogItem(log: LogMessage) = Unit

                    override fun reportFailure(error: String) {
                        reason = error
                        startupLogStore.append("LOCAL_HTTP failed=$error")
                        StatusProvider.markRuntimeFailed(ProxyMode.Http)
                        sendClashStopped(error)
                        Log.e("HTTP runtime failed: $error")
                        stopSelf()
                    }
                },
                transport = LocalHttpTransport(this),
                scope = this,
            )

            registerRuntimeReceiver()
            startupLogStore.append("LOCAL_HTTP service: receiver registered")
            launch {
                runCatching {
                    startupLogStore.append("LOCAL_HTTP spec: create begin")
                    val spec = SessionRuntimeSpecFactory(appContextOrSelf).createHttpSpec()
                    startupLogStore.append("LOCAL_HTTP spec: create done profile=${spec.profileUuid} overrides=${spec.overrideSpecs.size}")
                    val result = runtime.start(spec)
                    check(result.success) { result.error ?: "http runtime start failed" }
                }.onFailure { error ->
                    reason = error.message ?: "http runtime start failed"
                    startupLogStore.append("LOCAL_HTTP failed=$reason")
                    StatusProvider.markRuntimeFailed(ProxyMode.Http)
                    sendClashStopped(reason)
                    stopSelf()
                }
            }
        }.onFailure { error ->
            reason = error.message ?: "http runtime start failed"
            startupLogStore.append("LOCAL_HTTP failed=$reason")
            StatusProvider.markRuntimeFailed(ProxyMode.Http)
            sendClashStopped(reason)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (notificationJob?.isActive != true) {
            notificationJob = notificationManager.startTrafficUpdate(this, powerController.screenOn)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return Binder()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(runtimeEventsReceiver) }
        reloadJob?.cancel()
        reloadJob = null
        notificationJob?.cancel()
        notificationJob = null
        notificationManager.resetSpeedSmoothing()

        if (this::runtime.isInitialized) {
            runtime.requestStop(reason)
            runtime.destroy()
        }

        StatusProvider.markRuntimeIdle(ProxyMode.Http)
        sendClashStopped(reason)
        startupLogStore.append("LOCAL_HTTP destroy")
        Log.i("ClashService destroyed: ${reason ?: "successfully"}")

        powerController.stop()
        super.onDestroy()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        com.github.yumelira.yumebox.core.Clash.forceGc()
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerRuntimeReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intents.ACTION_PROFILE_CHANGED)
            addAction(Intents.ACTION_OVERRIDE_CHANGED)
            addAction(Intents.ACTION_CLASH_REQUEST_STOP)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(runtimeEventsReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(runtimeEventsReceiver, filter)
        }
    }

    private fun scheduleReload() {
        reloadJob?.cancel()
        reloadJob = launch {
            startupLogStore.append("LOCAL_HTTP spec: reload create begin")
            val spec = runCatching {
                SessionRuntimeSpecFactory(appContextOrSelf).createHttpSpec()
            }.getOrElse { error ->
                reason = error.message
                startupLogStore.append("LOCAL_HTTP failed=${error.message ?: "http runtime spec refresh failed"}")
                Log.w("HTTP runtime spec refresh failed: ${error.message}")
                return@launch
            }
            startupLogStore.append("LOCAL_HTTP spec: reload create done profile=${spec.profileUuid} overrides=${spec.overrideSpecs.size}")

            val result = runtime.reload(spec)
            if (!result.success) {
                reason = result.error
                startupLogStore.append("LOCAL_HTTP failed=${result.error ?: "http runtime reload failed"}")
                Log.w("HTTP runtime reload failed: ${result.error}")
            }
        }
    }
}
