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

package com.github.yumelira.yumebox.service

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.ProxyInfo
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import com.github.yumelira.yumebox.clash.config.Configuration
import com.github.yumelira.yumebox.clash.config.RouteConfig
import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.data.model.AccessControlMode
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import com.github.yumelira.yumebox.data.store.NetworkSettingsStorage
import com.github.yumelira.yumebox.data.store.ProfilesStore
import com.github.yumelira.yumebox.service.delegate.ClashServiceDelegate
import com.github.yumelira.yumebox.service.notification.ServiceNotificationManager
import timber.log.Timber
import java.net.InetSocketAddress
import java.security.SecureRandom

class ClashVpnService : VpnService() {

    companion object {
        private const val TAG = "ClashVpnService"
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        private const val EXTRA_PROFILE_ID = "profile_id"
        
        private val random = SecureRandom()

        fun start(context: Context, profileId: String) {
            val intent = Intent(context, ClashVpnService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PROFILE_ID, profileId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(Intent(context, ClashVpnService::class.java).apply {
                action = ACTION_STOP
            })
        }
        
        fun requestStop() {
            Clash.stopHttp()
            Clash.stopTun()
        }
    }

    private val clashManager: ClashManager by inject()
    private val profilesStore: ProfilesStore by inject()
    private val appSettingsStorage: AppSettingsStorage by inject()
    private val networkSettings: NetworkSettingsStorage by inject()

    private val delegate by lazy {
        ClashServiceDelegate(
            this, clashManager, profilesStore, appSettingsStorage,
            ServiceNotificationManager.VPN_CONFIG
        )
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var tunFd: Int? = null
    private var httpProxyAddress: InetSocketAddress? = null

    override fun onCreate() {
        super.onCreate()
        delegate.initialize()
    }

    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            ServiceNotificationManager.VPN_CONFIG.notificationId,
            delegate.notificationManager.create("正在连接...", "正在建立连接", false)
        )

        val action = intent?.action
        if (action != null) {
            when (action) {
                ACTION_START -> {
                    val profileId = intent.getStringExtra(EXTRA_PROFILE_ID)
                    if (profileId != null) {
                        startVpn(profileId)
                    } else {
                        Timber.tag(TAG).e("未提供配置文件 ID")
                        stopSelf()
                    }
                }
                ACTION_STOP -> stopVpn()
                else -> stopSelf()
            }
        } else {
            Timber.tag(TAG).d("Service restarted by system (START_STICKY)")
            if (appSettingsStorage.automaticRestart.value) {
                val profileId = profilesStore.lastUsedProfileId
                if (profileId.isNotEmpty()) {
                    Timber.tag(TAG).d("Recovering profile: $profileId")
                    startVpn(profileId)
                } else {
                    Timber.tag(TAG).w("No last used profile found for recovery")
                    stopSelf()
                }
            } else {
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun startVpn(profileId: String) {
        delegate.serviceScope.launch {
            try {
                val startTime = System.currentTimeMillis()

                val profile = delegate.loadProfileIfNeeded(
                    profileId = profileId,
                    willUseTunMode = true,
                    quickStart = true
                ).getOrElse { error ->
                    Timber.tag(TAG).e("配置加载失败: ${error.message}")
                    delegate.showErrorNotification("启动失败", error.message ?: "配置加载失败")
                    return@launch
                }
                
                val loadTime = System.currentTimeMillis() - startTime
                Timber.tag(TAG).d("配置加载完成: ${loadTime}ms")

                vpnInterface = withContext(Dispatchers.IO) { establishVpnInterface() }
                    ?: run {
                        Timber.tag(TAG).e("VPN 接口建立失败")
                        delegate.showErrorNotification("启动失败", "无法建立 VPN 连接")
                        return@launch
                    }

                val pfd = vpnInterface!!
                val rawFd = pfd.detachFd()
                tunFd = rawFd

                runCatching { pfd.close() }
                vpnInterface = null

                val config = Configuration.TunConfig()
                val tunDns = if (networkSettings.dnsHijack.value) config.dns else "0.0.0.0"
                val tunConfig = config.copy(
                    stack = networkSettings.tunStack.value.name.lowercase(),
                    dns = tunDns,
                    dnsHijacking = networkSettings.dnsHijack.value
                )

                clashManager.startTunMode(
                    fd = rawFd,
                    config = tunConfig,
                    markSocket = { protect(it) },
                    querySocketUid = ::querySocketUid
                )

                val totalTime = System.currentTimeMillis() - startTime
                Timber.tag(TAG).d("VPN 启动完成: ${totalTime}ms")
                
                delegate.startNotificationUpdate()
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "VPN 启动失败")
                delegate.showErrorNotification("启动失败", e.message ?: "未知错误")
            }
        }
    }

    private fun listenHttp(): InetSocketAddress? {
        val r = { 1 + random.nextInt(199) }
        val listenAt = "127.${r()}.${r()}.${r()}:0"
        val address = Clash.startHttp(listenAt)
        return address?.let { parseInetSocketAddress(it) }
    }
    
    private fun parseInetSocketAddress(address: String): InetSocketAddress {
        val lastColon = address.lastIndexOf(':')
        val host = address.substring(0, lastColon)
        val port = address.substring(lastColon + 1).toInt()
        return InetSocketAddress(host, port)
    }
    private fun querySocketUid(protocol: Int, source: InetSocketAddress, target: InetSocketAddress): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return -1
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return -1
        return runCatching {
            connectivityManager.getConnectionOwnerUid(protocol, source, target)
        }.getOrElse { -1 }
    }

    private fun establishVpnInterface(): ParcelFileDescriptor? = runCatching {
        val config = Configuration.TunConfig()
        Builder().apply {
            setSession("YumeBox VPN")
            setMtu(config.mtu)
            setBlocking(false)
            addAddress(config.gateway, 30)

            if (networkSettings.enableIPv6.value) {
                addAddress(RouteConfig.TUN_GATEWAY6, RouteConfig.TUN_SUBNET_PREFIX6)
            }

            if (networkSettings.bypassPrivateNetwork.value) {
                RouteConfig.BYPASS_PRIVATE_ROUTES.forEach { cidr ->
                    val (addr, prefix) = RouteConfig.parseCidr(cidr)
                    addRoute(addr, prefix)
                }
                if (networkSettings.enableIPv6.value) {
                    RouteConfig.BYPASS_PRIVATE_ROUTES_V6.forEach { cidr ->
                        val (addr, prefix) = RouteConfig.parseCidr(cidr)
                        addRoute(addr, prefix)
                    }
                }
                addRoute(config.dns, 32)
                if (networkSettings.enableIPv6.value) addRoute(RouteConfig.TUN_DNS6, 128)
            } else {
                addRoute("0.0.0.0", 0)
                if (networkSettings.enableIPv6.value) addRoute("::", 0)
            }

            val accessControlPackages = networkSettings.accessControlPackages.value
            when (networkSettings.accessControlMode.value) {
                AccessControlMode.allow_all -> {}
                AccessControlMode.allow_selected -> {
                    (accessControlPackages + packageName).forEach { runCatching { addAllowedApplication(it) } }
                }
                AccessControlMode.reject_selected -> {
                    (accessControlPackages - packageName).forEach { runCatching { addDisallowedApplication(it) } }
                }
            }

            addDnsServer(config.dns)
            if (networkSettings.enableIPv6.value) addDnsServer(RouteConfig.TUN_DNS6)
            if (networkSettings.allowBypass.value) allowBypass()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setMetered(false)
                
                if (networkSettings.systemProxy.value) {
                    listenHttp()?.let { addr ->
                        httpProxyAddress = addr
                        val exclusionList = if (networkSettings.bypassPrivateNetwork.value) {
                            RouteConfig.HTTP_PROXY_LOCAL_LIST + RouteConfig.HTTP_PROXY_BLACK_LIST
                        } else {
                            RouteConfig.HTTP_PROXY_BLACK_LIST
                        }
                        setHttpProxy(
                            ProxyInfo.buildDirectProxy(
                                addr.address.hostAddress,
                                addr.port,
                                exclusionList
                            )
                        )
                        Timber.tag(TAG).d("系统代理已启动: ${addr.address.hostAddress}:${addr.port}")
                    }
                }
            }
        }.establish()
    }.getOrNull()

    private fun stopVpn() {
        delegate.stopNotificationUpdate()

        Clash.stopHttp()
        httpProxyAddress = null

        clashManager.stop()
        tunFd = null
        vpnInterface?.close()
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        delegate.cleanup()
        super.onDestroy()
    }
}
