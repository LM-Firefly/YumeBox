package com.github.yumelira.yumebox.clash.core

import com.github.yumelira.yumebox.clash.exception.ConfigValidationException
import com.github.yumelira.yumebox.clash.exception.FileAccessException
import com.github.yumelira.yumebox.clash.exception.TimeoutException
import com.github.yumelira.yumebox.clash.exception.toConfigImportException
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.bridge.ClashException
import com.github.yumelira.yumebox.core.model.ConfigurationOverride
import com.github.yumelira.yumebox.core.model.Traffic
import com.github.yumelira.yumebox.core.model.TunnelState
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.net.InetSocketAddress
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object ClashCore {

    data class LoadOptions(
        val timeoutMs: Long = 30_000L,
        val resetBeforeLoad: Boolean = true,
        val clearSessionOverride: Boolean = true
    ) {
        companion object {
            val DEFAULT = LoadOptions()
        }
    }

    fun reset() {
        try {
            Clash.reset()
        } catch (e: Exception) {
            Timber.e(e, "Failed to reset clash core")
            throw e
        }
    }

    fun suspend(suspended: Boolean) {
        try {
            Clash.suspendCore(suspended)
        } catch (e: Exception) {
            Timber.e(e, "Failed to suspend clash core")
            throw e
        }
    }

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

            if (options.resetBeforeLoad) {
                reset()
            }

            if (options.clearSessionOverride) {
                Clash.clearOverride(Clash.OverrideSlot.Session)
            }

            withTimeout(options.timeoutMs) {
                suspendCancellableCoroutine { continuation ->
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

    fun queryOverride(slot: Clash.OverrideSlot): ConfigurationOverride {
        return try {
            Clash.queryOverride(slot)
        } catch (e: Exception) {
            Timber.w(e, "Failed to query override for slot $slot, returning empty")
            ConfigurationOverride()
        }
    }

    fun patchOverride(slot: Clash.OverrideSlot, override: ConfigurationOverride) {
        try {
            Clash.patchOverride(slot, override)
        } catch (e: Exception) {
            Timber.e(e, "Failed to patch override for slot $slot")
            throw e
        }
    }


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

    fun stopTun() {
        try {
            Clash.stopTun()
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop TUN")
            throw e
        }
    }

    fun startHttp(listenAt: String): String? {
        return try {
            Clash.startHttp(listenAt)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start HTTP proxy")
            throw e
        }
    }

    fun stopHttp() {
        try {
            Clash.stopHttp()
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop HTTP proxy")
            throw e
        }
    }

    fun queryTunnelState(): TunnelState {
        return try {
            Clash.queryTunnelState()
        } catch (e: Exception) {
            Timber.e(e, "Failed to query tunnel state")
            throw e
        }
    }

    fun queryTrafficNow(): Traffic {
        return try {
            Clash.queryTrafficNow()
        } catch (e: Exception) {
            Timber.e(e, "Failed to query traffic now")
            0L
        }
    }

    fun queryTrafficTotal(): Traffic {
        return try {
            Clash.queryTrafficTotal()
        } catch (e: Exception) {
            Timber.e(e, "Failed to query traffic total")
            0L
        }
    }

    fun subscribeLogcat() = Clash.subscribeLogcat()

}
