package com.netguardpro.mobile.vpn

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.netguardpro.mobile.data.PreferencesManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VpnConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
}

data class VpnStats(
    val uploadBytes: Long = 0L,
    val downloadBytes: Long = 0L,
    val uploadSpeed: Long = 0L,
    val downloadSpeed: Long = 0L,
    val connectedDurationSeconds: Long = 0L,
)

data class ConnectionHistoryEntry(
    val serverName: String,
    val timestamp: Long,
    val durationSeconds: Long,
    val uploadBytes: Long,
    val downloadBytes: Long,
)

data class VpnUiState(
    val connectionState: VpnConnectionState = VpnConnectionState.DISCONNECTED,
    val selectedServer: VpnServer = VpnServer.DEFAULT_SERVERS.first(),
    val servers: List<VpnServer> = VpnServer.DEFAULT_SERVERS,
    val stats: VpnStats = VpnStats(),
    val assignedIp: String = "",
    val connectionHistory: List<ConnectionHistoryEntry> = emptyList(),
)

class VpnViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)

    private val _uiState = MutableStateFlow(VpnUiState())
    val uiState: StateFlow<VpnUiState> = _uiState.asStateFlow()

    private var statsJob: Job? = null
    private var connectStartTime: Long = 0L

    init {
        loadHistory()
    }

    fun selectServer(server: VpnServer) {
        _uiState.update { it.copy(selectedServer = server) }
    }

    fun connect() {
        val currentState = _uiState.value.connectionState
        if (currentState == VpnConnectionState.CONNECTED || currentState == VpnConnectionState.CONNECTING) return

        _uiState.update { it.copy(connectionState = VpnConnectionState.CONNECTING) }

        viewModelScope.launch {
            delay(1500)
            connectStartTime = System.currentTimeMillis()
            _uiState.update {
                it.copy(
                    connectionState = VpnConnectionState.CONNECTED,
                    assignedIp = "10.0.0.${(2..254).random()}",
                    stats = VpnStats(),
                )
            }
            startStatsTracking()
        }
    }

    fun disconnect() {
        val currentState = _uiState.value.connectionState
        if (currentState == VpnConnectionState.DISCONNECTED || currentState == VpnConnectionState.DISCONNECTING) return

        _uiState.update { it.copy(connectionState = VpnConnectionState.DISCONNECTING) }
        statsJob?.cancel()

        viewModelScope.launch {
            delay(500)
            val stats = _uiState.value.stats
            val entry = ConnectionHistoryEntry(
                serverName = _uiState.value.selectedServer.name,
                timestamp = connectStartTime,
                durationSeconds = stats.connectedDurationSeconds,
                uploadBytes = stats.uploadBytes,
                downloadBytes = stats.downloadBytes,
            )
            _uiState.update {
                it.copy(
                    connectionState = VpnConnectionState.DISCONNECTED,
                    assignedIp = "",
                    stats = VpnStats(),
                    connectionHistory = listOf(entry) + it.connectionHistory.take(49),
                )
            }
        }
    }

    fun toggleConnection() {
        when (_uiState.value.connectionState) {
            VpnConnectionState.DISCONNECTED -> connect()
            VpnConnectionState.CONNECTED -> disconnect()
            else -> {}
        }
    }

    private fun startStatsTracking() {
        statsJob?.cancel()
        statsJob = viewModelScope.launch {
            var totalUp = 0L
            var totalDown = 0L
            while (true) {
                delay(1000)
                val upDelta = (1024L..51200L).random()
                val downDelta = (2048L..102400L).random()
                totalUp += upDelta
                totalDown += downDelta
                val elapsed = (System.currentTimeMillis() - connectStartTime) / 1000

                _uiState.update {
                    it.copy(
                        stats = VpnStats(
                            uploadBytes = totalUp,
                            downloadBytes = totalDown,
                            uploadSpeed = upDelta,
                            downloadSpeed = downDelta,
                            connectedDurationSeconds = elapsed,
                        )
                    )
                }
            }
        }
    }

    private fun loadHistory() {
        _uiState.update {
            it.copy(
                connectionHistory = listOf(
                    ConnectionHistoryEntry("US East", System.currentTimeMillis() - 86400000, 3600, 52428800, 157286400),
                    ConnectionHistoryEntry("EU Central", System.currentTimeMillis() - 172800000, 7200, 104857600, 314572800),
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        statsJob?.cancel()
    }
}
