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

package com.github.yumelira.yumebox.di

import com.github.yumelira.yumebox.data.controller.AccessControlCommandExecutor
import com.github.yumelira.yumebox.data.controller.AccessControlController
import com.github.yumelira.yumebox.data.controller.AppSettingsController
import com.github.yumelira.yumebox.data.controller.NetworkSettingsCommandExecutor
import com.github.yumelira.yumebox.data.controller.NetworkSettingsController
import com.github.yumelira.yumebox.data.controller.RuntimeOverrideController
import com.github.yumelira.yumebox.data.controller.AppIdentityResolver
import com.github.yumelira.yumebox.data.controller.AppTrafficStatisticsCollector
import com.github.yumelira.yumebox.data.gateway.LogRecordGateway
import com.github.yumelira.yumebox.data.store.LogStore
import com.github.yumelira.yumebox.data.gateway.NetworkInfoService
import com.github.yumelira.yumebox.data.store.OverrideConfigStore
import com.github.yumelira.yumebox.data.store.ProfileBindingProvider
import com.github.yumelira.yumebox.data.store.ProfileBindingStore
import com.github.yumelira.yumebox.data.controller.ProvidersController
import com.github.yumelira.yumebox.data.store.AppSettingsStore
import com.github.yumelira.yumebox.data.store.AppStateManager
import com.github.yumelira.yumebox.data.store.FeatureStore
import com.github.yumelira.yumebox.data.store.MMKVProvider
import com.github.yumelira.yumebox.data.store.NetworkSettingsStore
import com.github.yumelira.yumebox.data.store.ProfileLinksStore
import com.github.yumelira.yumebox.data.store.ProxyDisplaySettingsStore
import com.github.yumelira.yumebox.data.store.TrafficStatisticsStore
import com.github.yumelira.yumebox.lite.config.TunProfileSync
import com.github.yumelira.yumebox.core.model.ProxyMode
import com.github.yumelira.yumebox.screen.home.HomeViewModel
import com.github.yumelira.yumebox.screen.importconfig.ImportConfigViewModel
import com.github.yumelira.yumebox.screen.log.LogViewModel
import com.github.yumelira.yumebox.screen.settings.AccessControlViewModel
import com.github.yumelira.yumebox.screen.settings.VpnSettingsViewModel
import com.github.yumelira.yumebox.core.domain.model.TrafficData
import com.github.yumelira.yumebox.runtime.client.ProfilesRepository
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
import com.github.yumelira.yumebox.runtime.client.RuntimeStateMapper
import com.github.yumelira.yumebox.data.gateway.createLogRecordGateway
import com.github.yumelira.yumebox.common.APPLICATION_SCOPE_NAME
import com.github.yumelira.yumebox.common.util.AppLanguageManager
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val appFoundationModule = module {
    single<CoroutineScope>(named(APPLICATION_SCOPE_NAME)) {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    single { MMKVProvider() }
    single<MMKV>(named("profiles")) { get<MMKVProvider>().getMMKV("profiles") }
    single<MMKV>(named("settings")) { get<MMKVProvider>().getMMKV("settings") }
    single<MMKV>(named("network_settings")) { get<MMKVProvider>().getMMKV("network_settings") }
    single<MMKV>(named("substore")) { get<MMKVProvider>().getMMKV("substore") }
    single<MMKV>(named("proxy_display")) { get<MMKVProvider>().getMMKV("proxy_display") }
    single<MMKV>(named("traffic_statistics")) { get<MMKVProvider>().getMMKV("traffic_statistics") }
    single<MMKV>(named("profile_links")) { get<MMKVProvider>().getMMKV("profile_links") }
    single { AppSettingsStore(get(named("settings"))) }
    single { NetworkSettingsStore(get(named("network_settings"))) }
    single { ProfileLinksStore(get(named("profile_links"))) }
    single { FeatureStore(get(named("substore"))) }
    single { ProxyDisplaySettingsStore(get(named("proxy_display"))) }
    single { TrafficStatisticsStore(get(named("traffic_statistics"))) }
    single {
        AppStateManager(
            appSettingsStore = get(),
            networkSettingsStore = get(),
            featureStore = get(),
            profileLinksStore = get(),
            proxyDisplaySettingsStore = get(),
            trafficStatisticsStore = get(),
        )
    }
}

private val appDataRuntimeModule = module {
    single { AppSettingsController(get(), applyLanguage = AppLanguageManager::apply) }
    single {
        val proxyFacade = get<ProxyFacade>()
        val tunProfileSync = get<TunProfileSync>()
        NetworkSettingsCommandExecutor(
            store = get(),
            restartProxy = { mode -> proxyFacade.startProxy(mode) },
            beforeRestart = { targetMode ->
                if (targetMode == ProxyMode.Tun) {
                    tunProfileSync.syncActiveProfile()
                }
            },
        )
    }
    single {
        val proxyFacade = get<ProxyFacade>()
        NetworkSettingsController(
            store = get(),
            isRunning = { RuntimeStateMapper.isActuallyRunning(proxyFacade.runtimeSnapshot.value) },
            commandExecutor = get(),
        )
    }
    single {
        val proxyFacade = get<ProxyFacade>()
        AccessControlController(
            store = get(),
            isRunning = { proxyFacade.isRunning.value },
            resolveActiveMode = { RuntimeStateMapper.modeForOwner(proxyFacade.runtimeSnapshot.value.owner) },
            commandExecutor = get(),
        )
    }
    single {
        val proxyFacade = get<ProxyFacade>()
        val tunProfileSync = get<TunProfileSync>()
        AccessControlCommandExecutor(
            restartProxy = { mode -> proxyFacade.startProxy(mode) },
            beforeRestart = { targetMode ->
                if (targetMode == ProxyMode.Tun) {
                    tunProfileSync.syncActiveProfile()
                }
            },
        )
    }
    single<LogRecordGateway> { createLogRecordGateway() }
    single { LogStore(androidApplication(), get()) }
    single { NetworkInfoService() }
    single {
        val appContext = androidContext()
        ProvidersController(appContext) {
            com.github.yumelira.yumebox.runtime.client.remote.ServiceClient.connect(appContext)
            com.github.yumelira.yumebox.runtime.client.remote.ServiceClient.clash().queryProviders()
        }
    }
    single {
        val profilesRepository = get<ProfilesRepository>()
        RuntimeOverrideController(
            configStore = get(),
            queryActiveProfile = { profilesRepository.queryActiveProfile() },
        )
    }

    single { ProfileBindingStore(androidContext()) }
    single<ProfileBindingProvider> { get<ProfileBindingStore>() }
    single { OverrideConfigStore(androidContext(), get()) }

    single { TunProfileSync(androidContext(), get(), get()) }

    single { ProxyFacade(androidContext(), get()) }
    single { AppIdentityResolver(androidContext()) }
    single { ProfilesRepository(androidContext()) }
    single {
        val proxyFacade = get<ProxyFacade>()
        AppTrafficStatisticsCollector(
            isRunningFlow = proxyFacade.isRunning,
            currentProfileId = { proxyFacade.currentProfile.value?.uuid?.toString() },
            trafficStatisticsStore = get(),
            appIdentityResolver = get(),
            queryTrafficTotal = {
                if (proxyFacade.runtimeSnapshot.value.running)
                    TrafficData.from(proxyFacade.trafficTotal.value)
                else TrafficData.ZERO
            },
            connectionSnapshotFlow = proxyFacade.connectionSnapshot,
            queryActiveProfileId = {
                proxyFacade.refreshCurrentProfile()
                proxyFacade.currentProfile.value?.uuid?.toString()
            },
        )
    }
}

private val appViewModelModule = module {
    viewModel { HomeViewModel(androidApplication(), get(), get(), get(), get()) }
    viewModel { ImportConfigViewModel(androidApplication(), get(), get(), get()) }
    viewModel { VpnSettingsViewModel(get(), get(), get(), get()) }
    viewModel { AccessControlViewModel(androidApplication(), get(), get(), get()) }
    viewModel { LogViewModel(get()) }
}

val appModule: List<Module> = listOf(
    appFoundationModule,
    appDataRuntimeModule,
    appViewModelModule,
)
