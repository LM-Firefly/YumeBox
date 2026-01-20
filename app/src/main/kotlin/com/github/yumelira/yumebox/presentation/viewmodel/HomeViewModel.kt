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
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.clash.config.Configuration
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.repository.IpMonitoringState
import com.github.yumelira.yumebox.domain.facade.SettingsFacade
import com.github.yumelira.yumebox.domain.facade.ProfilesFacade
import com.github.yumelira.yumebox.domain.facade.ProxyFacade
import com.github.yumelira.yumebox.domain.model.Connection
import com.github.yumelira.yumebox.domain.model.ConnectionsSnapshot
import com.github.yumelira.yumebox.domain.model.TrafficData
import dev.oom_wg.purejoy.mlang.MLang
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import timber.log.Timber

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModel(
    application: Application,
    private val proxyFacade: ProxyFacade,
    private val profilesRepository: ProfilesFacade,
    private val settingsFacade: SettingsFacade
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "HomeViewModel"
        private val SUSPICIOUS_TYPE_LOGGED = AtomicBoolean(false)
    }

    val profiles: StateFlow<List<Profile>> = profilesRepository.profiles
    val recommendedProfile: StateFlow<Profile?> = profilesRepository.recommendedProfile
    val hasEnabledProfile: Flow<Boolean> = profiles.map { it.any { profile -> profile.enabled } }

    val isRunning = proxyFacade.isRunning
    val currentProfile = proxyFacade.currentProfile
    val trafficNow = proxyFacade.trafficNow
    val proxyGroups = proxyFacade.proxyGroups
    private val _connections = MutableStateFlow<List<Connection>>(emptyList())
    val connections: StateFlow<List<Connection>> = _connections.asStateFlow()

    val oneWord: StateFlow<String> = settingsFacade.oneWord
    val oneWordAuthor: StateFlow<String> = settingsFacade.oneWordAuthor

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _displayRunning = MutableStateFlow(false)
    val displayRunning: StateFlow<Boolean> = _displayRunning.asStateFlow()

    private val _isToggling = MutableStateFlow(false)
    val isToggling: StateFlow<Boolean> = _isToggling.asStateFlow()

    private val _vpnPrepareIntent = MutableSharedFlow<Intent>(
        replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val vpnPrepareIntent = _vpnPrepareIntent.asSharedFlow()

    val speedHistory: StateFlow<List<TrafficData>> = flow {
        val history = java.util.ArrayDeque<TrafficData>(24)
        repeat(24) { history.add(TrafficData.ZERO) }
        while (true) {
            val sample = proxyFacade.trafficNow.value
            if (history.size >= 24) history.removeFirst()
            history.add(sample)
            emit(history.toList())
            delay(1000)
        }
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = List(24) { TrafficData.ZERO }
    )

    private val mainProxyNode: StateFlow<com.github.yumelira.yumebox.core.model.Proxy?> =
        combine(isRunning, proxyGroups) { running, groups ->
            if (!running || groups.isEmpty()) return@combine null
            val mainGroup = groups.find { it.name.equals("Proxy", ignoreCase = true) } ?: groups.firstOrNull()
            if (mainGroup != null && mainGroup.now.isNotBlank()) {
                proxyFacade.resolveProxyEndNode(mainGroup.now, groups)
            } else {
                null
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedServerName: StateFlow<String?> =
        mainProxyNode.map { it?.name }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedServerPing: StateFlow<Int?> = mainProxyNode.map { node ->
        node?.let {
            proxyFacade.getCachedDelay(it.name)?.takeIf { d -> d > 0 } ?: it.delay.takeIf { d -> d > 0 }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val ipMonitoringState: StateFlow<IpMonitoringState> = isRunning.flatMapLatest { running ->
        if (running) {
            proxyFacade.startIpMonitoring(isRunning)
        } else {
            flowOf(IpMonitoringState.Loading)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IpMonitoringState.Loading)

    init {
        syncDisplayState()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                proxyFacade.connections.collect { snapshot ->
                    Timber.tag(TAG).v("Connections snapshot received: size=%d", snapshot.connections?.size ?: 0)
                    handleConnectionsSnapshot(snapshot)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Timber.tag(TAG).w(e, "Connections flow cancelled, aborting")
                    return@launch
                }
                Timber.tag(TAG).e(e, "Connections flow failed")
            }
        }
    }

    private fun syncDisplayState() {
        viewModelScope.launch {
            isRunning.collect { running ->
                if (!_isToggling.value) {
                    _displayRunning.value = running
                }
                if (running == _displayRunning.value) {
                    _isToggling.value = false
                }
            }
        }
    }

    private fun handleConnectionsSnapshot(snapshot: com.github.yumelira.yumebox.domain.model.ConnectionsSnapshot) {
        try {
            _connections.value = snapshot.connections ?: emptyList()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error handling connections snapshot in HomeViewModel")
        }
    }

    suspend fun reloadProfile(profileId: String) {
        try {
            setLoading(true)
            // 从 ProfilesStore 获取配置
            val profile = profilesRepository.profiles.value.find { it.id == profileId }
            if (profile == null) {
                showError(MLang.Home.Message.ConfigSwitchFailed.format(MLang.ProfilesVM.Error.ProfileNotExist))
                return
            }

            // 重新加载配置
            val result = proxyFacade.loadProfile(profile)
            if (result.isSuccess) {
                showMessage(MLang.Home.Message.ConfigSwitched)
            } else {
                showError(
                    MLang.Home.Message.ConfigSwitchFailed.format(result.exceptionOrNull()?.message)
                )
            }
        } catch (e: Exception) {
            showError(MLang.Home.Message.ConfigSwitchFailed.format(e.message))
        } finally {
            setLoading(false)
        }
    }

    fun startProxy(profileId: String, useTunMode: Boolean? = null) {
        if (_isToggling.value) return

        viewModelScope.launch {
            _isToggling.value = true
            _displayRunning.value = true
            _uiState.update {
                it.copy(
                    isStartingProxy = true,
                    loadingProgress = MLang.Home.Message.Preparing
                )
            }

            val result = proxyFacade.startProxy(profileId, useTunMode)

            result.fold(onSuccess = { intent ->
                if (intent != null) {
                    _uiState.update { it.copy(isStartingProxy = false, loadingProgress = null) }
                    _vpnPrepareIntent.emit(intent)
                    _displayRunning.value = false
                    _isToggling.value = false
                } else {
                    _uiState.update { it.copy(isStartingProxy = false, loadingProgress = null) }
                }
            }, onFailure = { error ->
                _displayRunning.value = false
                _isToggling.value = false
                _uiState.update { it.copy(isStartingProxy = false, loadingProgress = null) }
                showError(MLang.Home.Message.StartFailed.format(error.message))
            })
        }
    }

    fun stopProxy() {
        if (_isToggling.value) return

        viewModelScope.launch {
            _isToggling.value = true
            _displayRunning.value = false
            setLoading(true)

            val result = proxyFacade.stopProxy()
            result.onSuccess {
                showMessage(MLang.Home.Message.ProxyStopped)
            }.onFailure { e ->
                _displayRunning.value = true
                _isToggling.value = false
                showError(MLang.Home.Message.StopFailed.format(e.message))
            }

            setLoading(false)
        }
    }



    private fun setLoading(loading: Boolean) = _uiState.update { it.copy(isLoading = loading) }
    private fun showMessage(message: String) = _uiState.update { it.copy(message = message) }
    private fun showError(error: String) = _uiState.update { it.copy(error = error) }

    data class HomeUiState(
        val isLoading: Boolean = false,
        val isStartingProxy: Boolean = false,
        val loadingProgress: String? = null,
        val message: String? = null,
        val error: String? = null
    )
}
