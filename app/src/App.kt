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
import com.github.yumelira.yumebox.BuildConfig
import com.github.yumelira.yumebox.common.runtime.StartupGate
import com.github.yumelira.yumebox.common.util.AppLanguageManager
import com.github.yumelira.yumebox.common.util.PlatformIdentifier
import com.github.yumelira.yumebox.core.Global
import com.github.yumelira.yumebox.core.util.StartupTaskCoordinator
import com.github.yumelira.yumebox.core.util.runtimeHomeDir
import com.github.yumelira.yumebox.data.controller.AppTrafficStatisticsCollector
import com.github.yumelira.yumebox.data.gateway.writeRuntimeLog
import com.github.yumelira.yumebox.data.model.AppLogBuffer
import com.github.yumelira.yumebox.data.model.AppLogBridge
import com.github.yumelira.yumebox.data.model.AppLogTree
import com.github.yumelira.yumebox.data.model.CrashHandler
import com.github.yumelira.yumebox.data.store.AppStateManager
import com.github.yumelira.yumebox.data.store.FeatureStore
import com.github.yumelira.yumebox.di.appModule
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
import com.github.yumelira.yumebox.substore.util.AppUtil
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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
        try {
            runBlocking { executeStartupSequence() }
        } catch (e: Exception) {
            Timber.e(e, "Fatal error during application startup")
            CrashHandler.init(this)
        }
    }

    private suspend fun executeStartupSequence() {
        initSystem()
        initDataStore()
        val koinApp = startKoin {
            androidContext(this@App)
            modules(appModule)
        }
        val appState: AppStateManager = koinApp.koin.get()
        initConfig(appState)
        scheduleDeferredStartupTasks(koinApp.koin, appState.featureStore)
        PlatformIdentifier.getPlatformIdentifier()
    }

    private suspend fun initSystem() = withContext(Dispatchers.IO) {
        if (Timber.forest().isEmpty()) {
            Timber.plant(AppLogTree())
        }
        CrashHandler.init(this@App)
        AppLogBridge.runtimeLogWriter = ::writeRuntimeLog
        StartupGate.verify(this@App)
        Timber.d("System initialization completed")
    }

    private suspend fun initDataStore() = withContext(Dispatchers.IO) {
        Global.init(this@App)
        MMKV.disableProcessModeChecker()
        MMKV.initialize(this@App)
        Timber.d("Data stores initialized")
    }

    private suspend fun initConfig(appState: AppStateManager) = withContext(Dispatchers.IO) {
        AppLanguageManager.apply(appState.appSettingsStore.appLanguage.value)
        AppLogBuffer.minLogLevel = appState.appSettingsStore.logLevel.value
        extractGeoFiles()
        appState.featureStore.syncAppVersion(BuildConfig.VERSION_CODE)
        Timber.d("Application configuration applied")
    }

    private fun extractGeoFiles() {
        val mihomoDir = runtimeHomeDir.apply { mkdirs() }
        val failedFiles = mutableListOf<String>()
        listOf("geoip.metadb", "geosite.dat", "ASN.mmdb").forEach { filename ->
            val targetFile = File(mihomoDir, filename)
            if (!targetFile.exists()) {
                try {
                    if (!extractCompressedAssetIfExists("$filename.xz", targetFile)) {
                        assets.open(filename).use { input ->
                            targetFile.outputStream().use(input::copyTo)
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
                XZInputStream(input.buffered()).use { xz ->
                    targetFile.outputStream().buffered().use(xz::copyTo)
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
                    }.onFailure { Timber.w(it, "First-open initialization failed") }
                }
            }
        }
    }
}
