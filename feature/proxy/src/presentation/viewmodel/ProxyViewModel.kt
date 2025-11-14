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



package com.github.yumelira.yumebox.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.core.presentation.ContractStateViewModel
import com.github.yumelira.yumebox.core.presentation.LoadableState
import com.github.yumelira.yumebox.core.util.PollingTimerSpecs
import com.github.yumelira.yumebox.core.util.PollingTimers
import com.github.yumelira.yumebox.data.repository.AppSettingsRepository
import com.github.yumelira.yumebox.data.repository.OverrideRepository
import com.github.yumelira.yumebox.data.repository.ProxyDisplaySettingsRepository
import com.github.yumelira.yumebox.domain.model.*
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
import com.github.yumelira.yumebox.runtime.client.ProxyGroupSyncPriority
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProxyViewModel(
    private val overrideRepository: OverrideRepository,
    private val proxyFacade: ProxyFacade,
    private val proxyDisplaySettingsRepository: ProxyDisplaySettingsRepository,
    private val appSettingsRepository: AppSettingsRepository,
) : ContractStateViewModel<ProxyViewModel.ProxyUiState, ProxyViewModel.ProxyUiEffect>(ProxyUiState()) {
    private val _testingGroupNames = MutableStateFlow<Set<String>>(emptySet())
    val testingGroupNames: StateFlow<Set<String>> = _testingGroupNames.asStateFlow()

    private val _testingProxyNames = MutableStateFlow<Set<String>>(emptySet())
    val testingProxyNames: StateFlow<Set<String>> = _testingProxyNames.asStateFlow()

    private val groupSorter = ProxyGroupSorter()

    val currentMode: StateFlow<TunnelState.Mode> = proxyDisplaySettingsRepository.proxyMode.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TunnelState.Mode.Rule)

    val displayMode: StateFlow<ProxyDisplayMode> = proxyDisplaySettingsRepository.displayMode.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProxyDisplayMode.SINGLE_DETAILED)

    val sortMode: StateFlow<ProxySortMode> = proxyDisplaySettingsRepository.sortMode.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProxySortMode.DEFAULT)

    val sheetHeightFraction: StateFlow<Float> = proxyDisplaySettingsRepository.sheetHeightFraction.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PROXY_SHEET_HEIGHT_FRACTION_DEFAULT)

    val singleNodeTest: StateFlow<Boolean> = appSettingsRepository.singleNodeTest.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = proxyFacade.proxyGroups
        .map { groups -> groups.filterNot(ProxyGroupInfo::hidden) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val activeSyncSources = mutableSetOf<String>()

    init {
        proxyFacade.warmUpProxyGroups()
        viewModelScope.launch {
            proxyGroups.collect { groups ->
                groupSorter.track(groups)
            }
        }
    }

    val sortedProxyGroups: StateFlow<List<ProxyGroupInfo>> = groupSorter.bind(
        scope = viewModelScope,
        proxyGroups = proxyGroups,
        sortMode = sortMode,
    )

    fun ensureCoreLoaded(
        isActive: Boolean,
        source: String = "proxy_page",
    ) {
        val changed = if (isActive) {
            activeSyncSources.add(source)
        } else {
            activeSyncSources.remove(source)
        }
        if (!changed) return
        proxyFacade.setProxyGroupSyncPriority(
            priority = if (isActive) ProxyGroupSyncPriority.FAST else ProxyGroupSyncPriority.OFF,
            source = source,
        )
        if (isActive) {
            viewModelScope.launch {
                runCatching {
                    if (proxyGroups.value.isEmpty()) {
                        proxyFacade.refreshProxyGroups()
                    }
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                }
            }
        }
    }

    fun refreshGroup(groupName: String) {
        viewModelScope.launch {
            runCatching {
                proxyFacade.refreshProxyGroup(groupName)
            }.onFailure { error ->
                if (error is CancellationException) throw error
            }
        }
    }

    fun patchMode(mode: TunnelState.Mode) {
        val previousMode = proxyDisplaySettingsRepository.proxyMode.value
        proxyDisplaySettingsRepository.proxyMode.set(mode)
        viewModelScope.launch {
            val persistError = overrideRepository.updateProfile { it.copy(mode = mode) }.exceptionOrNull()
            if (persistError != null) {
                proxyDisplaySettingsRepository.proxyMode.set(previousMode)
                showError(MLang.Proxy.Mode.SwitchFailed.format(persistError.message))
                return@launch
            }

            val reloadError = proxyFacade.reloadCurrentProfile().exceptionOrNull()
            if (reloadError != null) {
                proxyDisplaySettingsRepository.proxyMode.set(previousMode)
                showError(MLang.Proxy.Mode.SwitchFailed.format(reloadError.message))
                return@launch
            }

            PollingTimers.awaitTick(PollingTimerSpecs.ProxySwitchFeedback)
            showMessage(MLang.Proxy.Mode.Switched.format(mode.toModeName()))
        }
    }

    fun testDelay(groupName: String? = null) {
        viewModelScope.launch {
            setLoading(true)
            clearError()
            val currentGroups = proxyGroups.value
            val testingTargets: Set<String> = if (groupName != null) {
                setOf(groupName)
            } else {
                currentGroups.mapTo(linkedSetOf()) { it.name }
            }
            if (testingTargets.isNotEmpty()) {
                _testingGroupNames.update { it + testingTargets }
            }

            val result = runCatching {
                if (groupName != null) {
                    showMessage(MLang.Proxy.Testing.Group.format(groupName))
                    proxyFacade.healthCheck(groupName)
                    PollingTimers.awaitTick(PollingTimerSpecs.ProxyHealthcheckRefresh)
                    proxyFacade.refreshProxyGroup(groupName)
                    showMessage(MLang.Proxy.Testing.RequestSent)
                } else {
                    showMessage(MLang.Proxy.Testing.All)
                    var firstError: Throwable? = null
                    currentGroups.forEach { group ->
                        runCatching {
                            proxyFacade.healthCheck(group.name)
                        }.onFailure { error ->
                            if (firstError == null) {
                                firstError = error
                            }
                        }
                    }
                    if (currentGroups.isNotEmpty()) {
                        PollingTimers.awaitTick(PollingTimerSpecs.ProxyHealthcheckRefresh)
                        proxyFacade.refreshProxyGroups()
                    }
                    firstError?.let { throw it }
                }
            }

            setLoading(false)

            if (testingTargets.isNotEmpty()) {
                PollingTimers.awaitTick(PollingTimerSpecs.ProxyTestingSortHold)
                _testingGroupNames.update { it - testingTargets }
            }

            result.exceptionOrNull()?.let { error ->
                showError(MLang.Proxy.Testing.Failed.format(error.message))
            }
        }
    }

    fun setDisplayMode(mode: ProxyDisplayMode) {
        proxyDisplaySettingsRepository.displayMode.set(mode)
    }

    fun setSortMode(mode: ProxySortMode) {
        proxyDisplaySettingsRepository.sortMode.set(mode)
    }

    fun setSheetHeightFraction(value: Float) {
        proxyDisplaySettingsRepository.sheetHeightFraction.set(normalizeProxySheetHeightFraction(value))
    }

    fun selectProxy(groupName: String, proxyName: String) {
        viewModelScope.launch {
            runCatching {
                val success = proxyFacade.selectProxy(groupName, proxyName)
                if (success) {
                    showMessage(MLang.Proxy.Selection.Switched.format(proxyName))
                } else {
                    showError(MLang.Proxy.Selection.Failed)
                }
            }.onFailure { error ->
                showError(MLang.Proxy.Selection.Error.format(error.message))
            }
        }
    }

    fun testProxyDelay(proxyName: String) {
        val groupName = proxyGroups.value.firstOrNull { group ->
            group.proxies.any { it.name == proxyName }
        }?.name ?: return
        testProxyDelay(groupName, proxyName)
    }

    fun testProxyDelay(groupName: String, proxyName: String) {
        viewModelScope.launch {
            _testingProxyNames.update { it + proxyName }
            runCatching {
                proxyFacade.healthCheckProxy(groupName, proxyName)
            }
            PollingTimers.awaitTick(PollingTimerSpecs.ProxySwitchFeedback)
            _testingProxyNames.update { it - proxyName }
        }
    }

    private fun showMessage(message: String) {
        postMessage(message, ProxyUiEffect.ShowMessage(message))
    }

    private fun showError(error: String) {
        postError(error, ProxyUiEffect.ShowError(error))
    }

    fun clearError() {
        clearErrorState()
    }

    private fun TunnelState.Mode.toModeName(): String = when (this) {
        TunnelState.Mode.Direct -> MLang.Proxy.Mode.Direct
        TunnelState.Mode.Global -> MLang.Proxy.Mode.Global
        TunnelState.Mode.Rule -> MLang.Proxy.Mode.Rule
        else -> MLang.Proxy.Mode.Unknown
    }

    data class ProxyUiState(
        override val isLoading: Boolean = false,
        override val message: String? = null,
        override val error: String? = null
    ) : LoadableState<ProxyUiState> {
        override fun withLoading(loading: Boolean): ProxyUiState = copy(isLoading = loading)
        override fun withError(error: String?): ProxyUiState = copy(error = error)
        override fun withMessage(message: String?): ProxyUiState = copy(message = message)
    }

    sealed interface ProxyUiEffect {
        data class ShowMessage(val message: String) : ProxyUiEffect
        data class ShowError(val message: String) : ProxyUiEffect
    }
}
