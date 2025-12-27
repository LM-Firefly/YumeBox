package com.github.yumelira.yumebox.clash.config

import com.github.yumelira.yumebox.core.Clash
import timber.log.Timber

object Configuration {
    private const val TAG = "Configuration"
    object Defaults {
        const val MIXED_PORT = 7890
        const val HTTP_ADDRESS = "127.0.0.1"
        const val TUN_MTU = 9000
        const val TUN_GATEWAY = "172.19.0.1"
        const val TUN_PORTAL = "172.19.0.2"
        const val TUN_DNS = "172.19.0.2"
        const val TUN_STACK = "gvisor"
    }

    fun getSmartControllerAndSecret(): Pair<String, String> {
        val session = Clash.queryOverride(Clash.OverrideSlot.Session)
        val persist = Clash.queryOverride(Clash.OverrideSlot.Persist)
        val config = Clash.queryConfiguration()
        val controller = session.externalController?.takeIf { it.isNotBlank() } ?: persist.externalController?.takeIf { it.isNotBlank() } ?: config.externalController?.takeIf { it.isNotBlank() } ?: "127.0.0.1:9090"
        val secret = session.secret?.takeIf { it.isNotBlank() } ?: persist.secret?.takeIf { it.isNotBlank() } ?: config.secret?.takeIf { it.isNotBlank() } ?: ""
        Timber.tag(TAG).d("getSmartControllerAndSecret: session=%s, persist=%s, config=%s, result=%s", session.externalController, persist.externalController, config.externalController, controller)
        return controller to secret
    }

    data class TunConfig(
        val mtu: Int = Defaults.TUN_MTU,
        val gateway: String = Defaults.TUN_GATEWAY,
        val portal: String = Defaults.TUN_PORTAL,
        val dns: String = Defaults.TUN_DNS,
        val stack: String = Defaults.TUN_STACK,
        val dnsHijacking: Boolean = true,
        val autoRoute: Boolean = true
    )

    data class HttpConfig(
        val host: String = Defaults.HTTP_ADDRESS, val port: Int = Defaults.MIXED_PORT
    ) {
        val address: String get() = "$host:$port"
        val listenAddress: String get() = ":$port"
    }
}
