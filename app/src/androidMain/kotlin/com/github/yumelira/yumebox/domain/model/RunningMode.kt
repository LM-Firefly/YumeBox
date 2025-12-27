package com.github.yumelira.yumebox.domain.model

sealed interface RunningMode {
    data object None : RunningMode
    data object Tun : RunningMode
    data class Http(val address: String) : RunningMode
}
