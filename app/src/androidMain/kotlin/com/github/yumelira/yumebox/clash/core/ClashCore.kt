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

package com.github.yumelira.yumebox.clash.core

import com.github.yumelira.yumebox.clash.exception.*
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.bridge.ClashException
import com.github.yumelira.yumebox.core.model.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.net.InetSocketAddress
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Clash 核心薄封装层
 *
 * 提供统一的超时控制、异常处理和日志记录，同时保持对底层 API 的直接访问
 */
object ClashCore {

    /** 配置加载选项 */
    data class LoadOptions(
        val timeoutMs: Long = 30_000L,
        val resetBeforeLoad: Boolean = true,
        val clearSessionOverride: Boolean = true
    ) {
        companion object {
            val DEFAULT = LoadOptions()
        }
    }

    /** 配置下载选项 */
    data class FetchOptions(
        val timeoutMs: Long = 300_000L, // 5分钟
        val force: Boolean = true
    ) {
        companion object {
            val DEFAULT = FetchOptions()
        }
    }

    /** Provider 更新选项 */
    data class ProviderUpdateOptions(
        val timeoutMs: Long = 60_000L,
        val parallel: Boolean = false
    ) {
        companion object {
            val DEFAULT = ProviderUpdateOptions()
        }
    }

    // ==================== 核心生命周期 ====================

    /** 重置核心 */
    fun reset() {
        try {
            Clash.reset()
        } catch (e: Exception) {
            Timber.e(e, "Failed to reset clash core")
            throw e
        }
    }

    /** 强制垃圾回收 */
    fun forceGc() {
        try {
            Clash.forceGc()
        } catch (e: Exception) {
            Timber.e(e, "Failed to trigger GC")
            throw e
        }
    }

    /** 挂起/恢复核心 */
    fun suspend(suspended: Boolean) {
        try {
            Clash.suspendCore(suspended)
        } catch (e: Exception) {
            Timber.e(e, "Failed to suspend clash core")
            throw e
        }
    }

    // ==================== 配置管理 ====================

    /** 加载配置文件（带超时控制） */
    suspend fun loadConfig(
        configDir: File,
        options: LoadOptions = LoadOptions.DEFAULT
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!configDir.exists() || !configDir.isDirectory) {
                return@withContext Result.failure(
                    FileAccessException(
                        "配置目录不存在或不是目录",
                        filePath = configDir.absolutePath,
                        reason = FileAccessException.Reason.NOT_FOUND
                    )
                )
            }

            val configFile = File(configDir, "config.yaml")
            if (!configFile.exists()) {
                return@withContext Result.failure(
                    FileAccessException(
                        "配置文件不存在",
                        filePath = configFile.absolutePath,
                        reason = FileAccessException.Reason.NOT_FOUND
                    )
                )
            }

            Timber.d("Loading config from: ${configDir.absolutePath}")

            // 可选：重置核心
            if (options.resetBeforeLoad) {
                reset()
            }

            // 可选：清除会话覆盖
            if (options.clearSessionOverride) {
                Clash.clearOverride(Clash.OverrideSlot.Session)
            }

            // 加载配置（带超时）
            withTimeout(options.timeoutMs) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    val deferred = Clash.load(configDir)

                    deferred.invokeOnCompletion { error ->
                        if (error != null) {
                            if (!continuation.isCompleted) {
                                continuation.resumeWithException(error)
                            }
                        } else {
                            if (!continuation.isCompleted) {
                                continuation.resume(Unit)
                            }
                        }
                    }

