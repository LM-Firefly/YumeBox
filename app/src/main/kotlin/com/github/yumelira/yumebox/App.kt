package com.github.yumelira.yumebox

import android.app.Application
import com.github.yumelira.yumebox.clash.cleanupOrphanedConfigs
import com.github.yumelira.yumebox.clash.restoreAll
import com.github.yumelira.yumebox.common.native.NativeLibraryManager.initialize
import com.github.yumelira.yumebox.common.util.AppUtil
import com.github.yumelira.yumebox.common.util.PlatformIdentifier
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.Global
import com.github.yumelira.yumebox.data.model.AppLogBuffer
import com.github.yumelira.yumebox.data.model.AppLogTree
import com.github.yumelira.yumebox.data.model.CrashHandler
import com.github.yumelira.yumebox.data.repository.TrafficStatisticsCollector
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import com.github.yumelira.yumebox.data.store.FeatureStore
import com.github.yumelira.yumebox.data.store.ProfilesStore
import com.github.yumelira.yumebox.di.appModule
import com.tencent.mmkv.MMKV
import dev.oom_wg.purejoy.mlang.MLang
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class App : Application() {

    companion object {
        lateinit var instance: App
            private set
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        instance = this
        if (Timber.forest().isEmpty()) {
            Timber.plant(AppLogTree())
        }
        CrashHandler.init(this)

        Global.init(this)
        MMKV.initialize(this)

        val koinApp = startKoin {
            androidContext(this@App)
            modules(appModule)
        }

        extractGeoFiles()

        val featureStore: FeatureStore = koinApp.koin.get()
        koinApp.koin.get<TrafficStatisticsCollector>()

        if (featureStore.isFirstTimeOpen()) {
            AppUtil.initFirstOpen()
            featureStore.markFirstOpenHandled()
        }

        initialize(this)

        // 恢复保存的自定义 User-Agent
        val appSettings: AppSettingsStorage = koinApp.koin.get()
        val savedUserAgent = appSettings.customUserAgent.value
        AppLogBuffer.minLogLevel = appSettings.logLevel.value
        if (savedUserAgent.isNotEmpty()) {
            Clash.setCustomUserAgent(savedUserAgent)
        }

        PlatformIdentifier.getPlatformIdentifier()

        // 清理孤儿配置并恢复自动更新任务
        applicationScope.launch {
            try {
                val profilesStore: ProfilesStore = koinApp.koin.get()
                val profiles = profilesStore.getAllProfiles()
                val clashWorkDir = File(filesDir, "clash")
                cleanupOrphanedConfigs(clashWorkDir, profiles)
            } catch (e: Exception) {
                Timber.e(e, "清理孤儿配置失败")
            }
            // 初始化随机密钥，并恢复所有配置
            runCatching {
                val config = Clash.queryOverride(Clash.OverrideSlot.Persist)
                if (config.secret.isNullOrBlank()) {
                    config.secret = java.util.UUID.randomUUID().toString()
                    Clash.patchOverride(Clash.OverrideSlot.Persist, config)
                    Clash.patchOverride(Clash.OverrideSlot.Session, config)
                }
            }.onFailure {
                Timber.e(it, MLang.Override.Message.InitSecretFailed)
            }
            restoreAll()
        }
    }

    private fun extractGeoFiles() {
        val clashDir = File(filesDir, "clash").apply { mkdirs() }
        val geoFiles = listOf("geoip.metadb", "geosite.dat", "ASN.mmdb")

        geoFiles.forEach { filename ->
            val targetFile = File(clashDir, filename)
            if (!targetFile.exists()) {
                runCatching {
                    assets.open(filename).use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }
}
