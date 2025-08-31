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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.core.model.RootTunDnsMode
import com.github.yumelira.yumebox.data.model.AccessControlMode
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.data.model.TunStack
import com.github.yumelira.yumebox.data.repository.NetworkSettingsRepository
import com.github.yumelira.yumebox.data.store.Preference
import com.github.yumelira.yumebox.runtime.client.ProfilesRepository
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
import com.github.yumelira.yumebox.runtime.client.RuntimeStateMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NetworkSettingsViewModel(
    application: Application,
    repository: NetworkSettingsRepository,
    private val profilesRepository: ProfilesRepository,
    private val proxyFacade: ProxyFacade,
) : AndroidViewModel(application) {

    private var restartJob: Job? = null

    val proxyMode: Preference<ProxyMode> = repository.proxyMode
    val bypassPrivateNetwork: Preference<Boolean> = repository.bypassPrivateNetwork
    val dnsHijack: Preference<Boolean> = repository.dnsHijack
    val allowBypass: Preference<Boolean> = repository.allowBypass
    val enableIPv6: Preference<Boolean> = repository.enableIPv6
    val systemProxy: Preference<Boolean> = repository.systemProxy
    val tunStack: Preference<TunStack> = repository.tunStack
    val rootTunAutoRoute: Preference<Boolean> = repository.rootTunAutoRoute
    val rootTunStrictRoute: Preference<Boolean> = repository.rootTunStrictRoute
    val rootTunAutoRedirect: Preference<Boolean> = repository.rootTunAutoRedirect
    val rootTunDnsMode: Preference<RootTunDnsMode> = repository.rootTunDnsMode
    val accessControlMode: Preference<AccessControlMode> = repository.accessControlMode

    private val rootTunIfName = repository.rootTunIfName
    private val rootTunMtu = repository.rootTunMtu
    private val rootTunIncludeAndroidUser = repository.rootTunIncludeAndroidUser
    private val rootTunRouteExcludeAddress = repository.rootTunRouteExcludeAddress
    private val rootTunFakeIpRange = repository.rootTunFakeIpRange
    private val rootTunFakeIpRange6 = repository.rootTunFakeIpRange6

    private val _rootTunIfNameDraft = MutableStateFlow(rootTunIfName.value)
    val rootTunIfNameDraft: StateFlow<String> = _rootTunIfNameDraft.asStateFlow()

    private val _rootTunMtuDraft = MutableStateFlow(rootTunMtu.value.toString())
    val rootTunMtuDraft: StateFlow<String> = _rootTunMtuDraft.asStateFlow()

    private val _rootTunIncludeAndroidUserDraft = MutableStateFlow(
        rootTunIncludeAndroidUser.value.joinToString(", ")
    )
    val rootTunIncludeAndroidUserDraft: StateFlow<String> = _rootTunIncludeAndroidUserDraft.asStateFlow()

    private val _rootTunRouteExcludeAddressDraft = MutableStateFlow(
        rootTunRouteExcludeAddress.value.joinToString("\n")
    )
    val rootTunRouteExcludeAddressDraft: StateFlow<String> = _rootTunRouteExcludeAddressDraft.asStateFlow()

    private val _rootTunFakeIpRangeDraft = MutableStateFlow(rootTunFakeIpRange.value)
    val rootTunFakeIpRangeDraft: StateFlow<String> = _rootTunFakeIpRangeDraft.asStateFlow()

    private val _rootTunFakeIpRange6Draft = MutableStateFlow(rootTunFakeIpRange6.value)
    val rootTunFakeIpRange6Draft: StateFlow<String> = _rootTunFakeIpRange6Draft.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private val runtimeSnapshot = proxyFacade.runtimeSnapshot

    val serviceState: StateFlow<ServiceState> = runtimeSnapshot
        .map { snapshot -> if (RuntimeStateMapper.isActuallyRunning(snapshot)) ServiceState.Running else ServiceState.Stopped }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ServiceState.Stopped)

    val currentProxyMode: StateFlow<ProxyMode> = proxyMode.state

    val uiState: StateFlow<NetworkSettingsUiState> = combine(
        serviceState,
        currentProxyMode,
        runtimeSnapshot,
        rootTunDnsMode.state,
    ) { serviceState, configuredMode, snapshot, dnsMode ->
        val effectiveMode = RuntimeStateMapper.resolveDisplayMode(snapshot, configuredMode)
        val activeMode = RuntimeStateMapper.modeForOwner(snapshot.owner)
        NetworkSettingsUiState(
            serviceState = serviceState,
            configuredMode = configuredMode,
            effectiveMode = effectiveMode,
            needsRestart = serviceState == ServiceState.Running && activeMode != configuredMode,
            showServiceOptions = configuredMode != ProxyMode.Http,
            showTunOnlyOptions = configuredMode == ProxyMode.Tun,
            showAccessControlMode = configuredMode != ProxyMode.Http,
            showRootTunAdvanced = configuredMode == ProxyMode.RootTun,
            showFakeIpRange = configuredMode == ProxyMode.RootTun && dnsMode == RootTunDnsMode.FakeIp,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NetworkSettingsUiState(),
    )

    fun onProxyModeChange(mode: ProxyMode) {
        proxyMode.set(mode)
    }

    fun onBypassPrivateNetworkChange(enabled: Boolean) {
        updatePreference(bypassPrivateNetwork, enabled)
    }

    fun onDnsHijackChange(enabled: Boolean) {
        updatePreference(dnsHijack, enabled)
    }

    fun onAllowBypassChange(enabled: Boolean) {
        updatePreference(allowBypass, enabled)
    }

    fun onEnableIPv6Change(enabled: Boolean) {
        updatePreference(enableIPv6, enabled)
    }

    fun onSystemProxyChange(enabled: Boolean) {
        updatePreference(systemProxy, enabled)
    }

    fun onTunStackChange(stack: TunStack) {
        updatePreference(tunStack, stack)
    }

    fun onRootTunAutoRouteChange(enabled: Boolean) {
        updatePreference(rootTunAutoRoute, enabled)
    }

    fun onRootTunStrictRouteChange(enabled: Boolean) {
        updatePreference(rootTunStrictRoute, enabled)
    }

    fun onRootTunAutoRedirectChange(enabled: Boolean) {
        updatePreference(rootTunAutoRedirect, enabled)
    }

    fun onRootTunDnsModeChange(mode: RootTunDnsMode) {
        updatePreference(rootTunDnsMode, mode)
    }

    fun onAccessControlModeChange(mode: AccessControlMode) {
        updatePreference(accessControlMode, mode)
    }

    fun onRootTunIfNameDraftChange(value: String) {
        _rootTunIfNameDraft.value = value
    }

    fun commitRootTunIfName() {
        val normalized = _rootTunIfNameDraft.value.trim().ifBlank { DEFAULT_ROOT_TUN_IF_NAME }
        _rootTunIfNameDraft.value = normalized
        updatePreference(rootTunIfName, normalized)
    }

    fun onRootTunMtuDraftChange(value: String) {
        _rootTunMtuDraft.value = value
    }

    fun commitRootTunMtu() {
        val parsed = _rootTunMtuDraft.value.trim().toIntOrNull()?.takeIf { it > 0 } ?: return
        _rootTunMtuDraft.value = parsed.toString()
        updatePreference(rootTunMtu, parsed)
    }

    fun onRootTunIncludeAndroidUserDraftChange(value: String) {
        _rootTunIncludeAndroidUserDraft.value = value
    }

    fun commitRootTunIncludeAndroidUser() {
        val normalized = _rootTunIncludeAndroidUserDraft.value
            .split(',', '\n')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .mapNotNull(String::toIntOrNull)
            .filter { it >= 0 }
            .distinct()
            .sorted()
            .ifEmpty { listOf(0, 10) }
        _rootTunIncludeAndroidUserDraft.value = normalized.joinToString(", ")
        updatePreference(rootTunIncludeAndroidUser, normalized)
    }

    fun onRootTunRouteExcludeAddressDraftChange(value: String) {
        _rootTunRouteExcludeAddressDraft.value = value
    }

    fun commitRootTunRouteExcludeAddress() {
        val normalized = _rootTunRouteExcludeAddressDraft.value
            .split(',', '\n')
            .map(String::trim)
            .filter(String::isNotEmpty)
        _rootTunRouteExcludeAddressDraft.value = normalized.joinToString("\n")
        updatePreference(rootTunRouteExcludeAddress, normalized)
    }

    fun onRootTunFakeIpRangeDraftChange(value: String) {
        _rootTunFakeIpRangeDraft.value = value
    }

    fun commitRootTunFakeIpRange() {
        val normalized = _rootTunFakeIpRangeDraft.value.trim().ifBlank { DEFAULT_FAKE_IP_RANGE }
        _rootTunFakeIpRangeDraft.value = normalized
        updatePreference(rootTunFakeIpRange, normalized)
    }

    fun onRootTunFakeIpRange6DraftChange(value: String) {
        _rootTunFakeIpRange6Draft.value = value
    }

    fun commitRootTunFakeIpRange6() {
        val normalized = _rootTunFakeIpRange6Draft.value.trim().ifBlank { DEFAULT_FAKE_IP_RANGE6 }
        _rootTunFakeIpRange6Draft.value = normalized
        updatePreference(rootTunFakeIpRange6, normalized)
    }

    fun startService(mode: ProxyMode) {
        viewModelScope.launch {
            switchService(mode).onFailure { error ->
                _errors.tryEmit(error.message ?: "Failed to start proxy service")
            }
        }
    }

    fun restartService() {
        viewModelScope.launch {
            if (!RuntimeStateMapper.isActuallyRunning(runtimeSnapshot.value)) return@launch
            switchService(proxyMode.value).onFailure { error ->
                _errors.tryEmit(error.message ?: "Failed to restart proxy service")
            }
        }
    }

    private suspend fun switchService(mode: ProxyMode): Result<Unit> = runCatching {
        proxyMode.set(mode)
        withContext(Dispatchers.IO) {
            proxyFacade.startProxy(mode)
        }
    }

    private fun updateServiceConfig() {
        restartJob?.cancel()
        restartJob = viewModelScope.launch {
            delay(RESTART_DEBOUNCE_DELAY_MS)
            if (RuntimeStateMapper.isActuallyRunning(runtimeSnapshot.value)) {
                restartService()
            }
        }
    }

    private fun <T> updatePreference(preference: Preference<T>, value: T) {
        if (preference.value == value) return
        preference.set(value)
        updateServiceConfig()
    }

    companion object {
        private const val RESTART_DEBOUNCE_DELAY_MS = 300L
        private const val DEFAULT_ROOT_TUN_IF_NAME = "Yume"
        private const val DEFAULT_FAKE_IP_RANGE = "198.18.0.1/16"
        private const val DEFAULT_FAKE_IP_RANGE6 = "fc00::/18"
    }
}

data class NetworkSettingsUiState(
    val serviceState: ServiceState = ServiceState.Stopped,
    val configuredMode: ProxyMode = ProxyMode.Tun,
    val effectiveMode: ProxyMode = ProxyMode.Tun,
    val needsRestart: Boolean = false,
    val showServiceOptions: Boolean = true,
    val showTunOnlyOptions: Boolean = true,
    val showAccessControlMode: Boolean = true,
    val showRootTunAdvanced: Boolean = false,
    val showFakeIpRange: Boolean = false,
)

enum class ServiceState {
    Running, Stopped
}
