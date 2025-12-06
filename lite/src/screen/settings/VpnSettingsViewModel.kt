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

package com.github.yumelira.yumebox.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.data.controller.AppSettingsController
import com.github.yumelira.yumebox.data.controller.NetworkSettingsController
import com.github.yumelira.yumebox.data.model.AppLanguage
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.github.yumelira.yumebox.data.model.TunStack
import com.github.yumelira.yumebox.data.store.AppSettingsStore
import com.github.yumelira.yumebox.data.store.NetworkSettingsStore
import com.github.yumelira.yumebox.data.store.Preference
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
import com.github.yumelira.yumebox.runtime.client.RuntimeStateMapper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class VpnSettingsViewModel(
    private val appSettingsStore: AppSettingsStore,
    private val networkSettingsStore: NetworkSettingsStore,
    private val appSettingsController: AppSettingsController,
    private val networkSettingsController: NetworkSettingsController,
    private val proxyFacade: ProxyFacade,
) : ViewModel() {
    val themeMode: Preference<ThemeMode> = appSettingsStore.themeMode
    val appLanguage: Preference<AppLanguage> = appSettingsStore.appLanguage
    val dnsHijack: Preference<Boolean> = networkSettingsStore.dnsHijack
    val allowBypass: Preference<Boolean> = networkSettingsStore.allowBypass
    val enableIPv6: Preference<Boolean> = networkSettingsStore.enableIPv6
    val systemProxy: Preference<Boolean> = networkSettingsStore.systemProxy
    val tunStack: Preference<TunStack> = networkSettingsStore.tunStack
    val isRunning: StateFlow<Boolean> = proxyFacade.runtimeSnapshot
        .map(RuntimeStateMapper::isActuallyRunning)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            RuntimeStateMapper.isActuallyRunning(proxyFacade.runtimeSnapshot.value),
        )

    fun onDnsHijackChange(enabled: Boolean) {
        networkSettingsController.setAndRestartIfNeeded(dnsHijack, enabled)
    }

    fun onThemeModeChange(mode: ThemeMode) {
        themeMode.set(mode)
    }

    fun onAppLanguageChange(language: AppLanguage) {
        appSettingsController.applyAppLanguage(language)
    }

    fun onAllowBypassChange(enabled: Boolean) {
        networkSettingsController.setAndRestartIfNeeded(allowBypass, enabled)
    }

    fun onEnableIPv6Change(enabled: Boolean) {
        networkSettingsController.setAndRestartIfNeeded(enableIPv6, enabled)
    }

    fun onSystemProxyChange(enabled: Boolean) {
        networkSettingsController.setAndRestartIfNeeded(systemProxy, enabled)
    }

    fun onTunStackChange(stack: TunStack) {
        networkSettingsController.setAndRestartIfNeeded(tunStack, stack)
    }
}
