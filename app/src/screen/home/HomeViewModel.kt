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



package com.github.yumelira.yumebox.screen.home

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.data.repository.IpMonitoringState
import com.github.yumelira.yumebox.data.repository.NetworkInfoService
import com.github.yumelira.yumebox.data.repository.ProxyChainResolver
import com.github.yumelira.yumebox.data.store.MMKVProvider
import com.github.yumelira.yumebox.data.store.NetworkSettingsStorage
import com.github.yumelira.yumebox.domain.model.TrafficData
import com.github.yumelira.yumebox.runtime.client.ProfilesRepository
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
import com.github.yumelira.yumebox.runtime.client.RuntimeStateMapper
import com.github.yumelira.yumebox.service.runtime.entity.Profile
import com.github.yumelira.yumebox.service.runtime.state.RuntimePhase
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class HomeViewModel(
    application: Application,
    private val proxyFacade: ProxyFacade,
    private val profilesRepository: ProfilesRepository,
    private val networkInfoService: NetworkInfoService,
    private val proxyChainResolver: ProxyChainResolver,
) : AndroidViewModel(application) {
    private val networkSettingsStorage by lazy {
        NetworkSettingsStorage(MMKVProvider().getMMKV("network_settings"))
    }

    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    private val _recommendedProfile = MutableStateFlow<Profile?>(null)
    val recommendedProfile: StateFlow<Profile?> = _recommendedProfile.asStateFlow()

    private val _profilesLoaded = MutableStateFlow(false)
    val profilesLoaded: StateFlow<Boolean> = _profilesLoaded.asStateFlow()

    val hasEnabledProfile: Flow<Boolean> = profiles.map { list ->
        list.any { it.active }
    }

    val runtimeSnapshot = proxyFacade.runtimeSnapshot
    val isRunning = runtimeSnapshot
        .map(RuntimeStateMapper::isActuallyRunning)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RuntimeStateMapper.isActuallyRunning(runtimeSnapshot.value))
    val currentProfile = proxyFacade.currentProfile
    val trafficNow = proxyFacade.trafficNow
    val proxyGroups = proxyFacade.proxyGroups

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _displayRunning = MutableStateFlow(false)
    val displayRunning: StateFlow<Boolean> = _displayRunning.asStateFlow()

    private val _isToggling = MutableStateFlow(false)
    val isToggling: StateFlow<Boolean> = _isToggling.asStateFlow()

    private val _proxyMode = MutableStateFlow(ProxyMode.Tun)
    val proxyMode: StateFlow<ProxyMode> = _proxyMode.asStateFlow()

    private val _vpnPrepareIntent = MutableSharedFlow<Intent>(
        replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val vpnPrepareIntent = _vpnPrepareIntent.asSharedFlow()

    private val _speedHistory = MutableStateFlow<List<Long>>(emptyList())
    val speedHistory: StateFlow<List<Long>> = _speedHistory.asStateFlow()

    private val mainProxyNode: StateFlow<com.github.yumelira.yumebox.core.model.Proxy?> =
        combine(runtimeSnapshot, proxyGroups) { snapshot, groups ->
            if (!RuntimeStateMapper.isActuallyRunning(snapshot) || !snapshot.groupsReady || groups.isEmpty()) return@combine null
            val mainGroup = groups.find { it.name.equals("Proxy", ignoreCase = true) } ?: groups.firstOrNull()
            if (mainGroup != null && mainGroup.now.isNotBlank()) {
                proxyChainResolver.resolveEndNode(mainGroup.now, groups)
            } else {
                null
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedServerName: StateFlow<String?> =
        mainProxyNode.map { it?.name }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedServerPing: StateFlow<Int?> = mainProxyNode.map { node ->
        node?.delay?.takeIf { d -> d > 0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val ipMonitoringState: StateFlow<IpMonitoringState> = isRunning.flatMapLatest { running ->
        if (running) {
            networkInfoService.startIpMonitoring(isRunning)
        } else {
            flowOf(IpMonitoringState.Loading)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IpMonitoringState.Loading)

    init {
        refreshProfiles()
        syncDisplayState()
        observeRuntimeFailures()
        syncProxyModeState()
        startSpeedSampling()
        observeProfileChanges()
    }

    private fun refreshProfiles() {
        viewModelScope.launch {
            try {
                val allProfiles = profilesRepository.queryAllProfiles()
                val active = profilesRepository.queryActiveProfile()
                _profiles.value = allProfiles
                _recommendedProfile.value = active
                _profilesLoaded.value = true
            } catch (e: Exception) {
                Timber.e(e, "Failed to refresh profiles")
                _profilesLoaded.value = true
            }
        }
    }

    private fun observeProfileChanges() {
        viewModelScope.launch {
            proxyFacade.currentProfile.collect {

                refreshProfiles()
            }
        }
    }

    private fun syncDisplayState() {
        viewModelScope.launch {
            runtimeSnapshot.collect { snapshot ->
                val runningOrStarting = RuntimeStateMapper.isRunningOrStarting(snapshot)
                if (snapshot.phase == RuntimePhase.Idle || snapshot.phase == RuntimePhase.Failed) {
                    _speedHistory.value = List(24) { 0L }
                }
                _uiState.update {
                    it.copy(
                        isStartingProxy = snapshot.phase == RuntimePhase.Starting,
                        loadingProgress = if (snapshot.phase == RuntimePhase.Starting) {
                            MLang.Home.Message.Preparing
                        } else {
                            null
                        },
                    )
                }
                when (snapshot.phase) {
                    RuntimePhase.Starting -> {
                        _displayRunning.value = true
                    }

                    RuntimePhase.Stopping -> {
                        if (!_isToggling.value) {
                            _displayRunning.value = false
                        }
                    }

                    else -> {
                        _displayRunning.value = runningOrStarting
                        _isToggling.value = false
                    }
                }
            }
        }
    }

    private fun syncProxyModeState() {
        viewModelScope.launch {
            runtimeSnapshot.collect {
                refreshProxyMode()
            }
        }
    }

    private fun observeRuntimeFailures() {
        viewModelScope.launch {
            runtimeSnapshot
                .drop(1)
                .map { snapshot -> Triple(snapshot.phase, snapshot.lastError, snapshot.generation) }
                .distinctUntilChanged()
                .collect { (phase, lastError, _) ->
                    if (phase == RuntimePhase.Failed && !lastError.isNullOrBlank()) {
                        showError(lastError)
                    }
                }
        }
    }

    fun refreshProxyMode() {
        val configuredMode = networkSettingsStorage.proxyMode.value
        _proxyMode.value = RuntimeStateMapper.resolveDisplayMode(runtimeSnapshot.value, configuredMode)
    }

    suspend fun reloadProfile() {
        try {
            setLoading(true)

            val activeProfile = profilesRepository.queryActiveProfile()
            if (activeProfile == null) {
                showError(MLang.Home.Message.ConfigSwitchFailed.format(MLang.ProfilesVM.Error.ProfileNotExist))
                return
            }

            profilesRepository.updateProfile(activeProfile.uuid)

            profilesRepository.setActiveProfile(activeProfile.uuid)
            showMessage(MLang.Home.Message.ConfigSwitched)
        } catch (e: Exception) {
            Timber.e(e, "Failed to reload profile")
            showError(MLang.Home.Message.ConfigSwitchFailed.format(e.message))
        } finally {
            setLoading(false)
        }
    }

    fun isCurrentProfile(profileId: java.util.UUID): Boolean {
        return currentProfile.value?.uuid == profileId
    }

    fun startProxy(profileId: String, mode: ProxyMode? = null) {
        if (_isToggling.value) return

        viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            _isToggling.value = true
            _displayRunning.value = true
            _uiState.update {
                it.copy(
                    isStartingProxy = true,
                    loadingProgress = MLang.Home.Message.Preparing
                )
            }

            try {
                val proxyMode = mode ?: networkSettingsStorage.proxyMode.value
                _proxyMode.value = proxyMode
                Timber.d("Home startProxy kickoff: mode=$proxyMode profileId=$profileId")

                withContext(Dispatchers.IO) {
                    if (profileId.isNotBlank()) {
                        profilesRepository.setActiveProfile(java.util.UUID.fromString(profileId))
                    }

                    proxyFacade.startProxy(proxyMode)
                }

                Timber.i("Home startProxy completed in ${System.currentTimeMillis() - startedAt}ms, mode=$proxyMode")
            } catch (e: com.github.yumelira.yumebox.remote.VpnPermissionRequired) {

                _uiState.update { it.copy(isStartingProxy = false, loadingProgress = null) }
                _vpnPrepareIntent.emit(e.intent)
                _displayRunning.value = false
                _isToggling.value = false
                Timber.i("VPN permission required")
            } catch (e: Exception) {
                _displayRunning.value = false
                _isToggling.value = false
                _uiState.update { it.copy(isStartingProxy = false, loadingProgress = null) }
                Timber.e(e, "Failed to start proxy")
                showError(MLang.Home.Message.StartFailed.format(e.message))
            }
        }
    }

    suspend fun stopProxy() {
        if (_isToggling.value) return

        _isToggling.value = true
        _displayRunning.value = false
        setLoading(true)

        try {
            withContext(Dispatchers.IO) {
                proxyFacade.stopProxy()
            }
            _isToggling.value = false
        } catch (e: Exception) {
            _displayRunning.value = true
            _isToggling.value = false
            Timber.e(e, "Failed to stop proxy")
            showError(MLang.Home.Message.StopFailed.format(e.message))
        }

        setLoading(false)
    }

    private fun startSpeedSampling(sampleLimit: Int = 24) {
        viewModelScope.launch {
            flow {
                while (true) {
                    val snapshot = runtimeSnapshot.value
                    val sample = when {
                        snapshot.phase == RuntimePhase.Idle || snapshot.phase == RuntimePhase.Failed -> 0L
                        snapshot.phase == RuntimePhase.Starting && !snapshot.trafficReady -> {
                            _speedHistory.value.lastOrNull() ?: 0L
                        }

                        snapshot.phase.running -> {
                            val t = proxyFacade.trafficNow.value
                            val d = TrafficData.from(t)
                            (d.upload + d.download).coerceAtLeast(0L)
                        }

                        else -> 0L
                    }
                    emit(sample)
                    kotlinx.coroutines.delay(1000L)
                }
            }.catch { e ->
                Timber.w(e, "Speed sampling loop failed")
            }.collect { sample ->
                _speedHistory.update { old ->
                    buildList(sampleLimit) {
                        repeat((sampleLimit - old.size - 1).coerceAtLeast(0)) { add(0L) }
                        addAll(old.takeLast(sampleLimit - 1))
                        add(sample)
                    }
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) = _uiState.update { it.copy(isLoading = loading) }
    private fun showMessage(message: String) = _uiState.update { it.copy(message = message) }
    private fun showError(error: String) = _uiState.update { it.copy(error = error) }
    fun consumeMessage() = _uiState.update { it.copy(message = null) }
    fun consumeError() = _uiState.update { it.copy(error = null) }

    data class HomeUiState(
        val isLoading: Boolean = false,
        val isStartingProxy: Boolean = false,
        val loadingProgress: String? = null,
        val message: String? = null,
        val error: String? = null
    )
}
