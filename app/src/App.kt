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

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.github.yumelira.yumebox.BuildConfig
import com.github.yumelira.yumebox.common.runtime.StartupGate
import com.github.yumelira.yumebox.common.util.AppLanguageManager
import com.github.yumelira.yumebox.common.util.PredictiveBackCompat
import com.github.yumelira.yumebox.core.FirstRunInitializer
import com.github.yumelira.yumebox.core.Global
import com.github.yumelira.yumebox.core.util.AppForegroundState
import com.github.yumelira.yumebox.core.util.AppScreenState
import com.github.yumelira.yumebox.core.util.AutoStartSessionGate
import com.github.yumelira.yumebox.core.util.StartupTaskCoordinator
import com.github.yumelira.yumebox.core.util.runtimeHomeDir
import com.github.yumelira.yumebox.data.controller.AppIdentityResolver
import com.github.yumelira.yumebox.data.controller.AppTrafficStatisticsCollector
import com.github.yumelira.yumebox.data.gateway.writeRuntimeLog
import com.github.yumelira.yumebox.data.logging.AppLogBridge
import com.github.yumelira.yumebox.data.logging.AppLogBuffer
import com.github.yumelira.yumebox.data.logging.AppLogTree
import com.github.yumelira.yumebox.data.logging.CrashHandler
import com.github.yumelira.yumebox.data.store.AppSettingsStore
import com.github.yumelira.yumebox.data.store.FeatureStore
import com.github.yumelira.yumebox.di.appModule
import com.github.yumelira.yumebox.runtime.api.service.common.constants.Components
import com.github.yumelira.yumebox.runtime.api.service.common.constants.Intents
import com.github.yumelira.yumebox.runtime.client.common.util.ProxyAutoStartHelper
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
import com.tencent.mmkv.MMKV
import java.io.File
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.Koin
import org.koin.core.qualifier.named
import org.tukaani.xz.XZInputStream
import timber.log.Timber

