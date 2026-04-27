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
import com.github.yumelira.yumebox.lite.BuildConfig
import com.github.yumelira.yumebox.common.runtime.StartupGate
import com.github.yumelira.yumebox.core.Global
import com.github.yumelira.yumebox.core.util.StartupTaskCoordinator
import com.github.yumelira.yumebox.core.model.ProxyMode
import com.github.yumelira.yumebox.data.gateway.writeRuntimeLog
import com.github.yumelira.yumebox.data.logging.AppLogBridge
import com.github.yumelira.yumebox.data.logging.AppLogBuffer
import com.github.yumelira.yumebox.data.logging.AppLogTree
import com.github.yumelira.yumebox.data.logging.CrashHandler
import com.github.yumelira.yumebox.data.store.AppSettingsStore
import com.github.yumelira.yumebox.data.store.NetworkSettingsStore
import com.github.yumelira.yumebox.di.featureProxyModules
import com.github.yumelira.yumebox.di.appModule
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import timber.log.Timber

class App : Application() {
    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        if (Timber.forest().isEmpty()) {
            Timber.plant(AppLogTree())
        }
        CrashHandler.init(this)
        AppLogBridge.runtimeLogWriter = ::writeRuntimeLog

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
        scheduleWarmup(koin.get())
    }

    private fun bootstrapDefaults(
        appSettings: AppSettingsStore,
        networkSettings: NetworkSettingsStore,
        networkSettingsStore: MMKV,
    ) {
        AppLogBuffer.minLogLevel = appSettings.logLevel.value
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
