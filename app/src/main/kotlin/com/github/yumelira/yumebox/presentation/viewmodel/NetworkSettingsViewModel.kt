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
 * Copyright (c)  YumeLira 2025.
 *
 */

package com.github.yumelira.yumebox.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.data.model.AccessControlMode
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.data.model.TunStack
import com.github.yumelira.yumebox.domain.facade.ProfilesFacade
import com.github.yumelira.yumebox.domain.facade.ProxyFacade
import com.github.yumelira.yumebox.data.store.Preference
import com.github.yumelira.yumebox.domain.facade.SettingsFacade
import com.github.yumelira.yumebox.domain.model.RunningMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NetworkSettingsViewModel(
    application: Application,
    settingsFacade: SettingsFacade,
    private val profilesRepository: ProfilesFacade,
    private val proxyFacade: ProxyFacade,
) : AndroidViewModel(application) {

    private var restartJob: Job? = null

    val proxyMode: Preference<ProxyMode> = settingsFacade.proxyMode
    val bypassPrivateNetwork: Preference<Boolean> = settingsFacade.bypassPrivateNetwork
    val dnsHijack: Preference<Boolean> = settingsFacade.dnsHijack
    val allowBypass: Preference<Boolean> = settingsFacade.allowBypass
    val enableIPv6: Preference<Boolean> = settingsFacade.enableIPv6
    val systemProxy: Preference<Boolean> = settingsFacade.systemProxy
    val tunStack: Preference<TunStack> = settingsFacade.tunStack
    val accessControlMode: Preference<AccessControlMode> = settingsFacade.accessControlMode


    val serviceState: StateFlow<ServiceState> = proxyFacade.isRunning
        .map { running -> if (running) ServiceState.Running else ServiceState.Stopped }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ServiceState.Stopped)

    val currentProxyMode: StateFlow<ProxyMode> = proxyMode.state


    val uiState: StateFlow<NetworkSettingsUiState> = combine(
        serviceState,
        currentProxyMode
    ) { serviceState, proxyMode ->
        NetworkSettingsUiState(
            serviceState = serviceState,
            currentProxyMode = proxyMode,
            needsRestart = serviceState == ServiceState.Running
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NetworkSettingsUiState()
    )


    fun onProxyModeChange(mode: ProxyMode) {
        proxyMode.set(mode)
    }

    fun onBypassPrivateNetworkChange(enabled: Boolean) {
        bypassPrivateNetwork.set(enabled)
        updateServiceConfig()
    }

    fun onDnsHijackChange(enabled: Boolean) {
        dnsHijack.set(enabled)
        updateServiceConfig()
    }

    fun onAllowBypassChange(enabled: Boolean) {
        allowBypass.set(enabled)
        updateServiceConfig()
    }

    fun onEnableIPv6Change(enabled: Boolean) {
        enableIPv6.set(enabled)
        updateServiceConfig()
    }

    fun onSystemProxyChange(enabled: Boolean) {
        systemProxy.set(enabled)
        updateServiceConfig()
    }

    fun onTunStackChange(stack: TunStack) {
        tunStack.set(stack)
        updateServiceConfig()
    }

    fun onAccessControlModeChange(mode: AccessControlMode) {
        accessControlMode.set(mode)
        updateServiceConfig()
    }


    fun startService(proxyMode: ProxyMode) {
        viewModelScope.launch {
            val profileId = resolveProfileId() ?: return@launch
            val runningMode = proxyFacade.runningMode.value
            if (runningMode != RunningMode.None) {
                proxyFacade.stopProxy(runningMode)
            }
            proxyFacade.startDirectProxy(profileId, proxyMode)
        }
    }

    fun restartService() {
        viewModelScope.launch {
            val runningMode = proxyFacade.runningMode.value
            if (runningMode == RunningMode.None) return@launch
            proxyFacade.stopProxy(runningMode)
            val profileId = resolveProfileId() ?: return@launch
            proxyFacade.startDirectProxy(profileId, proxyMode.value)
        }
    }

    private fun updateServiceConfig() {
        restartJob?.cancel()
        restartJob = viewModelScope.launch {
            delay(300)
            if (serviceState.value == ServiceState.Running) {
                restartService()
            }
        }
    }

    private fun resolveProfileId(): String? {
        return profilesRepository.getLastUsedProfileId().takeIf { it.isNotBlank() }
            ?: profilesRepository.getRecommendedProfile()?.id
    }

}

data class NetworkSettingsUiState(
    val serviceState: ServiceState = ServiceState.Stopped,
    val currentProxyMode: ProxyMode = ProxyMode.Tun,
    val needsRestart: Boolean = false
)

enum class ServiceState {
    Running, Stopped
}
