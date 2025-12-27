package com.github.yumelira.yumebox.presentation.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.repository.IpMonitoringState
import com.github.yumelira.yumebox.domain.model.Connection
import java.net.URL
import java.net.HttpURLConnection
import kotlinx.serialization.json.Json
import com.github.yumelira.yumebox.domain.model.ConnectionsSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import com.github.yumelira.yumebox.clash.config.Configuration
import com.github.yumelira.yumebox.data.repository.NetworkInfoService
import com.github.yumelira.yumebox.data.repository.ProxyChainResolver
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import com.github.yumelira.yumebox.domain.facade.ProfilesRepository
import com.github.yumelira.yumebox.domain.facade.ProxyFacade
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModel(
    application: Application,
    private val proxyFacade: ProxyFacade,
    private val profilesRepository: ProfilesRepository,
    appSettingsStorage: AppSettingsStorage,
    private val clashManager: ClashManager,
    private val networkInfoService: NetworkInfoService,
    private val proxyChainResolver: ProxyChainResolver
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "HomeViewModel"
        private val SUSPICIOUS_TYPE_LOGGED = AtomicBoolean(false)
    }

    private val json = Json { ignoreUnknownKeys = true }

    val profiles: StateFlow<List<Profile>> = profilesRepository.profiles
    val recommendedProfile: StateFlow<Profile?> = profilesRepository.recommendedProfile
    val hasEnabledProfile: Flow<Boolean> = profiles.map { it.any { profile -> profile.enabled } }

    val isRunning = proxyFacade.isRunning
    val currentProfile = proxyFacade.currentProfile
    val trafficNow = proxyFacade.trafficNow
    val proxyGroups = proxyFacade.proxyGroups
    val tunnelState = proxyFacade.tunnelState
    val connections: StateFlow<List<Connection>> = isRunning.flatMapLatest { running ->
        if (running) {
            flow {
                while (true) {
                    try {
                        val (controller, secret) = Configuration.getSmartControllerAndSecret()
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
                            val rawConnections = snapshot.connections ?: emptyList()
                            val suspiciousFound = rawConnections.any { conn ->
                                conn.metadata.type.matches(Regex("^n\\d+$"))
                            }
                            if (suspiciousFound && SUSPICIOUS_TYPE_LOGGED.compareAndSet(false, true)) {
                                Timber.tag(TAG).w("Suspicious metadata.type detected in connections; dumping JSON for analysis")
                                Timber.tag(TAG).d(jsonString)
                            }
                            val sanitized = rawConnections.map { conn ->
                                conn.copy(
                                    metadata = conn.metadata.copy(
                                        sourceIP = conn.metadata.sourceIP.ifBlank { "<unknown>" },
                                        destinationIP = conn.metadata.destinationIP.ifBlank { "<unknown>" },
                                        host = conn.metadata.host.ifBlank { "<unknown>" },
                                        process = conn.metadata.process.ifBlank { "" }
                                    ),
                                    chains = conn.chains.map { it.ifBlank { "<unknown>" } },
                                    rule = conn.rule.ifBlank { "<unknown>" },
                                    rulePayload = conn.rulePayload
                                )
                            }.take(200) // limit to avoid huge lists
                            emit(sanitized)
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

    private val _speedHistory = MutableStateFlow<List<Long>>(emptyList())
    val speedHistory: StateFlow<List<Long>> = _speedHistory.asStateFlow()

    private val mainProxyNode: StateFlow<com.github.yumelira.yumebox.core.model.Proxy?> =
        combine(isRunning, proxyGroups) { running, groups ->
            if (!running || groups.isEmpty()) return@combine null
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
        node?.let {
            proxyFacade.getCachedDelay(it.name)?.takeIf { d -> d > 0 } ?: it.delay.takeIf { d -> d > 0 }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val ipMonitoringState: StateFlow<IpMonitoringState> = isRunning.flatMapLatest { running ->
        if (running) {
            networkInfoService.startIpMonitoring(isRunning)
        } else {
            flowOf(IpMonitoringState.Loading)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IpMonitoringState.Loading)

    private val _recentLogs = MutableStateFlow<List<LogMessage>>(emptyList())

    init {
        syncDisplayState()
        subscribeToLogs()
        startSpeedSampling()
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
            // 从 ProfilesStore 获取配置
            val profile = profilesRepository.profiles.value.find { it.id == profileId }
            if (profile == null) {
                showError(MLang.Home.Message.ConfigSwitchFailed.format(MLang.ProfilesVM.Error.ProfileNotExist))
                return
            }

            // 重新加载配置
            val result = clashManager.loadProfile(profile)
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

    private fun startSpeedSampling(sampleLimit: Int = 24) {
        viewModelScope.launch {
            flow {
                while (true) {
                    emit(proxyFacade.trafficNow.value.download.coerceAtLeast(0L))
                    kotlinx.coroutines.delay(1000L)
                }
            }.catch { }.collect { sample ->
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

    data class HomeUiState(
        val isLoading: Boolean = false,
        val isStartingProxy: Boolean = false,
        val loadingProgress: String? = null,
        val message: String? = null,
        val error: String? = null
    )
}
