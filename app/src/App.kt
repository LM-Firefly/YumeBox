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
 * Copyright (c)  YumeLira 2025 - Present
 *
 */



package com.github.yumelira.yumebox

import android.app.Application
import android.content.res.Configuration
import com.github.yumelira.yumebox.common.runtime.StartupGate
import com.github.yumelira.yumebox.common.util.AppLanguageManager
import com.github.yumelira.yumebox.common.util.PlatformIdentifier
import com.github.yumelira.yumebox.core.Global
import com.github.yumelira.yumebox.core.util.StartupTaskCoordinator
import com.github.yumelira.yumebox.core.util.runtimeHomeDir
import com.github.yumelira.yumebox.data.controller.AppTrafficStatisticsCollector
import com.github.yumelira.yumebox.data.model.AppLogBuffer
import com.github.yumelira.yumebox.data.model.AppLogBridge
import com.github.yumelira.yumebox.data.model.AppLogTree
import com.github.yumelira.yumebox.data.model.CrashHandler
import com.github.yumelira.yumebox.data.store.AppSettingsStore
import com.github.yumelira.yumebox.data.store.FeatureStore
import com.github.yumelira.yumebox.di.appModule
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
import com.github.yumelira.yumebox.service.LogRecordService
import com.github.yumelira.yumebox.substore.util.AppUtil
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.Koin
import org.tukaani.xz.XZInputStream
import timber.log.Timber
import java.io.File
import java.io.IOException

class App : Application() {
    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()

        instance = this
        if (Timber.forest().isEmpty()) {
            Timber.plant(AppLogTree())
        }
        CrashHandler.init(this)
        AppLogBridge.runtimeLogWriter = { line -> LogRecordService.writeLog(line) }

        StartupGate.verify(this)
        Global.init(this)
        MMKV.disableProcessModeChecker()
        MMKV.initialize(this)

        val koinApp = startKoin {
            androidContext(this@App)
            modules(appModule)
        }
        val appSettingsStorage: AppSettingsStore = koinApp.koin.get()
        AppLogBuffer.minLogLevel = appSettingsStorage.logLevel.value
        AppLanguageManager.apply(appSettingsStorage.appLanguage.value)

        extractGeoFiles()
        val featureStore: FeatureStore = koinApp.koin.get()
        featureStore.syncAppVersion(BuildConfig.VERSION_CODE)
        scheduleDeferredStartupTasks(koinApp.koin, featureStore)

        PlatformIdentifier.getPlatformIdentifier()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        AppLanguageManager.refreshSystemLanguage()
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

    private fun extractCompressedAssetIfExists(assetName: String, targetFile: File): Boolean {
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

    private fun scheduleDeferredStartupTasks(koin: Koin, featureStore: FeatureStore) {
        StartupTaskCoordinator.startRuntimeWarmup(startupScope) {
            runCatching { koin.get<AppTrafficStatisticsCollector>() }
                .onFailure { Timber.w(it, "App traffic collector init skipped") }

            runCatching { koin.get<ProxyFacade>().awaitProxyGroupWarmUp() }
                .onFailure { Timber.w(it, "Proxy preview warm-up skipped") }

            if (featureStore.isFirstTimeOpen()) {
                withContext(Dispatchers.IO) {
                    runCatching {
                        AppUtil.initFirstOpen()
                        featureStore.markFirstOpenHandled()
                    }.onFailure { error ->
                        Timber.w(error, "First-open asset initialization failed")
                    }
                }
            }
        }
    }
}