class App : Application() {
    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        lateinit var instance: App
            private set
        private const val GEO_EXTRACT_MAX_FAILURES = 3
    }

    override fun onCreate() {
        super.onCreate()

        instance = this
        AppScreenState.init(this)
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) = AppForegroundState.onActivityResumed()
            override fun onActivityPaused(activity: Activity) = AppForegroundState.onActivityPaused()
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
        try {
            if (Timber.forest().isEmpty()) Timber.plant(AppLogTree())
            CrashHandler.init(this)
            AppLogBridge.runtimeLogWriter = ::writeRuntimeLog
            Global.init(this)
            Components.register(
                mainActivityClassName = MainActivity::class.java.name,
                proxySheetActivityClassName = ProxySheetActivity::class.java.name,
            )
            MMKV.disableProcessModeChecker()
            MMKV.initialize(this)
            Timber.d("System initialization completed")
            val koinApp = startKoin {
                androidContext(this@App)
                modules(appModule)
            }
            val appSettingsStorage: AppSettingsStore = koinApp.koin.get()
            AppLogBuffer.minLogLevel = appSettingsStorage.logLevel.value
            AppLanguageManager.apply(appSettingsStorage.appLanguage.value)
            PredictiveBackCompat.apply(applicationInfo, appSettingsStorage.predictiveBackEnabled.value)
            val featureStore: FeatureStore = koinApp.koin.get()
            featureStore.syncAppVersion(BuildConfig.VERSION_CODE)
            scheduleDeferredStartupTasks(koinApp.koin, featureStore)
        } catch (e: Exception) {
            Timber.e(e, "Fatal error during application startup")
            CrashHandler.init(this)
        }
        startupScope.launch { runCatching { StartupGate.verify(this@App) } }
        startupScope.launch { observeAndBroadcastForegroundState() }
    }

    private suspend fun observeAndBroadcastForegroundState() {
        AppForegroundState.foreground
            .collect { isForeground ->
                runCatching {
                    val intent = android.content.Intent(
                        Intents.actionAppForeground(packageName)
                    ).apply {
                        setPackage(packageName)
                        putExtra(Intents.EXTRA_APP_FOREGROUND, isForeground)
                    }
                    sendBroadcast(intent)
                }
            }
    }

    private fun scheduleDeferredStartupTasks(koin: Koin, featureStore: FeatureStore) {
        StartupTaskCoordinator.startRuntimeWarmup(startupScope) {
            withContext(Dispatchers.IO) {
                ensureGeoAssetsPrepared()
            }
            runCatching { koin.get<AppTrafficStatisticsCollector>() }
                .onFailure { Timber.w(it, "App traffic collector init skipped") }

            runCatching { koin.get<ProxyFacade>().awaitProxyGroupWarmUp() }
                .onFailure { Timber.w(it, "Proxy preview warm-up skipped") }

            if (featureStore.isFirstTimeOpen()) {
                withContext(Dispatchers.IO) {
                    runCatching {
                        koin.getAll<FirstRunInitializer>().forEach { it.initialize() }
                        featureStore.markFirstOpenHandled()
                    }.onFailure { error ->
                        Timber.w(error, "First-open asset initialization failed")
                    }
                }
            }
        }
        startupScope.launch {
            StartupTaskCoordinator.awaitRuntimeWarmup()
            if (!AutoStartSessionGate.tryBeginForegroundAutoActions()) return@launch
            var handled = false
            try {
                ProxyAutoStartHelper.checkAndAutoStart(
                    context = this@App,
                    featureStore = featureStore,
                    proxyFacade = koin.get(),
                    profilesRepository = koin.get(),
                    appSettingsStorage = koin.get(),
                    networkSettingsStorage = koin.get(),
                    serviceCache = koin.get(qualifier = named("service_cache")),
                )
                handled = true
            } finally {
                AutoStartSessionGate.finishForegroundAutoActions(markHandled = handled)
            }
        }
    }

    override fun onTerminate() {
        runCatching {
            val koin = GlobalContext.getOrNull()
            koin?.getOrNull<ProxyFacade>()?.shutdown()
            koin?.getOrNull<AppIdentityResolver>()?.close()
        }
        runCatching { startupScope.cancel() }
        super.onTerminate()
    }

    private fun ensureGeoAssetsPrepared() {
        val homeDir = runtimeHomeDir.apply { mkdirs() }
        val mmkv = MMKV.mmkvWithID("geo_extract_state")
        val geoNames = listOf("geoip.metadb", "geosite.dat", "ASN.mmdb")
        geoNames.forEach { name ->
            val target = File(homeDir, name)
            if (target.exists() && target.length() > 0L) return@forEach
            val failureKey = "fail_count_$name"
            val failCount = mmkv.getInt(failureKey, 0)
            if (failCount >= GEO_EXTRACT_MAX_FAILURES) {
                Timber.w(
                    "Geo asset %s.xz skipped: failed %d times previously",
                    name, failCount,
                )
                return@forEach
            }
            val prepared = extractXzAsset("$name.xz", target)
            if (prepared) {
                mmkv.removeValueForKey(failureKey)
            } else {
                val newCount = failCount + 1
                mmkv.putInt(failureKey, newCount)
                Timber.w("Geo asset decompress failed: %s.xz (attempt %d/%d)", name, newCount, GEO_EXTRACT_MAX_FAILURES)
            }
        }
    }

    private fun extractXzAsset(assetName: String, target: File): Boolean {
        return runCatching {
            target.parentFile?.mkdirs()
            assets.open(assetName).use { input ->
                XZInputStream(input.buffered()).use { xz ->
                    target.outputStream().buffered().use { output ->
                        xz.copyTo(output)
                    }
                }
            }
            true
        }.getOrElse { error ->
            runCatching { if (target.exists()) target.delete() }
            Timber.w(error, "Geo asset extract failed: %s", assetName)
            false
        }
    }
}
