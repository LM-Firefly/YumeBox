package com.github.yumelira.yumebox

import android.app.Application
import com.github.yumelira.yumebox.lite.BuildConfig
import com.github.yumelira.yumebox.common.runtime.StartupGate
import com.github.yumelira.yumebox.core.Global
import com.github.yumelira.yumebox.core.util.StartupTaskCoordinator
import com.github.yumelira.yumebox.core.util.runtimeHomeDir
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.data.repository.NetworkSettingsRepository
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import com.github.yumelira.yumebox.di.featureProxyModules
import com.github.yumelira.yumebox.di.appModule
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.tukaani.xz.XZInputStream
import timber.log.Timber
import java.io.File
import java.io.IOException

class App : Application() {
    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG && Timber.forest().isEmpty()) {
            Timber.plant(Timber.DebugTree())
        }

        StartupGate.loadPrimary()
        Global.init(this)
        MMKV.initialize(this)

        val koin = startKoin {
            androidContext(this@App)
            modules(appModule + featureProxyModules)
        }.koin

        bootstrapDefaults(
            appSettings = koin.get(),
            networkSettings = koin.get(),
            networkSettingsStore = koin.get(named("network_settings")),
        )
        startupScope.launch(Dispatchers.IO) {
            extractGeoFiles()
        }
        scheduleWarmup(koin.get())
    }

    private fun bootstrapDefaults(
        appSettings: AppSettingsStorage,
        networkSettings: NetworkSettingsRepository,
        networkSettingsStore: MMKV,
    ) {
        networkSettings.proxyMode.set(ProxyMode.Tun)
        if (!networkSettingsStore.containsKey("bypassPrivateNetwork")) {
            networkSettings.bypassPrivateNetwork.set(false)
        }
        if (!networkSettingsStore.containsKey("dnsHijack")) {
            networkSettings.dnsHijack.set(true)
        }
        if (!networkSettingsStore.containsKey("allowBypass")) {
            networkSettings.allowBypass.set(false)
        }

        if (!appSettings.initialSetupCompleted.value) {
            appSettings.initialSetupCompleted.set(true)
            appSettings.privacyPolicyAccepted.set(true)
        }
    }

    private fun extractGeoFiles() {
        val mihomoDir = runtimeHomeDir.apply { mkdirs() }
        val geoFiles = listOf("geoip.metadb", "geosite.dat", "ASN.mmdb")
        val failedFiles = mutableListOf<String>()

        geoFiles.forEach { filename ->
            val targetFile = File(mihomoDir, filename)
            if (!targetFile.exists()) {
                try {
                    if (!extractCompressedAssetIfExists("$filename.xz", targetFile)) {
                        assets.open(filename).use { input ->
                            targetFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                } catch (_: IOException) {
                    failedFiles += filename
                }
            }
        }

        if (failedFiles.isNotEmpty()) {
            Timber.w("Failed to extract geo files: ${failedFiles.joinToString()}")
        }
    }

    private fun extractCompressedAssetIfExists(
        assetName: String,
        targetFile: File,
    ): Boolean {
        return try {
            assets.open(assetName).use { input ->
                XZInputStream(input.buffered()).use { xzInput ->
                    targetFile.outputStream().buffered().use { output ->
                        xzInput.copyTo(output)
                    }
                }
            }
            true
        } catch (_: IOException) {
            false
        }
    }

    private fun scheduleWarmup(proxyFacade: ProxyFacade) {
        StartupTaskCoordinator.startRuntimeWarmup(startupScope) {
            try {
                proxyFacade.awaitProxyGroupWarmUp()
            } catch (error: Exception) {
                Timber.w(error, "Proxy preview warm-up skipped")
            }
        }
    }
}
