package com.github.yumelira.yumebox.core.util

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import timber.log.Timber

object StartupTaskCoordinator {
    private val runtimeWarmupFallback = CompletableDeferred(Unit)

    @Volatile
    private var runtimeWarmup: Deferred<Unit>? = null

    @Volatile
    private var backgroundWarmup: Deferred<Unit>? = null

    fun startRuntimeWarmup(
        scope: CoroutineScope,
        block: suspend () -> Unit,
    ) {
        if (runtimeWarmup != null) return
        synchronized(this) {
            if (runtimeWarmup != null) return
            runtimeWarmup = scope.async {
                try {
                    block()
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    Timber.e(error, "Runtime warmup failed")
                    throw error
                }
            }
        }
    }

    fun startBackgroundWarmup(
        scope: CoroutineScope,
        block: suspend () -> Unit,
    ) {
        if (backgroundWarmup != null) return
        synchronized(this) {
            if (backgroundWarmup != null) return
            backgroundWarmup = scope.async {
                try {
                    block()
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    Timber.e(error, "Background warmup failed")
                }
            }
        }
    }

    suspend fun awaitRuntimeWarmup() {
        (runtimeWarmup ?: runtimeWarmupFallback).await()
    }
}
