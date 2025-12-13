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

package com.github.yumelira.yumebox.clash.manager

import kotlinx.coroutines.*
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.clash.config.Configuration
import com.github.yumelira.yumebox.common.util.SystemProxyHelper
import com.github.yumelira.yumebox.domain.model.RunningMode
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.os.PowerManager
import androidx.core.content.getSystemService
import com.github.yumelira.yumebox.domain.model.TrafficData
import timber.log.Timber
import java.net.InetSocketAddress

class ServiceManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val stateManager: ProxyStateManager,
    private val proxyGroupManager: ProxyGroupManager
) {
    private var trafficMonitorJob: Job? = null
    private var isProxyScreenActive = false
    private var lastProxyGroupRefreshTime = 0L
    private var isScreenOn = context.getSystemService<PowerManager>()?.isInteractive ?: true
    private var screenReceiver: BroadcastReceiver? = null

    fun setProxyScreenActive(active: Boolean) {
        isProxyScreenActive = active
        if (active) {
            scope.launch {
                proxyGroupManager.refreshProxyGroups(skipCacheClear = true, currentProfile = stateManager.currentProfile.value)
                lastProxyGroupRefreshTime = System.currentTimeMillis()
            }
        }
    }

    suspend fun startTunMode(
        fd: Int,
        config: Configuration.TunConfig = Configuration.TunConfig(),
        markSocket: (Int) -> Boolean,
        querySocketUid: (protocol: Int, source: InetSocketAddress, target: InetSocketAddress) -> Int = { _, _, _ -> -1 }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            stateManager.connecting(RunningMode.Tun)
            Configuration.applyOverride(Configuration.ProxyMode.Tun)
            Clash.startTun(
                fd = fd,
                stack = config.stack,
                gateway = "${config.gateway}/30",
                portal = "${config.portal}/30",
                dns = config.dns,
                markSocket = markSocket,
                querySocketUid = querySocketUid
            )
            val profile = stateManager.currentProfile.value
                ?: throw IllegalStateException("Cannot start TUN mode without loaded profile")
            stateManager.running(profile, RunningMode.Tun)
            startMonitoring()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "TUN 模式启动失败")
            stateManager.error("TUN 模式启动失败: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun startHttpMode(config: Configuration.HttpConfig = Configuration.HttpConfig()): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val profile = stateManager.currentProfile.value
            if (profile == null) {
                Timber.e("HTTP 代理启动失败 - 没有已加载的配置")
                return@withContext Result.failure(IllegalStateException("Cannot start HTTP mode without loaded profile"))
            }
            val httpMode = RunningMode.Http(config.address)
            stateManager.connecting(httpMode)
            Configuration.applyOverride(Configuration.ProxyMode.Http(config.port))
            val address = Clash.startHttp(config.listenAddress) ?: config.address
            stateManager.running(profile, RunningMode.Http(address))
            startMonitoring()
            Result.success(address)
        } catch (e: Exception) {
            Timber.e(e, "HTTP 代理启动失败")
            stateManager.error("HTTP 代理启动失败: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun stop() {
        runCatching {
            stateManager.stopping()
            when (val mode = stateManager.runningMode.value) {
                is RunningMode.Tun -> { Clash.stopTun(); Clash.reset() }
                is RunningMode.Http -> {
                    Clash.stopHttp()
                    Clash.reset()
                    SystemProxyHelper.clearSystemProxy(context)
                }
                RunningMode.None -> {}
            }
            stateManager.reset()
            stopMonitoring()
        }
    }

    private fun startMonitoring() {
        stopMonitoring()
        registerScreenReceiver()
        trafficMonitorJob = scope.launch {
            while (isActive) {
                runCatching {
                    stateManager.updateTrafficNow(TrafficData.from(Clash.queryTrafficNow()))
                    stateManager.updateTrafficTotal(TrafficData.from(Clash.queryTrafficTotal()))
                    stateManager.updateTunnelState(Clash.queryTunnelState())
                    
                    // Only refresh proxy groups when screen is on and proxy screen is active
                    if (isScreenOn && isProxyScreenActive) {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastProxyGroupRefreshTime >= 5000L) {
                            proxyGroupManager.refreshProxyGroups(skipCacheClear = true, currentProfile = stateManager.currentProfile.value)
                            lastProxyGroupRefreshTime = currentTime
                        }
                    }
                }
                
                // Adjust polling interval based on screen state
                val pollInterval = if (isScreenOn) 2000L else 30000L
                delay(pollInterval)
            }
        }
    }
    
    private fun registerScreenReceiver() {
        if (screenReceiver != null) return
        
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        isScreenOn = true
                        Timber.d("Screen ON - increasing polling frequency")
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        isScreenOn = false
                        Timber.d("Screen OFF - reducing polling frequency")
                    }
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        context.registerReceiver(screenReceiver, filter)
    }
    
    private fun unregisterScreenReceiver() {
        screenReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {
                Timber.e(e, "Error unregistering screen receiver")
            }
            screenReceiver = null
        }
    }

    private fun stopMonitoring() {
        trafficMonitorJob?.cancel()
        trafficMonitorJob = null
        unregisterScreenReceiver()
    }
}