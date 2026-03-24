package com.netguardpro.mobile.firewall

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.netguardpro.mobile.NetGuardApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FirewallUiState(
    val isEnabled: Boolean = false,
    val rules: List<FirewallRule> = emptyList(),
    val searchQuery: String = "",
    val blockedTodayCount: Long = 0,
    val isLoading: Boolean = true,
    val showSystemApps: Boolean = false,
)

class FirewallViewModel(application: Application) : AndroidViewModel(application) {

    private val db = NetGuardApp.instance.database
    private val dao = db.firewallRuleDao()

    private val _uiState = MutableStateFlow(FirewallUiState())
    val uiState: StateFlow<FirewallUiState> = _uiState.asStateFlow()

    private var allRules: List<FirewallRule> = emptyList()

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }

            val pm = getApplication<Application>().packageManager
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            val existingRules = dao.getAllRules().associateBy { it.packageName }

            val rules = installedApps
                .filter { it.flags and ApplicationInfo.FLAG_HAS_CODE != 0 }
                .map { appInfo ->
                    val packageName = appInfo.packageName
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                    existingRules[packageName] ?: FirewallRule(
                        packageName = packageName,
                        appName = appName,
                        allowWifi = true,
                        allowMobile = true,
                        isSystemApp = isSystem,
                    )
                }
                .sortedWith(compareBy({ it.isSystemApp }, { it.appName.lowercase() }))

            allRules = rules
            val totalBlocked = rules.sumOf { it.blockedCount }

            _uiState.update {
                it.copy(
                    rules = filterRules(rules, it.searchQuery, it.showSystemApps),
                    blockedTodayCount = totalBlocked,
                    isLoading = false,
                )
            }
        }
    }

    fun toggleFirewall(enabled: Boolean) {
        _uiState.update { it.copy(isEnabled = enabled) }
    }

    fun updateSearch(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                rules = filterRules(allRules, query, it.showSystemApps),
            )
        }
    }

    fun toggleShowSystemApps(show: Boolean) {
        _uiState.update {
            it.copy(
                showSystemApps = show,
                rules = filterRules(allRules, it.searchQuery, show),
            )
        }
    }

    fun toggleWifi(packageName: String, allowed: Boolean) {
        updateRule(packageName) { it.copy(allowWifi = allowed) }
    }

    fun toggleMobile(packageName: String, allowed: Boolean) {
        updateRule(packageName) { it.copy(allowMobile = allowed) }
    }

    private fun updateRule(packageName: String, transform: (FirewallRule) -> FirewallRule) {
        viewModelScope.launch(Dispatchers.IO) {
            allRules = allRules.map { rule ->
                if (rule.packageName == packageName) {
                    val updated = transform(rule)
                    dao.insertOrUpdate(updated)
                    updated
                } else {
                    rule
                }
            }
            _uiState.update {
                it.copy(rules = filterRules(allRules, it.searchQuery, it.showSystemApps))
            }
        }
    }

    private fun filterRules(
        rules: List<FirewallRule>,
        query: String,
        showSystem: Boolean,
    ): List<FirewallRule> {
        return rules.filter { rule ->
            (showSystem || !rule.isSystemApp) &&
                (query.isEmpty() || rule.appName.contains(query, ignoreCase = true) || rule.packageName.contains(query, ignoreCase = true))
        }
    }
}
