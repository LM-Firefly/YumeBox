package com.github.yumelira.yumebox.runtime.client.internal

import android.content.Context
import com.github.yumelira.yumebox.runtime.api.service.runtime.entity.RuntimeOwner
import com.github.yumelira.yumebox.runtime.client.remote.ServiceClient

internal class RuntimeBackendRouter(
    private val appContext: Context,
    private val ownerProvider: () -> RuntimeOwner,
    private val runningProvider: () -> Boolean,
) {
    val owner: RuntimeOwner get() = ownerProvider()
    val running: Boolean get() = runningProvider()
    val isRootTun: Boolean get() = ownerProvider() == RuntimeOwner.RootTun
    suspend fun <T> dispatch(
        requireRunning: Boolean = false,
        defaultIfNotRunning: () -> T = { error("Runtime is not running") },
        onRoot: suspend (Context) -> T,
        onLocal: suspend () -> T,
    ): T {
        if (requireRunning && !runningProvider()) return defaultIfNotRunning()
        return when (ownerProvider()) {
            RuntimeOwner.RootTun -> onRoot(appContext)
            else -> {
                ServiceClient.connect(appContext)
                onLocal()
            }
        }
    }
    suspend fun ensureLocalConnected() {
        if (ownerProvider() != RuntimeOwner.RootTun) {
            ServiceClient.connect(appContext)
        }
    }
}
