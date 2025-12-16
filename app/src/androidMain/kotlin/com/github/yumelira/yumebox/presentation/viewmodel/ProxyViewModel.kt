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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.data.store.ProxyDisplaySettingsStore
import com.github.yumelira.yumebox.domain.model.ProxyDisplayMode
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.domain.model.ProxySortMode
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.delay

class ProxyViewModel(
    private val clashManager: ClashManager,
    private val proxyDisplaySettingsStore: ProxyDisplaySettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProxyUiState())
    val uiState: StateFlow<ProxyUiState> = _uiState.asStateFlow()

    val currentMode: StateFlow<TunnelState.Mode> = proxyDisplaySettingsStore.proxyMode.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, TunnelState.Mode.Rule)

    val displayMode: StateFlow<ProxyDisplayMode> = proxyDisplaySettingsStore.displayMode.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProxyDisplayMode.DOUBLE_SIMPLE)

    val sortMode: StateFlow<ProxySortMode> = proxyDisplaySettingsStore.sortMode.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProxySortMode.DEFAULT)

    private val _globalTimeout = MutableStateFlow(0)
    val globalTimeout: StateFlow<Int> = _globalTimeout.asStateFlow()

    private val _selectedGroupIndex = MutableStateFlow(0)
    val selectedGroupIndex: StateFlow<Int> = _selectedGroupIndex.asStateFlow()


    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = clashManager.proxyGroups


    val sortedProxyGroups: StateFlow<List<ProxyGroupInfo>> =
        combine(proxyGroups, sortMode) { groups, mode ->
            if (mode == ProxySortMode.DEFAULT) {
                groups
            } else {
                groups.map { group ->
                    group.copy(proxies = sortProxies(group.proxies, mode))
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _testRequested = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            runCatching {
                val config = Clash.queryOverride(Clash.OverrideSlot.Persist)
                _globalTimeout.value = config.globalTimeout ?: 0
            }
        }
    }

    fun patchMode(mode: TunnelState.Mode) {
        proxyDisplaySettingsStore.proxyMode.set(mode)
        viewModelScope.launch {
            runCatching {
                val persistOverride = Clash.queryOverride(Clash.OverrideSlot.Persist)
                persistOverride.mode = mode
                Clash.patchOverride(Clash.OverrideSlot.Persist, persistOverride)
                
                val sessionOverride = Clash.queryOverride(Clash.OverrideSlot.Session)
                sessionOverride.mode = mode
                Clash.patchOverride(Clash.OverrideSlot.Session, sessionOverride)
                
                clashManager.reloadCurrentProfile()
                delay(100)
                val modeName = when (mode) {
                    TunnelState.Mode.Direct -> MLang.Proxy.Mode.Direct
                    TunnelState.Mode.Global -> MLang.Proxy.Mode.Global
                    TunnelState.Mode.Rule -> MLang.Proxy.Mode.Rule
                    else -> MLang.Proxy.Mode.Unknown
                }
                showMessage(MLang.Proxy.Mode.Switched.format(modeName))
                clashManager.refreshProxyGroups()
            }.onFailure { e ->
                showError(MLang.Proxy.Mode.SwitchFailed.format(e.message))
            }
        }
    }

    fun testDelay(groupName: String? = null) {
        viewModelScope.launch {
            try {
                _testRequested.value = true
                setLoading(true)
                clearError()
                if (groupName != null) {
                    showMessage(MLang.Proxy.Testing.Group.format(groupName))
                    val result = clashManager.healthCheck(groupName)
                    if (result.isSuccess) {
                        showMessage(MLang.Proxy.Testing.RequestSent)
                    } else {
                        showError(MLang.Proxy.Testing.Failed.format(result.exceptionOrNull()?.message))
                    }
                } else {
                    showMessage(MLang.Proxy.Testing.All)
                    val result = clashManager.healthCheckAll()
                    if (result.isFailure) {
                        showError(MLang.Proxy.Testing.Failed.format(result.exceptionOrNull()?.message))
                    }
                }
            } catch (e: Exception) {
                showError(MLang.Proxy.Testing.Failed.format(e.message))
            } finally {
                _testRequested.value = false
                setLoading(false)
            }
        }
    }

    fun refreshProxyGroups() {
        viewModelScope.launch {
            try {
                setLoading(true)
                val result = clashManager.refreshProxyGroups()
                if (result.isSuccess) {
                    showMessage(MLang.Proxy.Refresh.Success)
                } else {
                    showError(MLang.Proxy.Refresh.Failed.format(result.exceptionOrNull()?.message))
                }
            } catch (e: Exception) {
                showError(MLang.Proxy.Refresh.Failed.format(e.message))
            } finally {
                setLoading(false)
            }
        }
    }

    fun setSelectedGroup(index: Int) {
        val groups = proxyGroups.value
        _selectedGroupIndex.value = index.coerceIn(0, groups.size - 1)
    }

    fun toggleDisplayMode() {
        val current = displayMode.value
        val newMode = when (current) {
            ProxyDisplayMode.SINGLE_DETAILED -> ProxyDisplayMode.SINGLE_SIMPLE
            ProxyDisplayMode.SINGLE_SIMPLE -> ProxyDisplayMode.DOUBLE_DETAILED
            ProxyDisplayMode.DOUBLE_DETAILED -> ProxyDisplayMode.DOUBLE_SIMPLE
            ProxyDisplayMode.DOUBLE_SIMPLE -> ProxyDisplayMode.SINGLE_DETAILED
        }
        setDisplayMode(newMode)
    }

    fun setDisplayMode(mode: ProxyDisplayMode) {
        proxyDisplaySettingsStore.displayMode.set(mode)
    }

    fun setSortMode(mode: ProxySortMode) {
        proxyDisplaySettingsStore.sortMode.set(mode)
    }

    fun toggleSortMode() {
        val current = sortMode.value
        val newMode = when (current) {
            ProxySortMode.DEFAULT -> ProxySortMode.BY_NAME
            ProxySortMode.BY_NAME -> ProxySortMode.BY_LATENCY
            ProxySortMode.BY_LATENCY -> ProxySortMode.DEFAULT
        }
        setSortMode(newMode)
    }

    fun selectProxy(groupName: String, proxyName: String) {
        viewModelScope.launch {
            try {
                val success = clashManager.selectProxy(groupName, proxyName)
                if (success) {
                    showMessage(MLang.Proxy.Selection.Switched.format(proxyName))
                } else {
                    showError(MLang.Proxy.Selection.Failed)
                }
            } catch (e: Exception) {
                showError(MLang.Proxy.Selection.Error.format(e.message))
            }
        }
    }

    fun forceSelectProxy(groupName: String, proxyName: String) {
        viewModelScope.launch {
            try {
                val success = clashManager.forceSelectProxy(groupName, proxyName)
                if (success) {
                    if (proxyName.isBlank()) {
                        showMessage(MLang.Proxy.Selection.Unpinned)
                    } else {
                        showMessage(MLang.Proxy.Selection.Pinned.format(proxyName))
                    }
                } else {
                    showError(MLang.Proxy.Selection.Failed)
                }
            } catch (e: Exception) {
                showError(MLang.Proxy.Selection.Error.format(e.message))
            }
        }
    }

    fun onScreenActive() {
        clashManager.setProxyScreenActive(true)
        refreshProxyGroups()
    }

    fun onScreenInactive() {
        clashManager.setProxyScreenActive(false)
    }

    private fun setLoading(loading: Boolean) = _uiState.update { it.copy(isLoading = loading) }
    private fun showMessage(message: String) = _uiState.update { it.copy(message = message) }
    private fun showError(error: String) = _uiState.update { it.copy(error = error) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    private fun sortProxies(proxies: List<Proxy>, sortMode: ProxySortMode): List<Proxy> = when (sortMode) {
        ProxySortMode.DEFAULT -> proxies
        ProxySortMode.BY_NAME -> proxies.sortedBy { it.name }
        ProxySortMode.BY_LATENCY -> proxies.sortedWith(compareBy { if (it.delay > 0) it.delay else Int.MAX_VALUE })
    }

    data class ProxyUiState(
        val isLoading: Boolean = false,
        val message: String? = null,
        val error: String? = null
    )
}