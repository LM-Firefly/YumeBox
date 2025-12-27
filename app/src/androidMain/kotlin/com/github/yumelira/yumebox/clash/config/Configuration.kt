package com.github.yumelira.yumebox.clash.config

import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.ConfigurationOverride
import com.github.yumelira.yumebox.core.model.TunnelState
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

    sealed class ProxyMode {
        object Tun : ProxyMode()
        data class Http(val port: Int = Defaults.MIXED_PORT) : ProxyMode()
    }

    fun applyOverride(
        mode: ProxyMode,
        slot: Clash.OverrideSlot = Clash.OverrideSlot.Session
    ) {
        val persist = Clash.queryOverride(Clash.OverrideSlot.Persist)
        val sessionPrev = Clash.queryOverride(Clash.OverrideSlot.Session)
        val controller = sessionPrev.externalController?.takeIf { it.isNotBlank() }
            ?: persist.externalController?.takeIf { it.isNotBlank() }
        val secret = sessionPrev.secret ?: persist.secret
        Timber.tag(TAG).d("Applying override. Persist Controller: ${persist.externalController}, Session Controller: ${sessionPrev.externalController}, SecretFromSession: ${if (sessionPrev.secret != null) "***" else "null"}")
        Timber.tag(TAG).d("Resolved Controller: $controller, Secret: ${if (!secret.isNullOrEmpty()) "***" else "empty"}")
        val base = persist.copy()
        if (sessionPrev.externalController?.isNotBlank() == true) {
            base.externalController = sessionPrev.externalController
        }
        if (sessionPrev.secret != null) {
            base.secret = sessionPrev.secret
        }
        when (mode) {
            is ProxyMode.Tun -> {
                base.mode = TunnelState.Mode.Rule
                base.mixedPort = null
            }
            is ProxyMode.Http -> {
                base.mode = TunnelState.Mode.Rule
                base.mixedPort = mode.port
            }
        }
        if (controller != null) base.externalController = controller
        if (secret != null) base.secret = secret
        Clash.patchOverride(slot, base)
        Timber.tag(TAG).d("Override patched to slot $slot")
    }

    fun getSmartControllerAndSecret(): Pair<String, String> {
        val session = Clash.queryOverride(Clash.OverrideSlot.Session)
        val persist = Clash.queryOverride(Clash.OverrideSlot.Persist)
        val config = Clash.queryConfiguration()
        val controller = session.externalController?.takeIf { it.isNotBlank() } ?: persist.externalController?.takeIf { it.isNotBlank() } ?: config.externalController?.takeIf { it.isNotBlank() } ?: "127.0.0.1:9090"
        val secret = session.secret ?: persist.secret ?: config.secret ?: ""
        return controller to secret
    }

    fun clearOverride(slot: Clash.OverrideSlot = Clash.OverrideSlot.Session) {
        Clash.clearOverride(slot)
    }

    fun getCurrentOverride(slot: Clash.OverrideSlot = Clash.OverrideSlot.Session): ConfigurationOverride {
        return Clash.queryOverride(slot)
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
