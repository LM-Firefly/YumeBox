package com.github.yumelira.yumebox.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.domain.facade.ProxyFacade
import com.github.yumelira.yumebox.domain.facade.RuntimeFacade
import com.github.yumelira.yumebox.domain.model.ProxyDisplayMode
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.domain.model.ProxySortMode
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

class ProxyViewModel(
    private val proxyFacade: ProxyFacade,
    private val runtimeFacade: RuntimeFacade
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProxyUiState())
    val uiState: StateFlow<ProxyUiState> = _uiState.asStateFlow()

    private val _testingGroupNames = MutableStateFlow<Set<String>>(emptySet())
    val testingGroupNames: StateFlow<Set<String>> = _testingGroupNames.asStateFlow()

    val currentMode: StateFlow<TunnelState.Mode> = proxyFacade.proxyMode.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, TunnelState.Mode.Rule)

    val displayMode: StateFlow<ProxyDisplayMode> = proxyFacade.displayMode.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProxyDisplayMode.SINGLE_DETAILED)

    val sortMode: StateFlow<ProxySortMode> = proxyFacade.sortMode.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProxySortMode.DEFAULT)

    private val _globalTimeout = MutableStateFlow(0)
    val globalTimeout: StateFlow<Int> = _globalTimeout.asStateFlow()

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

    private val _testRequested = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            runCatching {
                val configResult = runtimeFacade.loadPersist()
                if (configResult.isSuccess) {
                    _globalTimeout.value = configResult.getOrNull()?.globalTimeout ?: 5000
                } else {
                    _globalTimeout.value = 5000
                }
            }
        }
    }

    fun patchMode(mode: TunnelState.Mode) {
        proxyFacade.proxyMode.set(mode)
        viewModelScope.launch {
            val persistResult = runtimeFacade.updatePersist {
                it.copy(mode = mode)
            }
            if (persistResult.isFailure) {
                val error = persistResult.exceptionOrNull()
                Timber.e(error, "代理模式切换失败：$mode")
                showError(MLang.Proxy.Mode.SwitchFailed.format(error?.message))
                return@launch
            }

            val sessionResult = runtimeFacade.updateSession {
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

            // reloadCurrentProfile 已经触发了同步，等待一段时间确保完成
            delay(500)
            
            val modeName = when (mode) {
                TunnelState.Mode.Direct -> MLang.Proxy.Mode.Direct
                TunnelState.Mode.Global -> MLang.Proxy.Mode.Global
                TunnelState.Mode.Rule -> MLang.Proxy.Mode.Rule
                else -> MLang.Proxy.Mode.Unknown
            }
            showMessage(MLang.Proxy.Mode.Switched.format(modeName))
        }
    }

    fun testDelay(groupName: String? = null) {
        viewModelScope.launch {
            try {
                _testRequested.value = true
                setLoading(true)
                clearError()

                if (groupName != null) {
                    _testingGroupNames.update { it + groupName }
                    showMessage(MLang.Proxy.Testing.Group.format(groupName))
                    val result = proxyFacade.healthCheck(groupName)
                    if (result.isSuccess) {
                        showMessage(MLang.Proxy.Testing.RequestSent)
                    } else {
                        showError(MLang.Proxy.Testing.Failed.format(result.exceptionOrNull()?.message))
                    }
                } else {
                    showMessage(MLang.Proxy.Testing.All)
                    val result = proxyFacade.healthCheckAll()
                    if (result.isFailure) {
                        showError(MLang.Proxy.Testing.Failed.format(result.exceptionOrNull()?.message))
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

    fun refreshProxyGroups() {
        viewModelScope.launch {
            try {
                setLoading(true)
                val result = proxyFacade.refreshProxyGroups()
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


    fun setDisplayMode(mode: ProxyDisplayMode) {
        proxyFacade.setDisplayMode(mode)
    }

    fun setSortMode(mode: ProxySortMode) {
        proxyFacade.setSortMode(mode)
    }

    fun selectProxy(groupName: String, proxyName: String) {
        viewModelScope.launch {
            try {
                val result = proxyFacade.selectProxy(groupName, proxyName)
                if (result.isSuccess && result.getOrNull() == true) {
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
                val success = proxyFacade.forceSelectProxy(groupName, proxyName)
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

    fun getCachedDelay(nodeName: String): Int? {
        return proxyFacade.getCachedDelay(nodeName)
    }

    fun getResolvedDelay(nodeName: String): Int? {
        return proxyFacade.getResolvedDelay(nodeName)
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

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
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
