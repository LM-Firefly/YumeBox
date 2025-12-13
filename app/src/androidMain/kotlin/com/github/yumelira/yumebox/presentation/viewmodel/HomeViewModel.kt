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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.repository.NetworkInfoService
import com.github.yumelira.yumebox.data.repository.IpMonitoringState
import com.github.yumelira.yumebox.data.repository.ProxyChainResolver
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import com.github.yumelira.yumebox.domain.facade.ProxyFacade
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.domain.facade.ProfilesRepository
import com.github.yumelira.yumebox.clash.loader.ConfigAutoLoader
import com.github.yumelira.yumebox.clash.config.Configuration
import com.github.yumelira.yumebox.core.model.Proxy
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.delay
import com.github.yumelira.yumebox.domain.model.Connection
import com.github.yumelira.yumebox.domain.model.ConnectionsSnapshot
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import timber.log.Timber

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModel(
    application: Application,
    private val proxyFacade: ProxyFacade,
    private val profilesRepository: ProfilesRepository,
    private val appSettingsStorage: AppSettingsStorage,
    private val configAutoLoadService: ConfigAutoLoader,
    private val networkInfoService: NetworkInfoService,
    private val proxyChainResolver: ProxyChainResolver
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "HomeViewModel"
    }
    private val json = Json { ignoreUnknownKeys = true }

    val profiles: StateFlow<List<Profile>> = profilesRepository.profiles
    val recommendedProfile: StateFlow<Profile?> = profilesRepository.recommendedProfile
    val hasEnabledProfile: Flow<Boolean> = profiles.map { it.any { profile -> profile.enabled } }

    val proxyState = proxyFacade.proxyState
    val isRunning = proxyFacade.isRunning
    val runningMode = proxyFacade.runningMode
    val currentProfile = proxyFacade.currentProfile
    val trafficNow = proxyFacade.trafficNow
    val trafficTotal = proxyFacade.trafficTotal
    val proxyGroups = proxyFacade.proxyGroups
    val tunnelState = proxyFacade.tunnelState
    val connections: StateFlow<List<Connection>> = isRunning.flatMapLatest { running ->
        if (running) {
            flow {
                while (currentCoroutineContext().isActive) {
                    try {
                        val sessionOverride = Clash.queryOverride(Clash.OverrideSlot.Session)
                        val config = Clash.queryConfiguration()
                        val controller = sessionOverride.externalController
                            ?: config.externalController
                            ?: "127.0.0.1:9090"
                        val secret = sessionOverride.secret
                            ?: config.secret
                            ?: Configuration.API_SECRET
                        val url = URL("http://$controller/connections")
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        if (secret.isNotEmpty()) {
                            connection.setRequestProperty("Authorization", "Bearer $secret")
                        }
                        connection.connectTimeout = 1000
                        connection.readTimeout = 1000
                        val responseCode = connection.responseCode
                        if (responseCode == 200) {
                            val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                            val snapshot = json.decodeFromString<ConnectionsSnapshot>(jsonString)
                            emit(snapshot.connections ?: emptyList())
                        } else {
                            Timber.tag(TAG).e("Failed to poll connections: $responseCode")
                            emit(emptyList())
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "Error polling connections")
                        emit(emptyList())
                    }
                    delay(2000)
                }
            }
        } else {
            flowOf(emptyList())
        }
    }
    .flowOn(Dispatchers.IO)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val oneWord: StateFlow<String> = appSettingsStorage.oneWord.state
    val oneWordAuthor: StateFlow<String> = appSettingsStorage.oneWordAuthor.state
    val oneWordAndAuthor: StateFlow<String> =
        combine(appSettingsStorage.oneWord.state, appSettingsStorage.oneWordAuthor.state) { word, author ->
            "\"$word\" — $author"
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _displayRunning = MutableStateFlow(false)
    val displayRunning: StateFlow<Boolean> = _displayRunning.asStateFlow()

    private val _isToggling = MutableStateFlow(false)
    val isToggling: StateFlow<Boolean> = _isToggling.asStateFlow()

    private val _vpnPrepareIntent = MutableSharedFlow<Intent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val vpnPrepareIntent = _vpnPrepareIntent.asSharedFlow()

    val speedHistory: StateFlow<List<Long>> = flow {
        val sampleLimit = 24
        var history = emptyList<Long>()
        while (currentCoroutineContext().isActive) {
            val sample = proxyFacade.trafficNow.value.download.coerceAtLeast(0L)
            history = buildList(sampleLimit) {
                repeat((sampleLimit - history.size - 1).coerceAtLeast(0)) { add(0L) }
                addAll(history.takeLast(sampleLimit - 1))
                add(sample)
            }
            emit(history)
            delay(1000L)
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val mainProxyNode: StateFlow<Proxy?> =
        combine(isRunning, proxyGroups) { running, groups ->
            if (!running || groups.isEmpty()) return@combine null
            val mainGroup = groups.find { it.name.equals("Proxy", ignoreCase = true) } ?: groups.firstOrNull()
            if (mainGroup != null && mainGroup.now.isNotBlank()) {
                proxyChainResolver.resolveEndNode(mainGroup.now, groups)
            } else {
                null
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    val selectedServerName: StateFlow<String?> = mainProxyNode.map { it?.name }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedServerPing: StateFlow<Int?> = mainProxyNode.map { node ->
        node?.let {
            proxyFacade.getCachedDelay(it.name)?.takeIf { d -> d > 0 } ?: it.delay.takeIf { d -> d > 0 }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val ipMonitoringState: StateFlow<IpMonitoringState> =
        isRunning.flatMapLatest { running ->
            if (running) {
                networkInfoService.startIpMonitoring(isRunning)
            } else {
                flowOf(IpMonitoringState.Loading)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IpMonitoringState.Loading)

    private val _recentLogs = MutableStateFlow<List<LogMessage>>(emptyList())
    val recentLogs: StateFlow<List<LogMessage>> = _recentLogs.asStateFlow()

    init {
        syncDisplayState()
        lazyInitialize()
    }
    
    private fun lazyInitialize() {
        viewModelScope.launch {
            delay(500)
            subscribeToLogs()
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

    suspend fun reloadProfile(profileId: String) {
        try {
            setLoading(true)
            val result = configAutoLoadService.reloadConfig(profileId)
            if (result.isSuccess) {
                showMessage(MLang.Home.Message.ConfigSwitched)
            } else {
                showError(MLang.Home.Message.ConfigSwitchFailed.format(result.exceptionOrNull()?.message))
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
            try {
                _isToggling.value = true
                _displayRunning.value = true
                _uiState.update { it.copy(isStartingProxy = true, loadingProgress = MLang.Home.Message.Preparing) }
                val result = proxyFacade.startProxy(profileId, useTunMode)
                result.fold(
                    onSuccess = { intent ->
                        if (intent != null) {
                            _uiState.update { it.copy(isStartingProxy = false, loadingProgress = null) }
                            _vpnPrepareIntent.emit(intent)
                            _displayRunning.value = false
                            _isToggling.value = false
                        } else {
                            _uiState.update { it.copy(isStartingProxy = false, loadingProgress = null) }
                        }
                    },
                    onFailure = { error ->
                        _displayRunning.value = false
                        _isToggling.value = false
                        _uiState.update { it.copy(isStartingProxy = false, loadingProgress = null) }
                        showError(MLang.Home.Message.StartFailed.format(error.message))
                    }
                )
            } catch (e: Exception) {
                _displayRunning.value = false
                _isToggling.value = false
                _uiState.update { it.copy(isStartingProxy = false, loadingProgress = null) }
                showError(MLang.Home.Message.StartFailed.format(e.message))
            }
        }
    }

    fun stopProxy() {
        if (_isToggling.value) return
        viewModelScope.launch {
            try {
                _isToggling.value = true
                _displayRunning.value = false
                setLoading(true)
                proxyFacade.stopProxy()
                showMessage(MLang.Home.Message.ProxyStopped)
            } catch (e: Exception) {
                _displayRunning.value = true
                _isToggling.value = false
                showError(MLang.Home.Message.StopFailed.format(e.message))
            } finally {
                setLoading(false)
            }
        }
    }

    fun refreshIpInfo() = networkInfoService.triggerRefresh()

    fun clearLogs() { _recentLogs.value = emptyList() }

    private fun subscribeToLogs() {
        viewModelScope.launch {
            proxyFacade.logs.collect { log ->
                _recentLogs.update { currentLogs ->
                    buildList {
                        add(log)
                        addAll(currentLogs.take(49))
                    }
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) = _uiState.update { it.copy(isLoading = loading) }
    private fun showMessage(message: String) = _uiState.update { it.copy(message = message) }
    private fun showError(error: String) = _uiState.update { it.copy(error = error) }
    fun clearMessage() = _uiState.update { it.copy(message = null) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    data class HomeUiState(
        val isLoading: Boolean = false,
        val isStartingProxy: Boolean = false,
        val loadingProgress: String? = null,
        val message: String? = null,
        val error: String? = null
    )
}