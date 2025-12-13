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

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.github.yumelira.yumebox.clash.cache.GlobalDelayCache
import com.github.yumelira.yumebox.clash.config.ClashConfiguration
import com.github.yumelira.yumebox.clash.testing.ProxyTestManager
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.core.model.ProxySort
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.domain.model.HealthStatus
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.domain.model.ProxyState
import com.github.yumelira.yumebox.domain.model.RunningMode
import com.github.yumelira.yumebox.domain.model.TrafficData
import com.github.yumelira.yumebox.domain.usecase.*
import timber.log.Timber
import java.io.Closeable
import java.io.File
import java.net.InetSocketAddress


class ClashManager(
    private val context: Context,
    private val workDir: File,
    private val proxyModeProvider: (() -> TunnelState.Mode)? = null
) : Closeable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val stateManager = ProxyStateManager(scope)
    private val proxyGroupManager = ProxyGroupManager(context, scope, GlobalDelayCache())
    private val profileManager = ProfileManager(workDir)
    private val serviceManager = ServiceManager(context, scope, stateManager, proxyGroupManager)
    private val proxyTestManager = ProxyTestManager(scope, maxConcurrentTests = 5)

    private val loadProfileUseCase by lazy { LoadProfileUseCase(profileManager, stateManager, proxyGroupManager) }
    private val downloadProfileUseCase by lazy { DownloadProfileUseCase(profileManager) }
    private val reloadProfileUseCase by lazy { ReloadProfileUseCase(profileManager, stateManager) }
    private val startTunModeUseCase by lazy { StartTunModeUseCase(serviceManager) }
    private val startHttpModeUseCase by lazy { StartHttpModeUseCase(serviceManager) }
    private val stopProxyUseCase by lazy { StopProxyUseCase(serviceManager, proxyGroupManager) }
    private val selectProxyUseCase by lazy { SelectProxyUseCase(proxyGroupManager, stateManager) }
    private val refreshProxyGroupsUseCase by lazy { RefreshProxyGroupsUseCase(proxyGroupManager, stateManager) }
    private val testProxyDelayUseCase by lazy { TestProxyDelayUseCase(proxyTestManager) }
    private val healthCheckUseCase by lazy { HealthCheckUseCase(refreshProxyGroupsUseCase) }

    val proxyState: StateFlow<ProxyState> = stateManager.proxyState
    val isRunning: StateFlow<Boolean> = stateManager.isRunning
    val currentProfile: StateFlow<Profile?> = stateManager.currentProfile
    val trafficNow: StateFlow<TrafficData> = stateManager.trafficNow
    val trafficTotal: StateFlow<TrafficData> = stateManager.trafficTotal
    val tunnelState: StateFlow<TunnelState?> = stateManager.tunnelState
    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = proxyGroupManager.proxyGroups
    val runningMode: StateFlow<RunningMode> = stateManager.runningMode

    val testStates: StateFlow<Map<String, ProxyTestManager.TestState>> = proxyTestManager.testStates
    val testResults: SharedFlow<ProxyTestManager.TestResult> = proxyTestManager.testResults
    val queueState: StateFlow<ProxyTestManager.QueueState> = proxyTestManager.queueState

    private val _healthStatus = MutableStateFlow(HealthStatus())
    val healthStatus: StateFlow<HealthStatus> = _healthStatus.asStateFlow()

    private val _logs = MutableSharedFlow<LogMessage>(replay = 100)
    val logs: SharedFlow<LogMessage> = _logs.asSharedFlow()

    init {
        workDir.mkdirs()
        _healthStatus.value = HealthStatus(isHealthy = true, message = "Service ready")
        observeTestResults()
        subscribeToLogs()
    }

    private fun observeTestResults() {
        scope.launch {
            proxyTestManager.testResults.collect { result ->
                runCatching {
                    val group = Clash.queryGroup(
                        result.groupName, 
                        ProxySort.Default
                    )
                    group.proxies.filter { it.delay > 0 }.forEach { p ->
                        proxyGroupManager.getGroupState(result.groupName)?.now = p.name
                    }
                }
                delay(500)
                runCatching { refreshProxyGroupsUseCase(skipCacheClear = true) }
            }
        }
    }

    private fun subscribeToLogs() {
        scope.launch {
            try {
                val logChannel = Clash.subscribeLogcat()
                for (log in logChannel) {
                    if (!log.message.contains("Request interrupted by user") &&
                        !log.message.contains("更新延迟")) {
                        _logs.emit(log)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Log subscription error")
            }
        }
    }

    fun testProxyDelay(
        groupName: String,
        priority: Int = ProxyTestManager.Priority.NORMAL,
        forceTest: Boolean = false
    ) = testProxyDelayUseCase(groupName, priority, forceTest)

    suspend fun testAllProxyDelay(): Result<String> = runCatching {
        val groupNames = Clash.queryGroupNames(excludeNotSelectable = false)
        if (groupNames.isEmpty()) throw Exception("No proxy groups available")
        groupNames.forEach { testProxyDelay(it, priority = ProxyTestManager.Priority.NORMAL) }
        "batch_test_${System.currentTimeMillis()}"
    }

    fun getTestStatistics(): ProxyTestManager.TestStatistics = proxyTestManager.getTestStatistics()

    suspend fun selectProxy(groupName: String, proxyName: String): Boolean = 
        selectProxyUseCase(groupName, proxyName)

    suspend fun forceSelectProxy(groupName: String, proxyName: String): Boolean =
        proxyGroupManager.forceSelectProxy(groupName, proxyName, stateManager.currentProfile.value)

    suspend fun refreshProxyGroups(skipCacheClear: Boolean = false): Result<Unit> = 
        refreshProxyGroupsUseCase(skipCacheClear)

    suspend fun healthCheck(groupName: String): Result<Unit> = healthCheckUseCase(groupName)
    suspend fun healthCheckAll(): Result<Unit> = healthCheckUseCase.checkAll()

    suspend fun reloadCurrentProfile(): Result<Unit> = reloadProfileUseCase()

    suspend fun downloadProfileOnly(
        profile: Profile,
        forceDownload: Boolean = true,
        onProgress: ((String, Int) -> Unit)? = null
    ): Result<String> = downloadProfileUseCase(profile, forceDownload, onProgress)

    suspend fun loadProfile(
        profile: Profile,
        forceDownload: Boolean = false,
        onProgress: ((String, Int) -> Unit)? = null,
        willUseTunMode: Boolean = false,
        quickStart: Boolean = false
    ): Result<String> {
        return loadProfileUseCase(profile, forceDownload, onProgress, willUseTunMode, quickStart)
            .onSuccess {
                scope.launch { 
                    applySavedProxyMode()
                    delay(300)
                    refreshProxyGroups(skipCacheClear = true) 
                }
            }
    }
    
    private fun applySavedProxyMode() {
        val savedMode = proxyModeProvider?.invoke() ?: return
        runCatching {
            val persistOverride = Clash.queryOverride(
                Clash.OverrideSlot.Persist
            )
            if (persistOverride.mode != savedMode) {
                persistOverride.mode = savedMode
                Clash.patchOverride(
                    Clash.OverrideSlot.Persist,
                    persistOverride
                )
            }
            
            val sessionOverride = Clash.queryOverride(
                Clash.OverrideSlot.Session
            )
            sessionOverride.mode = savedMode
            Clash.patchOverride(
                Clash.OverrideSlot.Session,
                sessionOverride
            )
        }
    }

    suspend fun startTunMode(
        fd: Int,
        config: ClashConfiguration.TunConfig =
            ClashConfiguration.TunConfig(),
        markSocket: (Int) -> Boolean,
        querySocketUid: (protocol: Int, source: InetSocketAddress, target: InetSocketAddress) -> Int = { _, _, _ -> -1 }
    ): Result<Unit> = startTunModeUseCase(fd, config, markSocket, querySocketUid)

    suspend fun startHttpMode(
        config: ClashConfiguration.HttpConfig =
            ClashConfiguration.HttpConfig()
    ): Result<String?> = startHttpModeUseCase(config)

    fun stop() {
        stopProxyUseCase()
        scope.launch {
            runCatching { proxyGroupManager.refreshProxyGroups(true) }
        }
    }

    fun getCachedDelay(nodeName: String): Int? {
        return proxyGroups.value.flatMap { it.proxies }.find { it.name == nodeName }?.delay
    }

    override fun close() {
        scope.cancel("ClashManager closed")
        proxyGroupManager.clearGroupStates()
        stateManager.reset()
    }
}