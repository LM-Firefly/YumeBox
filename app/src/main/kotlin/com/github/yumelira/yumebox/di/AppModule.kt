package com.github.yumelira.yumebox.di

import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.data.repository.*
import com.github.yumelira.yumebox.data.store.*
import com.github.yumelira.yumebox.domain.facade.ProfilesRepository
import com.github.yumelira.yumebox.domain.facade.ProxyFacade
import com.github.yumelira.yumebox.presentation.viewmodel.*
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

const val APPLICATION_SCOPE_NAME = "applicationScope"

val appModule = module {
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

    single { AppSettingsStorage(get<MMKV>(named("settings"))) }
    single { NetworkSettingsStorage(get(named("network_settings"))) }
    single { ProfileLinksStorage(get(named("profile_links"))) }
    single { FeatureStore(get(named("substore"))) }
    single { ProfilesStore(get(named("profiles")), get<CoroutineScope>(named(APPLICATION_SCOPE_NAME))) }
    single { ProxyDisplaySettingsStore(get(named("proxy_display"))) }
    single { TrafficStatisticsStore(get(named("traffic_statistics"))) }

    single(createdAtStart = false) {
        ClashManager(
            androidContext(),
            androidApplication().filesDir.resolve("clash"),
            proxyModeProvider = { get<ProxyDisplaySettingsStore>().proxyMode.value }
        )
    }

    single { NetworkInfoService() }
    single { ProxyConnectionService(androidContext(), get(), get()) }
    single { ProxyChainResolver() }
    single { TrafficStatisticsCollector(get(), get()) }
    single { SelectionDao(androidContext()) }
    single { OverrideRepository() }
    single { ProvidersRepository() }

    single { ProxyFacade(get(), get(), get()) }
    single { ProfilesRepository(get()) }

    viewModel { AppSettingsViewModel(get()) }
    viewModel { HomeViewModel(androidApplication(), get(), get(), get(), get(), get(), get()) }
    viewModel { ProfilesViewModel(androidApplication(), get(), get()) }
    viewModel { ProxyViewModel(get<ClashManager>(), get(), get()) }
    viewModel { ProvidersViewModel(get(), get()) }
    viewModel { LogViewModel(androidApplication()) }
    viewModel { SettingViewModel(get()) }
    viewModel { FeatureViewModel(get(), androidApplication()) }
    viewModel { NetworkSettingsViewModel(androidApplication(), get(), get(), get(), get()) }
    viewModel { AccessControlViewModel(androidApplication(), get()) }
    viewModel { OverrideViewModel(get(), get(), get(named(APPLICATION_SCOPE_NAME))) }
    viewModel { TrafficStatisticsViewModel(androidApplication(), get()) }
    viewModel { ConnectionsViewModel(get()) }
}
