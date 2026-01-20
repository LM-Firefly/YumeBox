package com.github.yumelira.yumebox.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.clash.config.Configuration
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.domain.model.Connection
import com.github.yumelira.yumebox.domain.model.ConnectionsSnapshot
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import timber.log.Timber

enum class ConnectionSortType {
    Host, Download, Upload, DownloadSpeed, UploadSpeed, Time
}

enum class ConnectionTab {
    Active, Closed
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ConnectionsViewModel(private val clashManager: com.github.yumelira.yumebox.clash.manager.ClashManager) : ViewModel() {
    companion object {
        private const val TAG = "ConnectionsViewModel"
    }
    private val _activeConnections = MutableStateFlow<List<Connection>>(emptyList())
    private val _closedConnections = MutableStateFlow<List<Connection>>(emptyList())
    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()
    val activeCount: StateFlow<Int> = _activeConnections.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val closedCount: StateFlow<Int> = _closedConnections.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val _sortType = MutableStateFlow(ConnectionSortType.DownloadSpeed)
    val sortType: StateFlow<ConnectionSortType> = _sortType.asStateFlow()
    private val _isAscending = MutableStateFlow(false)
    val isAscending: StateFlow<Boolean> = _isAscending.asStateFlow()
    private val _currentTab = MutableStateFlow(ConnectionTab.Active)
    val currentTab: StateFlow<ConnectionTab> = _currentTab.asStateFlow()
    private val sourceListFlow = combine(_activeConnections, _closedConnections, _currentTab) { active, closed, tab ->
        if (tab == ConnectionTab.Active) active else closed
    }
    private val filteredConnections = combine(sourceListFlow, _searchQuery, _sortType, _isAscending) { list, query, sort, ascending ->
        filterAndSort(list, query, sort, ascending)
    }
    val connections: StateFlow<List<Connection>> = filteredConnections.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeConnections: StateFlow<List<Connection>> = combine(_activeConnections, _searchQuery, _sortType, _isAscending) { list, query, sort, ascending ->
        filterAndSort(list, query, sort, ascending)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val closedConnections: StateFlow<List<Connection>> = combine(_closedConnections, _searchQuery, _sortType, _isAscending) { list, query, sort, ascending ->
        filterAndSort(list, query, sort, ascending)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _downloadTotal = MutableStateFlow(0L)
    val downloadTotal: StateFlow<Long> = _downloadTotal.asStateFlow()
    private val _uploadTotal = MutableStateFlow(0L)
    val uploadTotal: StateFlow<Long> = _uploadTotal.asStateFlow()
    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()
    private val json = Json { ignoreUnknownKeys = true }
    private var previousConnectionsMap = ConcurrentHashMap<String, Connection>()
    private val SUSPICIOUS_TYPE_LOGGED = java.util.concurrent.atomic.AtomicBoolean(false)
    init {
        // Use merged polling+subscription flow directly to avoid cases where subscription produces only initial snapshot and never subsequent events.
        viewModelScope.launch(Dispatchers.IO) {
            try {
                clashManager.createConnectionsFlow().collect { snapshot ->
                    handleSnapshot(snapshot)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Timber.tag(TAG).w(e, "Connections flow cancelled, aborting")
                    return@launch
                }
                Timber.tag(TAG).e(e, "Connections flow failed")
                _connectionError.value = e.message ?: "Unknown error"
            }
        }
    }
    private fun handleSnapshot(snapshot: ConnectionsSnapshot) {
        try {
            val currentMap = HashMap<String, Connection>()
            val newActiveList = (snapshot.connections ?: emptyList()).mapNotNull { it }
            newActiveList.forEach { sanitized ->
                val prev = previousConnectionsMap[sanitized.id]
                if (prev != null) {
                    sanitized.downloadSpeed = sanitized.download - prev.download
                    sanitized.uploadSpeed = sanitized.upload - prev.upload
                }
                currentMap[sanitized.id] = sanitized
            }
            val closed = previousConnectionsMap.keys.filter { !currentMap.containsKey(it) }
            if (closed.isNotEmpty()) {
                val newlyClosed = closed.mapNotNull { previousConnectionsMap[it] }
                val currentClosed = _closedConnections.value.toMutableList()
                currentClosed.addAll(0, newlyClosed)
                _closedConnections.value = if (currentClosed.size > 100) currentClosed.take(100) else currentClosed
            }
            previousConnectionsMap.clear()
            previousConnectionsMap.putAll(currentMap)
            _activeConnections.value = newActiveList
            _downloadTotal.value = snapshot.downloadTotal
            _uploadTotal.value = snapshot.uploadTotal
            _connectionError.value = null
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error handling shared connections snapshot")
            _connectionError.value = e.message ?: "Unknown error"
        }
    }
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    fun updateSortType(type: ConnectionSortType) {
        if (_sortType.value == type) {
            _isAscending.value = !_isAscending.value
        } else {
            _sortType.value = type
            _isAscending.value = false
        }
    }
    fun updateTab(tab: ConnectionTab) {
        _currentTab.value = tab
    }
    fun togglePause() {
        _isPaused.value = !_isPaused.value
    }
    private fun filterAndSort(list: List<Connection>, query: String, sort: ConnectionSortType, ascending: Boolean): List<Connection> {
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
        val sorted = when (sort) {
            ConnectionSortType.Host -> filtered.sortedBy { it.metadata.host.ifEmpty { it.metadata.destinationIP } }
            ConnectionSortType.Download -> filtered.sortedBy { it.download }
            ConnectionSortType.Upload -> filtered.sortedBy { it.upload }
            ConnectionSortType.DownloadSpeed -> filtered.sortedBy { it.downloadSpeed }
            ConnectionSortType.UploadSpeed -> filtered.sortedBy { it.uploadSpeed }
            ConnectionSortType.Time -> filtered.sortedBy { it.start }
        }
        return if (ascending) sorted else sorted.reversed()
    }
    fun closeConnection(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = Clash.closeConnection(id)
                if (!success) {
                    Timber.tag(TAG).e("Error closing connection via core: %s", id)
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error closing connection $id")
            }
        }
    }
    fun closeAllConnections() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = Clash.closeAllConnections()
                if (!success) {
                    Timber.tag(TAG).e("Error closing all connections via core")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error closing all connections")
            }
        }
    }
}