                    continuation.invokeOnCancellation {
                        deferred.cancel()
                    }
                }
            }

            Result.success(Unit)

        } catch (e: TimeoutCancellationException) {
            val error = TimeoutException(
                "配置加载超时",
                e,
                timeoutMs = options.timeoutMs,
                operation = "加载配置"
            )
            Timber.e(error, "Config load timeout")
            Result.failure(error)

        } catch (e: ClashException) {
            val error = ConfigValidationException(
                e.message ?: "配置加载失败",
                e
            )
            Timber.e(error, "Config load failed")
            Result.failure(error)

        } catch (e: Exception) {
            val error = e.toConfigImportException()
            Timber.e(error, "Config load error")
            Result.failure(error)
        }
    }

    /** 下载并验证配置（带超时控制） */
    suspend fun fetchAndValidate(
        targetDir: File,
        url: String,
        options: FetchOptions = FetchOptions.DEFAULT,
        onProgress: ((FetchStatus) -> Unit)? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Fetching config, url=$url, force=${options.force}")

            withTimeout(options.timeoutMs) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    val deferred = Clash.fetchAndValid(
                        path = targetDir,
                        url = url,
                        force = options.force,
                        reportStatus = { status ->
                            onProgress?.invoke(status)
                        }
                    )

                    deferred.invokeOnCompletion { error ->
                        if (error != null) {
                            if (!continuation.isCompleted) {
                                continuation.resumeWithException(error)
                            }
                        } else {
                            if (!continuation.isCompleted) {
                                continuation.resume(Unit)
                            }
                        }
                    }

                    continuation.invokeOnCancellation {
                        deferred.cancel()
                    }
                }
            }

            Result.success(Unit)

        } catch (e: TimeoutCancellationException) {
            val error = TimeoutException(
                "配置下载超时",
                e,
                timeoutMs = options.timeoutMs,
                operation = "下载配置"
            )
            Timber.e(error, "Fetch timeout")
            Result.failure(error)

        } catch (e: ClashException) {
            // 将 ClashException 转换为更具体的异常
            val error = when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    NetworkException(e.message ?: "网络错误", e, url = url)

                e.message?.contains("invalid", ignoreCase = true) == true ||
                        e.message?.contains("parse", ignoreCase = true) == true ->
                    ConfigValidationException(e.message ?: "配置格式错误", e)

                else ->
                    NetworkException(e.message ?: "下载失败", e, url = url)
            }
            Timber.e(error, "Fetch failed")
            Result.failure(error)

        } catch (e: Exception) {
            val error = e.toConfigImportException()
            Timber.e(error, "Fetch error")
            Result.failure(error)
        }
    }

    // ==================== 覆盖配置管理 ====================

    /** 查询覆盖配置 */
    fun queryOverride(slot: Clash.OverrideSlot): ConfigurationOverride {
        return try {
            Clash.queryOverride(slot)
        } catch (e: Exception) {
            Timber.w(e, "Failed to query override for slot $slot, returning empty")
            ConfigurationOverride()
        }
    }

    /** 应用覆盖配置 */
    fun patchOverride(slot: Clash.OverrideSlot, override: ConfigurationOverride) {
        try {
            Clash.patchOverride(slot, override)
        } catch (e: Exception) {
            Timber.e(e, "Failed to patch override for slot $slot")
            throw e
        }
    }

    /** 清除覆盖配置 */
    fun clearOverride(slot: Clash.OverrideSlot) {
        try {
            Clash.clearOverride(slot)
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear override for slot $slot")
            throw e
        }
    }

    // ==================== Provider 管理 ====================

    /** 查询所有 Providers */
    fun queryProviders(): List<Provider> {
        return try {
            Clash.queryProviders()
        } catch (e: Exception) {
            Timber.e(e, "Failed to query providers")
            emptyList()
        }
    }

    /** 更新单个 Provider（带超时） */
    suspend fun updateProvider(
        type: Provider.Type,
        name: String,
        options: ProviderUpdateOptions = ProviderUpdateOptions.DEFAULT
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Updating provider: $name (type=$type)")

            withTimeout(options.timeoutMs) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    val deferred = Clash.updateProvider(type, name)

                    deferred.invokeOnCompletion { error ->
                        if (error != null) {
                            if (!continuation.isCompleted) {
                                continuation.resumeWithException(error)
                            }
                        } else {
                            if (!continuation.isCompleted) {
                                continuation.resume(Unit)
                            }
                        }
                    }

                    continuation.invokeOnCancellation {
                        deferred.cancel()
                    }
                }
            }

            Result.success(Unit)

        } catch (e: TimeoutCancellationException) {
            val error = ProviderUpdateException(
                "Provider 更新超时",
                e,
                providerName = name
            )
            Timber.e(error, "Provider update timeout")
            Result.failure(error)

        } catch (e: ClashException) {
            val error = ProviderUpdateException(
                e.message ?: "Provider 更新失败",
                e,
                providerName = name
            )
            Timber.e(error, "Provider update failed")
            Result.failure(error)

        } catch (e: Exception) {
            val error = e.toConfigImportException()
            Timber.e(error, "Provider update error")
            Result.failure(error)
        }
    }

    /** 批量更新 Providers */
    suspend fun updateProviders(
        providers: List<Pair<Provider.Type, String>>,
        options: ProviderUpdateOptions = ProviderUpdateOptions.DEFAULT
    ): Map<String, Result<Unit>> = withContext(Dispatchers.IO) {
        if (options.parallel) {
            // 并行更新
            providers.map { (type, name) ->
                name to async {
                    updateProvider(type, name, options)
                }
            }.associate { (name, deferred) ->
                name to deferred.await()
            }
        } else {
            // 串行更新
            providers.associate { (type, name) ->
                name to updateProvider(type, name, options)
            }
        }
    }

    // ==================== 代理组管理 ====================

    /** 查询代理组名称列表 */
    fun queryGroupNames(excludeNotSelectable: Boolean = false): List<String> {
        return try {
            Clash.queryGroupNames(excludeNotSelectable)
        } catch (e: Exception) {
            Timber.e(e, "Failed to query group names")
            emptyList()
        }
    }

    /** 查询代理组详情 */
    fun queryGroup(name: String, sort: ProxySort = ProxySort.Default): ProxyGroup {
        return try {
            Clash.queryGroup(name, sort)
        } catch (e: Exception) {
            Timber.e(e, "Failed to query group: $name")
            ProxyGroup(Proxy.Type.Unknown, emptyList(), "")
        }
    }

    /** 选择代理节点 */
    fun selectProxy(selector: String, proxyName: String): Boolean {
        return try {
            val result = Clash.patchSelector(selector, proxyName)
            if (result) {
                Timber.d("Proxy selected: $selector -> $proxyName")
            } else {
                Timber.w("Failed to select proxy: $selector -> $proxyName")
            }
            result
        } catch (e: Exception) {
            Timber.e(e, "Error selecting proxy: $selector -> $proxyName")
            false
        }
    }

    /** 健康检查（单个组） */
    suspend fun healthCheck(
        groupName: String,
        timeoutMs: Long = 30_000L
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            withTimeout(timeoutMs) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    val deferred = Clash.healthCheck(groupName)

                    deferred.invokeOnCompletion { error ->
                        if (error != null) {
                            if (!continuation.isCompleted) {
                                continuation.resumeWithException(error)
                            }
                        } else {
                            if (!continuation.isCompleted) {
                                continuation.resume(Unit)
                            }
                        }
                    }

                    continuation.invokeOnCancellation {
                        deferred.cancel()
                    }
                }
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Health check failed for: $groupName")
            Result.failure(e)
        }
    }

    /** 健康检查（所有组） */
    fun healthCheckAll() {
        try {
            Clash.healthCheckAll()
        } catch (e: Exception) {
            Timber.e(e, "Failed to trigger health check all")
            throw e
        }
    }

    // ==================== 隧道管理 ====================

    /** 启动 TUN */
    fun startTun(
        fd: Int,
        stack: String,
        gateway: String,
        portal: String,
        dns: String,
        markSocket: (Int) -> Boolean,
        querySocketUid: (protocol: Int, source: InetSocketAddress, target: InetSocketAddress) -> Int
    ) {
        try {
            Clash.startTun(fd, stack, gateway, portal, dns, markSocket, querySocketUid)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start TUN")
            throw e
        }
    }

    /** 停止 TUN */
    fun stopTun() {
        try {
            Clash.stopTun()
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop TUN")
            throw e
        }
    }

    /** 启动 HTTP 代理 */
    fun startHttp(listenAt: String): String? {
        return try {
            Clash.startHttp(listenAt)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start HTTP proxy")
            throw e
        }
    }

    /** 停止 HTTP 代理 */
    fun stopHttp() {
        try {
            Clash.stopHttp()
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop HTTP proxy")
            throw e
        }
    }

    // ==================== 状态查询 ====================

    /** 查询隧道状态 */
    fun queryTunnelState(): TunnelState {
        return try {
            Clash.queryTunnelState()
        } catch (e: Exception) {
            Timber.e(e, "Failed to query tunnel state")
            throw e
        }
    }

    /** 查询当前流量 */
    fun queryTrafficNow(): Traffic {
        return try {
            Clash.queryTrafficNow()
        } catch (e: Exception) {
            Timber.e(e, "Failed to query traffic now")
            0L
        }
    }

    /** 查询总流量 */
    fun queryTrafficTotal(): Traffic {
        return try {
            Clash.queryTrafficTotal()
        } catch (e: Exception) {
            Timber.e(e, "Failed to query traffic total")
            0L
        }
    }

    /** 查询配置 */
    fun queryConfiguration(): UiConfiguration {
        return try {
            Clash.queryConfiguration()
        } catch (e: Exception) {
            Timber.e(e, "Failed to query configuration")
            throw e
        }
    }

    // ==================== 系统通知 ====================

    /** 通知 DNS 变更 */
    fun notifyDnsChanged(dns: List<String>) {
        try {
            Clash.notifyDnsChanged(dns)
        } catch (e: Exception) {
            Timber.e(e, "Failed to notify DNS changed")
        }
    }

    /** 通知时区变更 */
    fun notifyTimeZoneChanged(name: String, offset: Int) {
        try {
            Clash.notifyTimeZoneChanged(name, offset)
        } catch (e: Exception) {
            Timber.e(e, "Failed to notify timezone changed")
        }
    }

    /** 通知已安装应用变更 */
    fun notifyInstalledAppsChanged(uids: List<Pair<Int, String>>) {
        try {
            Clash.notifyInstalledAppsChanged(uids)
        } catch (e: Exception) {
            Timber.e(e, "Failed to notify installed apps changed")
        }
    }

    // ==================== 其他 ====================

    /** 订阅日志 */
    fun subscribeLogcat() = Clash.subscribeLogcat()

    /** 设置自定义 User Agent */
    fun setCustomUserAgent(userAgent: String) {
        try {
            Clash.setCustomUserAgent(userAgent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to set custom user agent")
        }
    }

    /** 直接访问底层 Clash 对象（用于特殊需求） */
    fun getRawClash(): Clash = Clash
}
