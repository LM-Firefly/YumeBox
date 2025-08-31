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



package com.github.yumelira.yumebox.feature.meta.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.core.domain.ConnectionHistoryManager
import com.github.yumelira.yumebox.core.model.ConnectionInfo
import com.github.yumelira.yumebox.core.model.ConnectionSnapshot
import com.github.yumelira.yumebox.remote.ServiceClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

enum class ConnectionSort {
    Time,
    Upload,
    Download,
    Host,
}

enum class ConnectionTab {
    ACTIVE,
    CLOSED,
}

data class ConnectionState(
    val snapshot: ConnectionSnapshot? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val sortBy: ConnectionSort = ConnectionSort.Time,
    val selectedTab: ConnectionTab = ConnectionTab.ACTIVE,
    val error: String? = null,
) {
    val totalConnections: Int get() = snapshot?.connections?.size ?: 0
}

class ConnectionViewModel : ViewModel() {

    private val _state = MutableStateFlow(ConnectionState())
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _filteredConnections = MutableStateFlow<List<ConnectionInfo>>(emptyList())
    val filteredConnections: StateFlow<List<ConnectionInfo>> = _filteredConnections.asStateFlow()

    private var pollingJob: Job? = null
    private var _isPolling = false

    init {
        startPolling()
    }

    fun startPolling() {
        if (_isPolling) return
        _isPolling = true

        pollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    refreshConnections()
                } catch (e: Exception) {
                    Timber.w(e, "Failed to poll connections")
                    _state.update { it.copy(error = e.message) }
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun stopPolling() {
        _isPolling = false
        pollingJob?.cancel()
        pollingJob = null
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
        updateFilteredConnections()
    }

    fun setSortBy(sort: ConnectionSort) {
        _state.update { it.copy(sortBy = sort) }
        updateFilteredConnections()
    }

    fun setTab(tab: ConnectionTab) {
        _state.update { it.copy(selectedTab = tab) }
        updateFilteredConnections()
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private suspend fun refreshConnections() {
        withContext(Dispatchers.IO) {
            try {
                val snapshot = ServiceClient.clash().queryConnections()
                if (snapshot != null) {

                    ConnectionHistoryManager.updateConnections(snapshot.connections)

                    _state.update {
                        it.copy(
                            snapshot = snapshot,
                            error = null,
                            isLoading = false,
                        )
                    }
                    updateFilteredConnections()
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to query connections")
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun updateFilteredConnections() {
        val currentState = _state.value

        val connections = when (currentState.selectedTab) {
            ConnectionTab.ACTIVE -> currentState.snapshot?.connections ?: emptyList()
            ConnectionTab.CLOSED -> ConnectionHistoryManager.getClosedConnections()
        }

        val filtered = if (currentState.searchQuery.isEmpty()) {
            connections
        } else {
            val query = currentState.searchQuery.lowercase()
            connections.filter { conn ->
                val host = conn.metadata["host"]?.jsonPrimitive?.content?.lowercase() ?: ""
                val process = conn.metadata["process"]?.jsonPrimitive?.content?.lowercase() ?: ""
                val chains = conn.chains.joinToString(" ").lowercase()
                val rule = conn.rule.lowercase()

                host.contains(query) ||
                    process.contains(query) ||
                    chains.contains(query) ||
                    rule.contains(query)
            }
        }

        val sorted = if (currentState.selectedTab == ConnectionTab.ACTIVE) {
            when (currentState.sortBy) {
                ConnectionSort.Time -> filtered.sortedByDescending { it.start }
                ConnectionSort.Upload -> filtered.sortedByDescending { it.upload }
                ConnectionSort.Download -> filtered.sortedByDescending { it.download }
                ConnectionSort.Host -> {
                    filtered.sortedBy { conn ->
                        conn.metadata["host"]?.jsonPrimitive?.content ?: ""
                    }
                }
            }
        } else {
            filtered
        }

        _filteredConnections.value = sorted
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }

    companion object {
        private const val POLL_INTERVAL_MS = 1000L
    }
}
