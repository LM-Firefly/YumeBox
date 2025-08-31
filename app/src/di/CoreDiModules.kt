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

import com.github.yumelira.yumebox.data.repository.*
import com.github.yumelira.yumebox.data.store.*
import com.github.yumelira.yumebox.runtime.client.ProfilesRepository
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

const val APPLICATION_SCOPE_NAME = "applicationScope"

val appFoundationModule = module {
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
    single<MMKV>(named("service_cache")) { get<MMKVProvider>().getMMKV("service_cache") }
    single<MMKV>(named("override_bindings")) { get<MMKVProvider>().getMMKV("override_bindings") }

    single { AppSettingsStorage(get<MMKV>(named("settings"))) }
    single { NetworkSettingsStorage(get(named("network_settings"))) }
    single { ProfileLinksStorage(get(named("profile_links"))) }
    single { FeatureStore(get(named("substore"))) }
    single { ProxyDisplaySettingsStore(get(named("proxy_display"))) }
    single { TrafficStatisticsStore(get(named("traffic_statistics"))) }
}

val appDataRuntimeModule = module {
    single { AppSettingsRepository(get()) }
    single { NetworkSettingsRepository(get()) }
    single { FeatureSettingsRepository(get()) }
    single { ProxyDisplaySettingsRepository(get()) }
    single { ProfileLinksRepository(get()) }
    single { LogRepository(androidApplication(), get()) }
    single { NetworkInfoService() }
    single { ProxyChainResolver() }
    single { OverrideRepository(androidContext(), get()) }
    single { ProvidersRepository(androidContext()) }

    single { OverrideConfigRepository(androidContext()) }
    single<OverrideConfigProvider> { get<OverrideConfigRepository>() }

    single { ProfileBindingRepository(androidContext()) }
    single<ProfileBindingProvider> { get<ProfileBindingRepository>() }

    single { OverrideResolver(get(), get()) }
    single { OverrideService(androidContext(), get()) }
    single { ActiveProfileOverrideReloader(get(), get(), get()) }

    single { com.github.yumelira.yumebox.remote.ServiceClient }
    single { ProxyFacade(androidContext()) }
    single { ProfilesRepository(androidContext()) }
    single { TrafficStatisticsCollector(get(), get()) }
}

val coreDiModules: List<Module> = listOf(
    appFoundationModule,
    appDataRuntimeModule,
)
