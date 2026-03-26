package com.netguardpro.mobile

import com.netguardpro.mobile.firewall.FirewallRule
import com.netguardpro.mobile.firewall.FirewallUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for FirewallRule data class and FirewallUiState defaults.
 */
class FirewallRuleTest {

    @Test
    fun `default firewall rule allows all traffic`() {
        val rule = FirewallRule(
            packageName = "com.example.app",
            appName = "Example App",
        )
        assertTrue(rule.allowWifi)
        assertTrue(rule.allowMobile)
        assertFalse(rule.isSystemApp)
        assertEquals(0L, rule.blockedCount)
    }

    @Test
    fun `firewall rule copy works correctly`() {
        val rule = FirewallRule(
            packageName = "com.example.app",
            appName = "Example App",
            allowWifi = true,
            allowMobile = true,
        )
        val blocked = rule.copy(allowWifi = false)
        assertFalse(blocked.allowWifi)
        assertTrue(blocked.allowMobile)
        assertEquals(rule.packageName, blocked.packageName)
    }

    @Test
    fun `default ui state is loading`() {
        val state = FirewallUiState()
        assertTrue(state.isLoading)
        assertFalse(state.isEnabled)
        assertTrue(state.rules.isEmpty())
        assertEquals("", state.searchQuery)
        assertEquals(0L, state.blockedTodayCount)
        assertNull(state.errorMessage)
    }

    @Test
    fun `ui state with error`() {
        val state = FirewallUiState(
            isLoading = false,
            errorMessage = "Database failed",
        )
        assertFalse(state.isLoading)
        assertEquals("Database failed", state.errorMessage)
    }

    @Test
    fun `system app flag works`() {
        val rule = FirewallRule(
            packageName = "com.android.system",
            appName = "System",
            isSystemApp = true,
        )
        assertTrue(rule.isSystemApp)
    }

    @Test
    fun `rules sort by system then name`() {
        val rules = listOf(
            FirewallRule(packageName = "com.b", appName = "Beta"),
            FirewallRule(packageName = "com.a", appName = "Alpha"),
            FirewallRule(packageName = "com.system", appName = "System", isSystemApp = true),
        )
        val sorted = rules.sortedWith(compareBy({ it.isSystemApp }, { it.appName.lowercase() }))
        assertEquals("Alpha", sorted[0].appName)
        assertEquals("Beta", sorted[1].appName)
        assertEquals("System", sorted[2].appName)
    }
}
