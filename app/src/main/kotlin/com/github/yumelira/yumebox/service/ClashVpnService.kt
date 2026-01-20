package com.github.yumelira.yumebox.service

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.ProxyInfo
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import com.github.yumelira.yumebox.clash.config.Configuration
import com.github.yumelira.yumebox.clash.config.RouteConfig
import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.data.model.AccessControlMode
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import com.github.yumelira.yumebox.data.store.NetworkSettingsStorage
import com.github.yumelira.yumebox.data.store.ProfilesStore
import com.github.yumelira.yumebox.service.notification.ServiceNotificationManager
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import java.net.InetSocketAddress
import java.security.SecureRandom
import timber.log.Timber

class ClashVpnService : VpnService() {

    companion object {
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        const val EXTRA_PROFILE_ID = "profile_id"

        private val random = SecureRandom()
        private const val TAG = "ClashVpnService"

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
    }

    private val clashManager: ClashManager by inject()
    private val profilesStore: ProfilesStore by inject()
    private val appSettingsStorage: AppSettingsStorage by inject()
    private val networkSettings: NetworkSettingsStorage by inject()

    private val notificationManager by lazy {
        ServiceNotificationManager(this, ServiceNotificationManager.VPN_CONFIG)
    }

    private var notificationJob: Job? = null
    private var serviceScope: CoroutineScope? = null
    private var appListModule: com.github.yumelira.yumebox.service.AppListCacheModule? = null

