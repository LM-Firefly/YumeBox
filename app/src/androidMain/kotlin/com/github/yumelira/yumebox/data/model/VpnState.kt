package com.github.yumelira.yumebox.data.model

data class VpnState(
    val status: VpnStatus,
    val isRunning: Boolean = status == VpnStatus.RUNNING,
) {
    companion object {
        val STOPPED = VpnState(VpnStatus.STOPPED)
        val STARTING = VpnState(VpnStatus.STARTING)
        val RUNNING = VpnState(VpnStatus.RUNNING)
        val STOPPING = VpnState(VpnStatus.STOPPING)
    }
}
