package com.github.yumelira.yumebox.autorestart

import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.data.repository.ProxyConnectionService
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import com.github.yumelira.yumebox.data.store.NetworkSettingsStorage
import com.github.yumelira.yumebox.data.store.ProfilesStorage
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

object AutoRestartCoordinator {
    private const val TAG = "AutoRestartCoordinator"
    private const val AUTO_START_COOLDOWN_MS = 15_000L
    enum class TriggerSource {
        BootBroadcast,  // 启动/更新广播触发
        UiResume        // UI恢复触发
    }
    enum class SkipReason {
        AutomaticRestartDisabled,  // 设置未开启
        Cooldown,                  // 冷却期内
        StateBusy,                 // 状态忙（运行中或过渡中）
        NoProfile                  // 无可用配置文件
    }
    data class SkipStats(
        val cooldownSkippedCount: Long,
        val stateBusySkippedCount: Long
    )
    sealed interface AutoStartResult {
        data object Started : AutoStartResult
        data class Skipped(
            val reason: SkipReason,
            val detail: String? = null,
            val stats: SkipStats
        ) : AutoStartResult
        data class Failed(val error: Throwable) : AutoStartResult
    }
    suspend fun enqueue(
        source: TriggerSource,
        proxyConnectionService: ProxyConnectionService,
        appSettingsStorage: AppSettingsStorage,
        networkSettingsStorage: NetworkSettingsStorage,
        profilesStore: ProfilesStorage,
        clashManager: ClashManager
    ): AutoStartResult {
        val deferred = CompletableDeferred<AutoStartResult>()
        queue.send(
            AutoStartRequest(
                source = source,
                proxyConnectionService = proxyConnectionService,
                appSettingsStorage = appSettingsStorage,
                networkSettingsStorage = networkSettingsStorage,
                profilesStore = profilesStore,
                clashManager = clashManager,
                result = deferred
            )
        )
        return deferred.await()
    }
    fun getSkipStats(): SkipStats {
        return SkipStats(
            cooldownSkippedCount = cooldownSkippedCount.get(),
            stateBusySkippedCount = stateBusySkippedCount.get()
        )
    }
    private data class AutoStartRequest(
        val source: TriggerSource,
        val proxyConnectionService: ProxyConnectionService,
        val appSettingsStorage: AppSettingsStorage,
        val networkSettingsStorage: NetworkSettingsStorage,
        val profilesStore: ProfilesStorage,
        val clashManager: ClashManager,
        val result: CompletableDeferred<AutoStartResult>
    )
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queue = Channel<AutoStartRequest>(capacity = Channel.UNLIMITED)
    private val autoStartMutex = Mutex()
    private val cooldownSkippedCount = AtomicLong(0)
    private val stateBusySkippedCount = AtomicLong(0)

