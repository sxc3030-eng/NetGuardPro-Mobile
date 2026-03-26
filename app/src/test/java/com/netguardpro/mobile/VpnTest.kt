package com.netguardpro.mobile

import com.netguardpro.mobile.vpn.ConnectionHistoryEntry
import com.netguardpro.mobile.vpn.VpnConnectionState
import com.netguardpro.mobile.vpn.VpnServer
import com.netguardpro.mobile.vpn.VpnStats
import com.netguardpro.mobile.vpn.VpnUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for VPN data classes and state management.
 */
class VpnTest {

    @Test
    fun `default vpn state is disconnected`() {
        val state = VpnUiState()
        assertEquals(VpnConnectionState.DISCONNECTED, state.connectionState)
        assertEquals("", state.assignedIp)
        assertTrue(state.connectionHistory.isEmpty())
    }

    @Test
    fun `vpn stats default is zero`() {
        val stats = VpnStats()
        assertEquals(0L, stats.uploadBytes)
        assertEquals(0L, stats.downloadBytes)
        assertEquals(0L, stats.uploadSpeed)
        assertEquals(0L, stats.downloadSpeed)
        assertEquals(0L, stats.connectedDurationSeconds)
    }

    @Test
    fun `default servers list not empty`() {
        assertTrue(VpnServer.DEFAULT_SERVERS.isNotEmpty())
    }

    @Test
    fun `connection state enum values`() {
        val states = VpnConnectionState.entries
        assertEquals(4, states.size)
        assertTrue(states.contains(VpnConnectionState.DISCONNECTED))
        assertTrue(states.contains(VpnConnectionState.CONNECTING))
        assertTrue(states.contains(VpnConnectionState.CONNECTED))
        assertTrue(states.contains(VpnConnectionState.DISCONNECTING))
    }

    @Test
    fun `connection history entry creation`() {
        val entry = ConnectionHistoryEntry(
            serverName = "US East",
            timestamp = System.currentTimeMillis(),
            durationSeconds = 3600,
            uploadBytes = 1024 * 1024,
            downloadBytes = 10 * 1024 * 1024,
        )
        assertEquals("US East", entry.serverName)
        assertEquals(3600L, entry.durationSeconds)
    }

    @Test
    fun `vpn state transition to connected`() {
        val state = VpnUiState()
        val connected = state.copy(
            connectionState = VpnConnectionState.CONNECTED,
            assignedIp = "10.0.0.2",
        )
        assertEquals(VpnConnectionState.CONNECTED, connected.connectionState)
        assertEquals("10.0.0.2", connected.assignedIp)
    }
}
