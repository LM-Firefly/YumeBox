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
import com.github.yumelira.yumebox.clash.testing.ProxyTestManager
import com.github.yumelira.yumebox.domain.model.HealthStatus
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.domain.model.RunningMode
import com.github.yumelira.yumebox.domain.usecase.*
import timber.log.Timber
import java.io.Closeable
import java.io.File


class ClashManager(
    private val context: Context,
    private val workDir: File
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

    val proxyState: StateFlow<com.github.yumelira.yumebox.domain.model.ProxyState> = stateManager.proxyState
    val isRunning: StateFlow<Boolean> = stateManager.isRunning
    val currentProfile: StateFlow<com.github.yumelira.yumebox.data.model.Profile?> = stateManager.currentProfile
    val trafficNow: StateFlow<com.github.yumelira.yumebox.domain.model.TrafficData> = stateManager.trafficNow
    val trafficTotal: StateFlow<com.github.yumelira.yumebox.domain.model.TrafficData> = stateManager.trafficTotal
    val tunnelState: StateFlow<com.github.yumelira.yumebox.core.model.TunnelState?> = stateManager.tunnelState
    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = proxyGroupManager.proxyGroups
    val runningMode: StateFlow<RunningMode> = stateManager.runningMode

    val testStates: StateFlow<Map<String, ProxyTestManager.TestState>> = proxyTestManager.testStates
    val testResults: SharedFlow<ProxyTestManager.TestResult> = proxyTestManager.testResults
    val queueState: StateFlow<ProxyTestManager.QueueState> = proxyTestManager.queueState

    private val _healthStatus = MutableStateFlow(HealthStatus())
    val healthStatus: StateFlow<HealthStatus> = _healthStatus.asStateFlow()

    private val _logs = MutableSharedFlow<com.github.yumelira.yumebox.core.model.LogMessage>(replay = 100)
    val logs: SharedFlow<com.github.yumelira.yumebox.core.model.LogMessage> = _logs.asSharedFlow()

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
                    val group = com.github.yumelira.yumebox.core.Clash.queryGroup(
                        result.groupName, 
                        com.github.yumelira.yumebox.core.model.ProxySort.Default
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
                val logChannel = com.github.yumelira.yumebox.core.Clash.subscribeLogcat()
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
        val groupNames = com.github.yumelira.yumebox.core.Clash.queryGroupNames(excludeNotSelectable = false)
        if (groupNames.isEmpty()) throw Exception("No proxy groups available")
        groupNames.forEach { testProxyDelay(it, priority = ProxyTestManager.Priority.NORMAL) }
        "batch_test_${System.currentTimeMillis()}"
    }

    fun getTestStatistics(): ProxyTestManager.TestStatistics = proxyTestManager.getTestStatistics()

    suspend fun selectProxy(groupName: String, proxyName: String): Boolean = 
        selectProxyUseCase(groupName, proxyName)

    suspend fun refreshProxyGroups(skipCacheClear: Boolean = false): Result<Unit> = 
        refreshProxyGroupsUseCase(skipCacheClear)

    suspend fun healthCheck(groupName: String): Result<Unit> = healthCheckUseCase(groupName)
    suspend fun healthCheckAll(): Result<Unit> = healthCheckUseCase.checkAll()

    suspend fun reloadCurrentProfile(): Result<Unit> = reloadProfileUseCase()

    suspend fun downloadProfileOnly(
        profile: com.github.yumelira.yumebox.data.model.Profile,
        forceDownload: Boolean = true,
        onProgress: ((String, Int) -> Unit)? = null
    ): Result<String> = downloadProfileUseCase(profile, forceDownload, onProgress)

    suspend fun loadProfile(
        profile: com.github.yumelira.yumebox.data.model.Profile,
        forceDownload: Boolean = false,
        onProgress: ((String, Int) -> Unit)? = null,
        willUseTunMode: Boolean = false,
        quickStart: Boolean = false
    ): Result<String> {
        return loadProfileUseCase(profile, forceDownload, onProgress, willUseTunMode, quickStart)
            .onSuccess {
                scope.launch { 
                    delay(300)
                    refreshProxyGroups(skipCacheClear = true) 
                }
            }
    }

    suspend fun startTunMode(
        fd: Int,
        config: com.github.yumelira.yumebox.clash.config.ClashConfiguration.TunConfig = 
            com.github.yumelira.yumebox.clash.config.ClashConfiguration.TunConfig(),
        markSocket: (Int) -> Boolean,
        querySocketUid: (protocol: Int, source: java.net.InetSocketAddress, target: java.net.InetSocketAddress) -> Int = { _, _, _ -> -1 }
    ): Result<Unit> = startTunModeUseCase(fd, config, markSocket, querySocketUid)

    suspend fun startHttpMode(
        config: com.github.yumelira.yumebox.clash.config.ClashConfiguration.HttpConfig = 
            com.github.yumelira.yumebox.clash.config.ClashConfiguration.HttpConfig()
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