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
import com.github.yumelira.yumebox.clash.config.ClashConfiguration
import com.github.yumelira.yumebox.clash.core.ClashCore
import com.github.yumelira.yumebox.clash.exception.toConfigImportException
import com.github.yumelira.yumebox.common.util.SystemProxyHelper
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.model.ProfileType
import com.github.yumelira.yumebox.data.repository.ProxyChainResolver
import com.github.yumelira.yumebox.data.repository.ProxyStateRepository
import com.github.yumelira.yumebox.data.repository.SelectionDao
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.domain.model.ProxyState
import com.github.yumelira.yumebox.domain.model.RunningMode
import com.github.yumelira.yumebox.domain.model.TrafficData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.io.Closeable
import java.io.File
import java.net.InetSocketAddress

/**
 * Clash 核心管理器
 *
 * 重构原则：
 * 1. 代理组状态完全由 ProxyStateRepository 管理
 * 2. ClashManager 只负责核心生命周期和基础设施
 * 3. 移除所有手动刷新逻辑，改为自动同步
 * 4. UI 层通过 ProxyStateRepository 观察状态
 */
class ClashManager(
    private val context: Context,
    private val workDir: File,
    private val proxyModeProvider: (() -> TunnelState.Mode)? = null
) : Closeable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val selectionDao = SelectionDao(context)

    // 代理状态仓库 - 唯一的代理组状态源
    val proxyStateRepository = ProxyStateRepository(context, ProxyChainResolver())

    // 代理状态
    private val _proxyState = MutableStateFlow<ProxyState>(ProxyState.Idle)
    val proxyState: StateFlow<ProxyState> = _proxyState.asStateFlow()

    val isRunning: StateFlow<Boolean> = _proxyState
        .map { it.isRunning }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val runningMode: StateFlow<RunningMode> = _proxyState
        .map { state ->
            when (state) {
                is ProxyState.Running -> state.mode
                is ProxyState.Connecting -> state.mode
                else -> RunningMode.None
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, RunningMode.None)

    // 当前配置
    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    // 流量统计
    private val _trafficNow = MutableStateFlow(TrafficData.ZERO)
    val trafficNow: StateFlow<TrafficData> = _trafficNow.asStateFlow()

    private val _trafficTotal = MutableStateFlow(TrafficData.ZERO)
    val trafficTotal: StateFlow<TrafficData> = _trafficTotal.asStateFlow()

    // 隧道状态
    private val _tunnelState = MutableStateFlow<TunnelState?>(null)
    val tunnelState: StateFlow<TunnelState?> = _tunnelState.asStateFlow()

    // 代理组信息 - 从 ProxyStateRepository 暴露
    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = proxyStateRepository.proxyGroups

    // 日志流
    private val _logs = MutableSharedFlow<LogMessage>(replay = 100)
    val logs: SharedFlow<LogMessage> = _logs.asSharedFlow()

    // 监控任务
    private var monitorJob: Job? = null
    private var logJob: Job? = null

    init {
        workDir.mkdirs()
        startLogSubscription()
    }

    /**
     * 加载配置文件
     *
     * @param profile 配置信息
     * @return 加载结果
     */
    suspend fun loadProfile(profile: Profile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val configDir = getConfigDir(profile)
            if (!configDir.exists() || !configDir.isDirectory) {
                return@withContext Result.failure(IllegalStateException("配置目录不存在: ${profile.name}"))
            }

            Timber.d("开始加载配置：${profile.name}")

            // 使用 ClashCore 加载配置（已包含重置和清除 Session Override）
            val loadResult = ClashCore.loadConfig(
                configDir = configDir,
                options = ClashCore.LoadOptions(
                    timeoutMs = 30_000L,
                    resetBeforeLoad = true,
                    clearSessionOverride = true
                )
            )

            if (loadResult.isFailure) {
                val error = loadResult.exceptionOrNull()
                Timber.e(error, "配置加载失败：${profile.name}")
                return@withContext loadResult
            }

            _currentProfile.value = profile

            // 应用代理模式
            proxyModeProvider?.let { provider ->
                try {
                    val mode = provider()
                    val persist = ClashCore.queryOverride(Clash.OverrideSlot.Persist)
                    if (persist.mode != mode) {
                        persist.mode = mode
                        ClashCore.patchOverride(Clash.OverrideSlot.Persist, persist)
                    }
                    val session = ClashCore.queryOverride(Clash.OverrideSlot.Session)
                    session.mode = mode
                    ClashCore.patchOverride(Clash.OverrideSlot.Session, session)
                    Timber.d("应用代理模式：$mode")
                } catch (e: Exception) {
                    Timber.e(e, "应用代理模式失败")
                }
            }

            // 启动代理状态仓库
            proxyStateRepository.start()

            // 恢复用户选择的节点并同步状态
            scope.launch {
                try {


                    // 从数据库恢复选择
                    val selections = selectionDao.getAllSelections(profile.id)
                    Timber.d("恢复 ${selections.size} 个节点选择")

                    selections.forEach { (groupName, proxyName) ->
                        runCatching {
                            ClashCore.selectProxy(groupName, proxyName)
                            Timber.d("恢复选择：$groupName -> $proxyName")
                        }.onFailure { e ->
                            Timber.e(e, "恢复选择失败：$groupName -> $proxyName")
                        }
                    }

                    // 同步一次代理组状态
                    proxyStateRepository.syncFromCore()
                } catch (e: Exception) {
                    Timber.e(e, "恢复节点选择失败")
                }
            }

            // 配置加载成功后立即异步同步节点数据
            scope.launch {
                try {
                    proxyStateRepository.syncFromCore()
                } catch (e: Exception) {
                    // 忽略同步失败
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            val importException = e.toConfigImportException()
            Timber.e(importException, "加载配置失败：${profile.name}")
            Result.failure(importException)
        }
    }

    /**
     * 获取配置目录
     */
    private fun getConfigDir(profile: Profile): File {
        return when (profile.type) {
            ProfileType.FILE -> {
                val configFile = File(profile.config)
                configFile.parentFile ?: configFile.parentFile!!
            }

            ProfileType.URL -> {
                val importedDir = workDir.parentFile?.resolve("imported") ?: File(workDir, "imported")
                File(importedDir, profile.id)
            }
        }
    }

    /**
     * 启动 TUN 模式
     *
     * @param fd TUN 设备文件描述符
     * @param config TUN 配置
     * @param markSocket Socket 标记回调
     * @param querySocketUid 查询 Socket UID 回调
     * @return 启动结果
     */
    suspend fun startTun(
        fd: Int,
        config: ClashConfiguration.TunConfig = ClashConfiguration.TunConfig(),
        markSocket: (Int) -> Boolean,
        querySocketUid: (protocol: Int, source: InetSocketAddress, target: InetSocketAddress) -> Int = { _, _, _ -> -1 }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val profile = _currentProfile.value
                ?: return@withContext Result.failure(IllegalStateException("请先加载配置"))

            Timber.d("启动 TUN 模式")
            _proxyState.value = ProxyState.Connecting(RunningMode.Tun)

            ClashCore.startTun(
                fd = fd,
                stack = config.stack,
                gateway = "${config.gateway}/30",
                portal = "${config.portal}/30",
                dns = config.dns,
                markSocket = markSocket,
                querySocketUid = querySocketUid
            )

            _proxyState.value = ProxyState.Running(profile, RunningMode.Tun)
            startMonitor()

            Timber.d("TUN 模式启动成功")
            Result.success(Unit)
        } catch (e: Exception) {
            val importException = e.toConfigImportException()
            Timber.e(importException, "TUN 模式启动失败")
            _proxyState.value = ProxyState.Error("TUN启动失败: ${importException.message}", importException)
            Result.failure(importException)
        }
    }

    /**
     * 启动 HTTP 代理模式
     *
     * @param config HTTP 配置
     * @return 启动结果，成功时返回实际监听地址
     */
    suspend fun startHttp(
        config: ClashConfiguration.HttpConfig = ClashConfiguration.HttpConfig()
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val profile = _currentProfile.value
                ?: return@withContext Result.failure(IllegalStateException("请先加载配置"))

            Timber.d("启动 HTTP 代理模式")
            _proxyState.value = ProxyState.Connecting(RunningMode.Http(config.address))

            val address = ClashCore.startHttp(config.listenAddress) ?: config.address

            _proxyState.value = ProxyState.Running(profile, RunningMode.Http(address))
            startMonitor()

            Timber.d("HTTP 代理模式启动成功：$address")
            Result.success(address)
        } catch (e: Exception) {
            val importException = e.toConfigImportException()
            Timber.e(importException, "HTTP 代理模式启动失败")
            _proxyState.value = ProxyState.Error("HTTP启动失败: ${importException.message}", importException)
            Result.failure(importException)
        }
    }

    /**
     * 停止代理
     */
    fun stop() {
        runCatching {
            _proxyState.value
            _proxyState.value = ProxyState.Stopping

            // 强制停止所有服务
            try {
                ClashCore.stopTun()
            } catch (e: Exception) {
                // 忽略已经停止的错误
            }

            try {
                ClashCore.stopHttp()
            } catch (e: Exception) {
                // 忽略已经停止的错误
            }

            try {
                SystemProxyHelper.clearSystemProxy(context)
            } catch (e: Exception) {
                // 忽略清除系统代理失败
            }

            // 重置核心
            try {
                ClashCore.reset()
            } catch (e: Exception) {
                // 忽略重置失败
            }

            // 停止代理状态仓库
            proxyStateRepository.stop()

            // 停止监控
            stopMonitor()

            // 最后重置状态
            resetState()

        }.onFailure { e ->
            // 确保重置状态
            try {
                ClashCore.reset()
            } catch (ex: Exception) {
                // 忽略强制重置失败
            }
            proxyStateRepository.stop()
            stopMonitor()
            resetState()
        }
    }


    /**
     * 启动监控
     *
     * 监控流量和隧道状态，但不再监控代理组（由 ProxyStateRepository 负责）
     */
    private fun startMonitor() {
        stopMonitor()

        monitorJob = scope.launch {
            while (isActive) {
                runCatching {
                    // 更新流量统计
                    _trafficNow.value = TrafficData.from(ClashCore.queryTrafficNow())
                    _trafficTotal.value = TrafficData.from(ClashCore.queryTrafficTotal())

                    // 更新隧道状态
                    _tunnelState.value = ClashCore.queryTunnelState()

                }.onFailure { e ->
                    Timber.e(e, "监控任务执行失败")
                }

                delay(1000) // 1 秒更新一次流量和隧道状态
            }
        }
    }

    /**
     * 停止监控
     */
    private fun stopMonitor() {
        monitorJob?.cancel()
        monitorJob = null
    }

    /**
     * 启动日志订阅
     */
    private fun startLogSubscription() {
        logJob = scope.launch {
            try {
                val channel = ClashCore.subscribeLogcat()
                for (log in channel) {
                    // 过滤掉一些噪音日志
                    if (!log.message.contains("Request interrupted by user") &&
                        !log.message.contains("更新延迟")
                    ) {
                        _logs.emit(log)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "日志订阅错误")
            }
        }
    }

    /**
     * 重置状态
     */
    private fun resetState() {
        _proxyState.value = ProxyState.Idle
        _currentProfile.value = null
        _trafficNow.value = TrafficData.ZERO
        _trafficTotal.value = TrafficData.ZERO
        _tunnelState.value = null
    }

    /**
     * 关闭管理器
     */
    override fun close() {
        logJob?.cancel()
        monitorJob?.cancel()
        proxyStateRepository.close()
        scope.cancel("ClashManager closed")
        resetState()
    }
}
