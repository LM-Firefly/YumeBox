package com.github.yumelira.yumebox.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.clash.config.ClashConfiguration
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.domain.model.Connection
import com.github.yumelira.yumebox.domain.model.ConnectionsSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.withContext

enum class ConnectionSortType {
    Host, Download, Upload, DownloadSpeed, UploadSpeed, Time
}

enum class ConnectionTab {
    Active, Closed
}

class ConnectionsViewModel : ViewModel() {
    companion object {
        private const val TAG = "ConnectionsViewModel"
    }

    private val _activeConnections = MutableStateFlow<List<Connection>>(emptyList())
    private val _closedConnections = MutableStateFlow<List<Connection>>(emptyList())

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()
    private val pollingFlow = isPaused.flatMapLatest { paused ->
        if (!paused) {
            flow<Unit> {
                while (currentCoroutineContext().isActive) {
                    pollConnections()
                    emit(Unit)
                    delay(2000)
                }
            }
        } else {
            flow { 
                emit(Unit)
                kotlinx.coroutines.delay(Long.MAX_VALUE) 
            }
        }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), replay = 1)

    val activeCount: StateFlow<Int> = combine(_activeConnections, pollingFlow) { list, _ -> list.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val closedCount: StateFlow<Int> = combine(_closedConnections, pollingFlow) { list, _ -> list.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortType = MutableStateFlow(ConnectionSortType.DownloadSpeed)
    val sortType: StateFlow<ConnectionSortType> = _sortType.asStateFlow()

    private val _currentTab = MutableStateFlow(ConnectionTab.Active)
    val currentTab: StateFlow<ConnectionTab> = _currentTab.asStateFlow()

    val connections: StateFlow<List<Connection>> = combine(
        _activeConnections, 
        _closedConnections, 
        _searchQuery, 
        _sortType, 
        _currentTab,
        pollingFlow
    ) { args: Array<Any?> ->
        val active = args[0] as List<Connection>
        val closed = args[1] as List<Connection>
        val query = args[2] as String
        val sort = args[3] as ConnectionSortType
        val tab = args[4] as ConnectionTab
        val sourceList = if (tab == ConnectionTab.Active) active else closed
        filterAndSort(sourceList, query, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _downloadTotal = MutableStateFlow(0L)
    val downloadTotal: StateFlow<Long> = _downloadTotal.asStateFlow()

    private val _uploadTotal = MutableStateFlow(0L)
    val uploadTotal: StateFlow<Long> = _uploadTotal.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }
    private var previousConnectionsMap = ConcurrentHashMap<String, Connection>()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSortType(type: ConnectionSortType) {
        _sortType.value = type
    }

    fun updateTab(tab: ConnectionTab) {
        _currentTab.value = tab
    }

    fun togglePause() {
        _isPaused.value = !_isPaused.value
    }

    private fun filterAndSort(list: List<Connection>, query: String, sort: ConnectionSortType): List<Connection> {
        val filtered = if (query.isEmpty()) {
            list
        } else {
            list.filter { 
                it.metadata.host.contains(query, ignoreCase = true) ||
                it.metadata.destinationIP.contains(query, ignoreCase = true) ||
                it.rule.contains(query, ignoreCase = true) ||
                it.chains.any { chain -> chain.contains(query, ignoreCase = true) }
            }
        }
        return when (sort) {
            ConnectionSortType.Host -> filtered.sortedBy { it.metadata.host.ifEmpty { it.metadata.destinationIP } }
            ConnectionSortType.Download -> filtered.sortedByDescending { it.download }
            ConnectionSortType.Upload -> filtered.sortedByDescending { it.upload }
            ConnectionSortType.DownloadSpeed -> filtered.sortedByDescending { it.downloadSpeed }
            ConnectionSortType.UploadSpeed -> filtered.sortedByDescending { it.uploadSpeed }
            ConnectionSortType.Time -> filtered.sortedByDescending { it.start }
        }
    }

    private suspend fun pollConnections() = withContext(Dispatchers.IO) {
        try {
            val sessionOverride = Clash.queryOverride(Clash.OverrideSlot.Session)
            val config = Clash.queryConfiguration()
            val controller = sessionOverride.externalController 
                ?: config.externalController 
                ?: "127.0.0.1:9090"
            val secret = sessionOverride.secret 
                ?: config.secret 
                ?: ClashConfiguration.API_SECRET
            val url = URL("http://$controller/connections")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            if (secret.isNotEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer $secret")
            }
            connection.connectTimeout = 1000
            connection.readTimeout = 1000
            if (connection.responseCode == 200) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val snapshot = json.decodeFromString<ConnectionsSnapshot>(jsonString)
                val currentMap = HashMap<String, Connection>()
                val newActiveList = (snapshot.connections ?: emptyList()).map { conn ->
                    val prev = previousConnectionsMap[conn.id]
                    if (prev != null) {
                        conn.downloadSpeed = conn.download - prev.download
                        conn.uploadSpeed = conn.upload - prev.upload
                    }
                    currentMap[conn.id] = conn
                    conn
                }
                // Detect closed connections
                val closed = previousConnectionsMap.keys.filter { !currentMap.containsKey(it) }
                if (closed.isNotEmpty()) {
                    val newlyClosed = closed.mapNotNull { previousConnectionsMap[it] }
                    val currentClosed = _closedConnections.value.toMutableList()
                    currentClosed.addAll(0, newlyClosed)
                    if (currentClosed.size > 100) {
                        _closedConnections.value = currentClosed.take(100)
                    } else {
                        _closedConnections.value = currentClosed
                    }
                }
                previousConnectionsMap.clear()
                previousConnectionsMap.putAll(currentMap)
                _activeConnections.value = newActiveList
                _downloadTotal.value = snapshot.downloadTotal
                _uploadTotal.value = snapshot.uploadTotal
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error polling connections")
        }
    }

    fun closeConnection(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sessionOverride = Clash.queryOverride(Clash.OverrideSlot.Session)
                val config = Clash.queryConfiguration()
                val controller = sessionOverride.externalController ?: config.externalController ?: "127.0.0.1:9090"
                val secret = sessionOverride.secret ?: config.secret ?: ClashConfiguration.API_SECRET
                val url = URL("http://$controller/connections/$id")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "DELETE"
                if (secret.isNotEmpty()) {
                    connection.setRequestProperty("Authorization", "Bearer $secret")
                }
                connection.connectTimeout = 1000
                connection.responseCode
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error closing connection $id")
            }
        }
    }

    fun closeAllConnections() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sessionOverride = Clash.queryOverride(Clash.OverrideSlot.Session)
                val config = Clash.queryConfiguration()
                val controller = sessionOverride.externalController ?: config.externalController ?: "127.0.0.1:9090"
                val secret = sessionOverride.secret ?: config.secret ?: ClashConfiguration.API_SECRET
                val url = URL("http://$controller/connections")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "DELETE"
                if (secret.isNotEmpty()) {
                    connection.setRequestProperty("Authorization", "Bearer $secret")
                }
                connection.connectTimeout = 1000
                connection.responseCode
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error closing all connections")
            }
        }
    }
}
