package com.netguardpro.mobile.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "netguardpro_prefs")

class PreferencesManager(private val context: Context) {

    // VPN Preferences
    private val vpnEnabledKey = booleanPreferencesKey("vpn_enabled")
    private val vpnServerKey = stringPreferencesKey("vpn_server")
    private val vpnPrivateKeyKey = stringPreferencesKey("vpn_private_key")
    private val vpnLastConnectedKey = longPreferencesKey("vpn_last_connected")
    private val vpnAutoConnectKey = booleanPreferencesKey("vpn_auto_connect")

    // Firewall Preferences
    private val firewallEnabledKey = booleanPreferencesKey("firewall_enabled")
    private val firewallBlockSystemAppsKey = booleanPreferencesKey("firewall_block_system")
    private val firewallLogEnabledKey = booleanPreferencesKey("firewall_log_enabled")

    // DNS Preferences
    private val dnsEnabledKey = booleanPreferencesKey("dns_enabled")
    private val dnsCustomServerKey = stringPreferencesKey("dns_custom_server")
    private val dnsBlockAdsKey = booleanPreferencesKey("dns_block_ads")
    private val dnsBlockTrackersKey = booleanPreferencesKey("dns_block_trackers")
    private val dnsBlockMalwareKey = booleanPreferencesKey("dns_block_malware")
    private val dnsBlockPhishingKey = booleanPreferencesKey("dns_block_phishing")

    // Cleaner Preferences
    private val cleanerAutoScanKey = booleanPreferencesKey("cleaner_auto_scan")
    private val cleanerScanIntervalKey = intPreferencesKey("cleaner_scan_interval_hours")
    private val cleanerLastScanKey = longPreferencesKey("cleaner_last_scan")

    // General Preferences
    private val onboardingCompleteKey = booleanPreferencesKey("onboarding_complete")
    private val notificationsEnabledKey = booleanPreferencesKey("notifications_enabled")
    private val themeKey = stringPreferencesKey("theme")

    // VPN
    val vpnEnabled: Flow<Boolean> = context.dataStore.data.map { it[vpnEnabledKey] ?: false }
    val vpnServer: Flow<String> = context.dataStore.data.map { it[vpnServerKey] ?: "" }
    val vpnAutoConnect: Flow<Boolean> = context.dataStore.data.map { it[vpnAutoConnectKey] ?: false }

    suspend fun setVpnEnabled(enabled: Boolean) {
        context.dataStore.edit { it[vpnEnabledKey] = enabled }
    }

    suspend fun setVpnServer(server: String) {
        context.dataStore.edit { it[vpnServerKey] = server }
    }

    suspend fun setVpnPrivateKey(key: String) {
        context.dataStore.edit { it[vpnPrivateKeyKey] = key }
    }

    suspend fun getVpnPrivateKey(): Flow<String> =
        context.dataStore.data.map { it[vpnPrivateKeyKey] ?: "" }

    suspend fun setVpnAutoConnect(enabled: Boolean) {
        context.dataStore.edit { it[vpnAutoConnectKey] = enabled }
    }

    // Firewall
    val firewallEnabled: Flow<Boolean> = context.dataStore.data.map { it[firewallEnabledKey] ?: false }
    val firewallLogEnabled: Flow<Boolean> = context.dataStore.data.map { it[firewallLogEnabledKey] ?: true }

    suspend fun setFirewallEnabled(enabled: Boolean) {
        context.dataStore.edit { it[firewallEnabledKey] = enabled }
    }

    suspend fun setFirewallLogEnabled(enabled: Boolean) {
        context.dataStore.edit { it[firewallLogEnabledKey] = enabled }
    }

    // DNS
    val dnsEnabled: Flow<Boolean> = context.dataStore.data.map { it[dnsEnabledKey] ?: false }
    val dnsBlockAds: Flow<Boolean> = context.dataStore.data.map { it[dnsBlockAdsKey] ?: true }
    val dnsBlockTrackers: Flow<Boolean> = context.dataStore.data.map { it[dnsBlockTrackersKey] ?: true }
    val dnsBlockMalware: Flow<Boolean> = context.dataStore.data.map { it[dnsBlockMalwareKey] ?: true }
    val dnsBlockPhishing: Flow<Boolean> = context.dataStore.data.map { it[dnsBlockPhishingKey] ?: true }

    suspend fun setDnsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[dnsEnabledKey] = enabled }
    }

    suspend fun setDnsBlockAds(enabled: Boolean) {
        context.dataStore.edit { it[dnsBlockAdsKey] = enabled }
    }

    suspend fun setDnsBlockTrackers(enabled: Boolean) {
        context.dataStore.edit { it[dnsBlockTrackersKey] = enabled }
    }

    suspend fun setDnsBlockMalware(enabled: Boolean) {
        context.dataStore.edit { it[dnsBlockMalwareKey] = enabled }
    }

    suspend fun setDnsBlockPhishing(enabled: Boolean) {
        context.dataStore.edit { it[dnsBlockPhishingKey] = enabled }
    }

    // Cleaner
    val cleanerAutoScan: Flow<Boolean> = context.dataStore.data.map { it[cleanerAutoScanKey] ?: false }

    suspend fun setCleanerAutoScan(enabled: Boolean) {
        context.dataStore.edit { it[cleanerAutoScanKey] = enabled }
    }

    suspend fun setCleanerLastScan(timestamp: Long) {
        context.dataStore.edit { it[cleanerLastScanKey] = timestamp }
    }

    // General
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[notificationsEnabledKey] ?: true }
    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { it[onboardingCompleteKey] ?: false }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[onboardingCompleteKey] = complete }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[notificationsEnabledKey] = enabled }
    }
}
