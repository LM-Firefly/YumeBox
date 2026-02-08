package com.github.yumelira.yumebox.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.data.repository.OverrideRepository
import com.github.yumelira.yumebox.data.store.ProxyDisplaySettingsStore
import com.github.yumelira.yumebox.domain.facade.ProfilesRepository
import com.github.yumelira.yumebox.domain.facade.ProxyFacade
import com.github.yumelira.yumebox.domain.model.ProxyDisplayMode
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.domain.model.ProxySortMode
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

class ProxyViewModel(
    private val overrideRepository: OverrideRepository,
    private val proxyFacade: ProxyFacade,
    private val proxyDisplaySettingsStore: ProxyDisplaySettingsStore,
    private val profilesRepository: ProfilesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProxyUiState())
    val uiState: StateFlow<ProxyUiState> = _uiState.asStateFlow()

    private val _testingGroupNames = MutableStateFlow<Set<String>>(emptySet())
    val testingGroupNames: StateFlow<Set<String>> = _testingGroupNames.asStateFlow()

    val currentMode: StateFlow<TunnelState.Mode> = proxyDisplaySettingsStore.proxyMode.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, TunnelState.Mode.Rule)

    val displayMode: StateFlow<ProxyDisplayMode> = proxyDisplaySettingsStore.displayMode.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProxyDisplayMode.SINGLE_DETAILED)

    val sortMode: StateFlow<ProxySortMode> = proxyDisplaySettingsStore.sortMode.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProxySortMode.DEFAULT)

    private val _selectedGroupIndex = MutableStateFlow(0)
    val selectedGroupIndex: StateFlow<Int> = _selectedGroupIndex.asStateFlow()

    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = proxyFacade.proxyGroups


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

    fun ensureCoreLoaded() {
        Timber.d("ensureCoreLoaded: no-op")
    }

    fun patchMode(mode: TunnelState.Mode) {
        proxyDisplaySettingsStore.proxyMode.set(mode)
        viewModelScope.launch {
            try {
                val persistResult = overrideRepository.updatePersist {
                    it.copy(mode = mode)
                }
                if (persistResult.isFailure) {
                    val error = persistResult.exceptionOrNull()
                    Timber.e(error, "代理模式切换失败：$mode")
                    showError(MLang.Proxy.Mode.SwitchFailed.format(error?.message))
                    return@launch
                }

                val sessionResult = overrideRepository.updateSession {
                    it.copy(mode = mode)
                }
                if (sessionResult.isFailure) {
                    val error = sessionResult.exceptionOrNull()
                    Timber.e(error, "代理模式切换失败：$mode")
                    showError(MLang.Proxy.Mode.SwitchFailed.format(error?.message))
                    return@launch
                }

                val reloadResult = proxyFacade.reloadCurrentProfile()
                if (reloadResult.isFailure) {
                    val error = reloadResult.exceptionOrNull()
                    Timber.e(error, "代理模式切换失败：$mode")
                    showError(MLang.Proxy.Mode.SwitchFailed.format(error?.message))
                    return@launch
                }

                delay(500)
                
                val modeName = when (mode) {
                    TunnelState.Mode.Direct -> MLang.Proxy.Mode.Direct
                    TunnelState.Mode.Global -> MLang.Proxy.Mode.Global
                    TunnelState.Mode.Rule -> MLang.Proxy.Mode.Rule
                    else -> MLang.Proxy.Mode.Unknown
                }
                showMessage(MLang.Proxy.Mode.Switched.format(modeName))
            } catch (e: Exception) {
                Timber.e(e, "代理模式切换异常")
                showError(MLang.Proxy.Mode.SwitchFailed.format(e.message))
            }
        }
    }

    fun testDelay(groupName: String? = null) {
        viewModelScope.launch {
            try {
                setLoading(true)
                clearError()

                if (groupName != null) {
                    _testingGroupNames.update { it + groupName }
                    showMessage(MLang.Proxy.Testing.Group.format(groupName))
                    proxyFacade.healthCheck(groupName, refreshAfter = true)
                    showMessage(MLang.Proxy.Testing.RequestSent)
                } else {
                    // 测试所有组
                    showMessage(MLang.Proxy.Testing.All)
                    val groups = proxyGroups.value
                    groups.forEach { group ->
                        try {
                            proxyFacade.healthCheck(group.name, refreshAfter = false)
                        } catch (e: Exception) {
                            Timber.w(e, "Health check failed for group: ${group.name}")
                        }
                    }

                    // 批量测试完后统一刷新几次，避免每个组都刷导致 UI 卡死
                    repeat(4) {
                        delay(600)
                        proxyFacade.refreshProxyGroups()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "延迟测试异常")
                showError(MLang.Proxy.Testing.Failed.format(e.message))
            } finally {
                if (groupName != null) {
                    _testingGroupNames.update { it - groupName }
                }
                setLoading(false)
            }
        }
    }

    fun setSelectedGroup(index: Int) {
        val groups = proxyGroups.value
        _selectedGroupIndex.value = index.coerceIn(0, groups.size - 1)
    }


    fun setDisplayMode(mode: ProxyDisplayMode) {
        proxyDisplaySettingsStore.displayMode.set(mode)
    }

    fun setSortMode(mode: ProxySortMode) {
        proxyDisplaySettingsStore.sortMode.set(mode)
    }

    fun selectProxy(groupName: String, proxyName: String) {
        viewModelScope.launch {
            try {
                val success = proxyFacade.selectProxy(groupName, proxyName)
                if (success) {
                    showMessage(MLang.Proxy.Selection.Switched.format(proxyName))
                } else {
                    showError(MLang.Proxy.Selection.Failed)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to select proxy")
                showError(MLang.Proxy.Selection.Error.format(e.message))
            }
        }
    }

    private fun sortProxies(proxies: List<Proxy>, sortMode: ProxySortMode): List<Proxy> = when (sortMode) {
        ProxySortMode.DEFAULT -> proxies
        ProxySortMode.BY_NAME -> proxies.sortedBy { it.name }
        ProxySortMode.BY_LATENCY -> proxies.sortedWith(compareBy { if (it.delay > 0) it.delay else Int.MAX_VALUE })
    }

    private fun setLoading(loading: Boolean) {
        _uiState.update { it.copy(isLoading = loading) }
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    private fun showError(error: String) {
        _uiState.update { it.copy(error = error) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    data class ProxyUiState(
        val isLoading: Boolean = false,
        val message: String? = null,
        val error: String? = null
    )
}
