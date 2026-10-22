package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.VpnServer
import com.example.data.repository.ServerSortMode
import com.example.data.repository.VpnRepository
import com.example.vpn.ProximityVpnService
import com.example.vpn.VpnConnectionStatus
import com.example.vpn.VpnSessionStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VpnViewModel(
    private val repository: VpnRepository = VpnRepository()
) : ViewModel() {

    private val _servers = MutableStateFlow<List<VpnServer>>(emptyList())
    val servers: StateFlow<List<VpnServer>> = _servers.asStateFlow()

    private val _selectedServer = MutableStateFlow<VpnServer?>(null)
    val selectedServer: StateFlow<VpnServer?> = _selectedServer.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortMode = MutableStateFlow(ServerSortMode.LOWEST_PING)
    val sortMode: StateFlow<ServerSortMode> = _sortMode.asStateFlow()

    private val _livePing = MutableStateFlow<Int?>(null)
    val livePing: StateFlow<Int?> = _livePing.asStateFlow()

    private val _isTestingPing = MutableStateFlow(false)
    val isTestingPing: StateFlow<Boolean> = _isTestingPing.asStateFlow()

    val connectionStatus: StateFlow<VpnConnectionStatus> = ProximityVpnService.status
    val sessionStats: StateFlow<VpnSessionStats> = ProximityVpnService.sessionStats

    // Filtered and sorted servers stream
    val filteredServers: StateFlow<List<VpnServer>> = combine(
        _servers,
        _searchQuery,
        _sortMode
    ) { serverList, query, sort ->
        val filtered = if (query.isBlank()) {
            serverList
        } else {
            val q = query.trim().lowercase()
            serverList.filter {
                it.countryLong.lowercase().contains(q) ||
                it.countryShort.lowercase().contains(q) ||
                it.ip.contains(q) ||
                it.hostName.lowercase().contains(q)
            }
        }
        repository.sortServers(filtered, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadServers()
    }

    fun loadServers(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val list = repository.getServers(forceRefresh)
            _servers.value = list

            if (_selectedServer.value == null && list.isNotEmpty()) {
                val best = repository.findBestServer(list)
                _selectedServer.value = best
                best?.let { testLivePing(it) }
            }
            _isRefreshing.value = false
        }
    }

    fun selectServer(server: VpnServer) {
        _selectedServer.value = server
        testLivePing(server)
    }

    fun autoSelectBestServer() {
        val currentList = _servers.value
        if (currentList.isNotEmpty()) {
            val best = repository.findBestServer(currentList)
            if (best != null) {
                _selectedServer.value = best
                testLivePing(best)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortMode(mode: ServerSortMode) {
        _sortMode.value = mode
    }

    fun testLivePing(server: VpnServer) {
        viewModelScope.launch {
            _isTestingPing.value = true
            val measured = repository.measureRealPing(server.ip)
            _livePing.value = if (measured > 0) measured else server.ping
            _isTestingPing.value = false
        }
    }

    fun toggleVpn(context: Context, onRequirePermission: () -> Unit) {
        when (connectionStatus.value) {
            VpnConnectionStatus.CONNECTED, VpnConnectionStatus.CONNECTING -> {
                ProximityVpnService.stopVpn(context)
            }
            VpnConnectionStatus.DISCONNECTED, VpnConnectionStatus.ERROR -> {
                val server = _selectedServer.value
                if (server != null) {
                    onRequirePermission()
                }
            }
            VpnConnectionStatus.DISCONNECTING -> {
                // Wait for disconnection to finish
            }
        }
    }

    fun startVpnWithSelected(context: Context) {
        val server = _selectedServer.value ?: return
        ProximityVpnService.startVpn(context, server)
    }
}