    private var vpnInterface: ParcelFileDescriptor? = null
    private var tunFd: Int? = null
    private var httpProxyAddress: InetSocketAddress? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager.createChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            ServiceNotificationManager.VPN_CONFIG.notificationId,
            notificationManager.create("正在连接...", "正在建立连接", false)
        )

        when (intent?.action) {
            ACTION_START -> {
                val profileId = intent.getStringExtra(EXTRA_PROFILE_ID)
                if (profileId != null) {
                    startVpn(profileId)
                } else {
                    Timber.tag(TAG).e("配置文件不存在")
                    stopSelf()
                }
            }

            ACTION_STOP -> stopVpn()
            else -> stopSelf()
        }

        return START_STICKY
    }

    private fun startVpn(profileId: String) {
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        serviceScope?.launch {
            try {
                // Resolve profile quickly
                val profile = profilesStore.getAllProfiles().find { it.id == profileId }
                    ?: throw IllegalStateException("配置文件不存在")
                // --- Load profile with timeout and retry ---
                val loadTimeout = 60_000L
                var loadAttempt = 0
                val maxLoadAttempts = 2
                var loadSuccess = false
                var lastLoadError: Throwable? = null
                while (loadAttempt < maxLoadAttempts && !loadSuccess) {
                    loadAttempt++
                    Timber.tag(TAG).d("Loading profile '${'$'}{profile.name}' (attempt #${'$'}loadAttempt)...")
                    try {
                        withTimeout(loadTimeout) {
                            val loadResult = clashManager.loadProfile(profile)
                            if (loadResult.isFailure) {
                                throw loadResult.exceptionOrNull() ?: IllegalStateException("配置加载失败")
                            }
                        }
                        loadSuccess = true
                    } catch (e: Exception) {
                        lastLoadError = e
                        Timber.tag(TAG).w(e, "loadProfile failed on attempt #${'$'}loadAttempt")
                        if (loadAttempt < maxLoadAttempts) delay(2000L * loadAttempt)
                    }
                }
                if (!loadSuccess) {
                    val msg = lastLoadError?.message ?: "配置加载失败"
                    throw IllegalStateException(msg)
                }
                // --- Establish VPN interface with separate timeout ---
                Timber.tag(TAG).d("Establishing VPN interface...")
                vpnInterface = try {
                    withTimeout(60_000L) { establishVpnInterface() ?: throw IllegalStateException("establishVpnInterface failed") }
                } catch (e: Exception) {
                    Timber.tag(TAG).w(e, "establishVpnInterface failed")
                    throw e
                }
                val pfd = vpnInterface!!
                val rawFd = pfd.detachFd()
                runCatching { pfd.close() }
                vpnInterface = null
                val config = Configuration.TunConfig()
                val tunDns = if (networkSettings.dnsHijack.value) config.dns else "0.0.0.0"
                val tunConfig = config.copy(
                    stack = networkSettings.tunStack.value.name.lowercase(),
                    dnsHijacking = networkSettings.dnsHijack.value,
                    dns = tunDns
                )
                // --- Start TUN with timeout and retry ---
                var startAttempt = 0
                val maxStartAttempts = 2
                var startSuccess = false
                var lastStartError: Throwable? = null
                while (startAttempt < maxStartAttempts && !startSuccess) {
                    startAttempt++
                    Timber.tag(TAG).d("Starting TUN (attempt #${'$'}startAttempt)...")
                    try {
                        withTimeout(60_000L) {
                            val startResult = clashManager.startTun(
                                fd = rawFd,
                                config = tunConfig,
                                enableIPv6 = networkSettings.enableIPv6.value,
                                markSocket = { protect(it) },
                                querySocketUid = ::querySocketUid
                            )
                            if (startResult.isFailure) {
                                throw startResult.exceptionOrNull() ?: IllegalStateException("Failed to start TUN")
                            }
                        }
                        startSuccess = true
                    } catch (e: Exception) {
                        lastStartError = e
                        Timber.tag(TAG).w(e, "startTun failed on attempt #${'$'}startAttempt")
                        if (startAttempt < maxStartAttempts) delay(1000L * startAttempt)
                    }
                }
                if (!startSuccess) {
                    val msg = lastStartError?.message ?: "Failed to start TUN"
                    throw IllegalStateException(msg)
                }
                startNotificationUpdate()
                appListModule = com.github.yumelira.yumebox.service.AppListCacheModule(this@ClashVpnService, serviceScope!!).apply { start() }
            } catch (e: TimeoutCancellationException) {
                Timber.tag(TAG).e(e, "VPN startup timed out")
                showErrorNotification("启动失败", "Connection timeout")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "VPN startup failed")
                showErrorNotification("启动失败", e.message ?: "未知错误")
            }
        }
    }

    private fun startNotificationUpdate() {
        notificationJob?.cancel()
        notificationJob = notificationManager.startTrafficUpdate(
            serviceScope!! + Dispatchers.IO, clashManager, appSettingsStorage
        )
    }

    private fun stopNotificationUpdate() {
        notificationJob?.cancel()
        notificationJob = null
    }

    private fun buildErrorNotification(title: String, content: String): Notification {
        return notificationManager.create(title, content, false)
    }

    private fun showNotificationAndStopService(notification: Notification, notificationId: Int) {
        startForeground(notificationId, notification)
        serviceScope?.launch {
            delay(3000)
            stopSelf()
        }
    }

    private fun showErrorNotification(title: String, content: String) {
        val notification = buildErrorNotification(title, content)
        showNotificationAndStopService(notification, ServiceNotificationManager.VPN_CONFIG.notificationId)
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
        val cm = connectivityManager ?: (getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager).also { connectivityManager = it }
        return runCatching {
            cm?.getConnectionOwnerUid(protocol, source, target) ?: -1
        }.getOrElse { -1 }
    }

    private var connectivityManager: ConnectivityManager? = null

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
                AccessControlMode.ALLOW_ALL -> {}
                AccessControlMode.ALLOW_SPECIFIC -> {
                    (accessControlPackages + packageName).forEach { runCatching { addAllowedApplication(it) } }
                }

                AccessControlMode.DENY_SPECIFIC -> {
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
                    }
                }
            }
        }.establish()
    }.getOrNull()

    private fun stopVpn() {
        stopNotificationUpdate()
        appListModule?.stop()
        appListModule = null

        clashManager.stop()

        vpnInterface?.close()
        vpnInterface = null
        tunFd = null

        httpProxyAddress = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        serviceScope?.cancel()
        serviceScope = null
        stopVpn()
        super.onDestroy()
    }
}