    @Volatile
    private var lastAutoStartAtMs: Long = 0L
    init {
        scope.launch {
            for (request in queue) {
                val outcome = runRequest(request)
                request.result.complete(outcome)
            }
        }
    }
    private suspend fun runRequest(request: AutoStartRequest): AutoStartResult {
        val maxRetries = when (request.source) {
            TriggerSource.BootBroadcast -> 3
            TriggerSource.UiResume -> 1
        }
        var retryCount = 0
        var lastFailure: Throwable? = null
        while (retryCount < maxRetries) {
            when (val result = checkAndAutoStart(
                request = request,
                isBootCompleted = request.source == TriggerSource.BootBroadcast
            )) {
                is AutoStartResult.Started,
                is AutoStartResult.Skipped -> return result
                is AutoStartResult.Failed -> {
                    lastFailure = result.error
                    retryCount++
                    if (retryCount < maxRetries) {
                        Timber.tag(TAG).w(
                            result.error,
                            "自动启动失败，准备重试（source=%s, %d/%d）",
                            request.source,
                            retryCount,
                            maxRetries
                        )
                        delay(1000L * retryCount)
                    }
                }
            }
        }
        return AutoStartResult.Failed(
            lastFailure ?: IllegalStateException("Auto-start failed without error")
        )
    }
    private suspend fun checkAndAutoStart(
        request: AutoStartRequest,
        isBootCompleted: Boolean
    ): AutoStartResult {
        return autoStartMutex.withLock {
            val automaticRestart = request.appSettingsStorage.automaticRestart.value
            if (!automaticRestart) {
                return@withLock AutoStartResult.Skipped(
                    reason = SkipReason.AutomaticRestartDisabled,
                    stats = getSkipStats()
                )
            }
            val now = System.currentTimeMillis()
            if (now - lastAutoStartAtMs < AUTO_START_COOLDOWN_MS) {
                val remainingMs = AUTO_START_COOLDOWN_MS - (now - lastAutoStartAtMs)
                val cooldownCount = cooldownSkippedCount.incrementAndGet()
                val stats = getSkipStats()
                Timber.tag(TAG).d(
                    "自动启动冷却中，跳过本次触发（source=%s, remaining=%dms, cooldownSkipped=%d, stateBusySkipped=%d）",
                    if (isBootCompleted) "boot" else "ui",
                    remainingMs,
                    cooldownCount,
                    stats.stateBusySkippedCount
                )
                return@withLock AutoStartResult.Skipped(
                    reason = SkipReason.Cooldown,
                    detail = "remainingMs=$remainingMs",
                    stats = stats
                )
            }
            val state = request.clashManager.proxyState.value
            if (state.isRunning || state.isTransitioning) {
                val busyCount = stateBusySkippedCount.incrementAndGet()
                val stats = getSkipStats()
                Timber.tag(TAG).d(
                    "代理当前状态为 %s，跳过自动启动（cooldownSkipped=%d, stateBusySkipped=%d）",
                    state::class.simpleName,
                    stats.cooldownSkippedCount,
                    busyCount
                )
                return@withLock AutoStartResult.Skipped(
                    reason = SkipReason.StateBusy,
                    detail = "proxyState=${state::class.simpleName}",
                    stats = stats
                )
            }
            val profileId = getProfileToStart(request.profilesStore)
            if (profileId == null) {
                Timber.tag(TAG).w("没有可用的配置文件，无法自动启动")
                return@withLock AutoStartResult.Skipped(
                    reason = SkipReason.NoProfile,
                    stats = getSkipStats()
                )
            }
            if (isBootCompleted) {
                delay(3000)
            }
            val proxyMode = request.networkSettingsStorage.proxyMode.value
            val startResult = runCatching {
                request.proxyConnectionService.startDirect(profileId = profileId, mode = proxyMode)
            }.getOrElse { e ->
                Timber.tag(TAG).e(e, "自动启动代理失败: ${e.message}")
                return@withLock AutoStartResult.Failed(e)
            }
            if (startResult.isFailure) {
                val error = startResult.exceptionOrNull() ?: IllegalStateException("startDirect failed")
                Timber.tag(TAG).e(error, "自动启动代理失败")
                return@withLock AutoStartResult.Failed(error)
            }
            lastAutoStartAtMs = System.currentTimeMillis()
            AutoStartResult.Started
        }
    }
    private fun getProfileToStart(profilesStore: ProfilesStorage): String? {
        val profiles = profilesStore.getAllProfiles()
        val lastUsedId = profilesStore.lastUsedProfileId
        if (lastUsedId.isNotEmpty()) {
            val lastUsedProfile = profiles.find { it.id == lastUsedId }
            if (lastUsedProfile != null) {
                return lastUsedId
            }
        }
        val enabledProfile = profiles.find { it.enabled }
        if (enabledProfile != null) {
            return enabledProfile.id
        }
        val firstProfile = profiles.firstOrNull()
        if (firstProfile != null) {
            return firstProfile.id
        }
        return null
    }
}
