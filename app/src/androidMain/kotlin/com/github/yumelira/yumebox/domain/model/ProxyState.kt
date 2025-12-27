package com.github.yumelira.yumebox.domain.model

import com.github.yumelira.yumebox.data.model.Profile

sealed interface ProxyState {
    data object Idle : ProxyState

    data class Preparing(val message: String = "正在准备...") : ProxyState

    data class Connecting(val mode: RunningMode) : ProxyState

    data class Running(
        val profile: Profile,
        val mode: RunningMode
    ) : ProxyState

    data object Stopping : ProxyState

    data class Error(val message: String, val cause: Throwable? = null) : ProxyState

    val isRunning: Boolean get() = this is Running

    val isTransitioning: Boolean get() = this is Preparing || this is Connecting || this is Stopping

    val canStart: Boolean get() = this is Idle || this is Error

    val canStop: Boolean get() = this is Running
}
