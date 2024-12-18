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
import com.github.yumelira.yumebox.domain.facade.ProfilesRepository
import com.github.yumelira.yumebox.domain.facade.ProxyFacade
import com.github.yumelira.yumebox.data.store.NetworkSettingsStorage
import com.github.yumelira.yumebox.data.store.Preference
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
    storage: NetworkSettingsStorage,
    private val profilesRepository: ProfilesRepository,
    private val proxyFacade: ProxyFacade,
) : AndroidViewModel(application) {

    private var restartJob: Job? = null

    val proxyMode: Preference<ProxyMode> = storage.proxyMode
    val bypassPrivateNetwork: Preference<Boolean> = storage.bypassPrivateNetwork
    val dnsHijack: Preference<Boolean> = storage.dnsHijack
    val allowBypass: Preference<Boolean> = storage.allowBypass
    val enableIPv6: Preference<Boolean> = storage.enableIPv6
    val systemProxy: Preference<Boolean> = storage.systemProxy
    val tunStack: Preference<TunStack> = storage.tunStack
    val accessControlMode: Preference<AccessControlMode> = storage.accessControlMode


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


    fun startService(mode: ProxyMode) {
        viewModelScope.launch {
            try {
                val activeProfile = profilesRepository.queryActiveProfile()
                if (activeProfile == null) {
                    // TODO: show error
                    return@launch
                }
                
                // 停止现有服务并启动新服务
                if (proxyFacade.isRunning.value) {
                    proxyFacade.stopProxy()
                    delay(500)
                }
                
                val useTun = mode == ProxyMode.Tun
                proxyFacade.startProxy(useTun)
            } catch (e: Exception) {
                // TODO: handle error
            }
        }
    }

    fun restartService() {
        viewModelScope.launch {
            try {
                if (!proxyFacade.isRunning.value) return@launch
                
                val activeProfile = profilesRepository.queryActiveProfile()
                if (activeProfile == null) {
                    // TODO: show error
                    return@launch
                }
                
                // 停止并重启服务
                proxyFacade.stopProxy()
                delay(500)
                
                val useTun = proxyMode.value == ProxyMode.Tun
                proxyFacade.startProxy(useTun)
            } catch (e: Exception) {
                // TODO: handle error
            }
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

}

data class NetworkSettingsUiState(
    val serviceState: ServiceState = ServiceState.Stopped,
    val currentProxyMode: ProxyMode = ProxyMode.Tun,
    val needsRestart: Boolean = false
)

enum class ServiceState {
    Running, Stopped
}
