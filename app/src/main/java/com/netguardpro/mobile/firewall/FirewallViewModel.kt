package com.netguardpro.mobile.firewall

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
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
    val errorMessage: String? = null,
)

class FirewallViewModel(application: Application) : AndroidViewModel(application) {

    private var db: Any? = null
    private var dao: Any? = null

    private val _uiState = MutableStateFlow(FirewallUiState())
    val uiState: StateFlow<FirewallUiState> = _uiState.asStateFlow()

    private var allRules: List<FirewallRule> = emptyList()

    init {
        initDatabase()
        loadInstalledApps()
    }

    private fun initDatabase() {
        try {
            val appDb = NetGuardApp.instance.database ?: return
            db = appDb
            dao = appDb.firewallRuleDao()
        } catch (e: Exception) {
            Log.e(TAG, "Database init failed: ${e.message}", e)
            db = null
            dao = null
        }
    }

    private fun getDao(): com.netguardpro.mobile.data.FirewallRuleDao? {
        return dao as? com.netguardpro.mobile.data.FirewallRuleDao
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val pm = getApplication<Application>().packageManager
                val installedApps = try {
                    pm.getInstalledApplications(PackageManager.GET_META_DATA)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get installed apps: ${e.message}", e)
                    // Fallback: try without META_DATA flag
                    try {
                        pm.getInstalledApplications(0)
                    } catch (e2: Exception) {
                        Log.e(TAG, "Fallback also failed: ${e2.message}", e2)
                        emptyList()
                    }
                }

                // Load existing rules from database (safely)
                val existingRules: Map<String, FirewallRule> = try {
                    getDao()?.getAllRules()?.associateBy { it.packageName } ?: emptyMap()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load rules from DB: ${e.message}", e)
                    emptyMap()
                }

                val rules = installedApps
                    .filter {
                        try {
                            it.flags and ApplicationInfo.FLAG_HAS_CODE != 0
                        } catch (e: Exception) {
                            false
                        }
                    }
                    .mapNotNull { appInfo ->
                        try {
                            val packageName = appInfo.packageName
                            val appName = try {
                                pm.getApplicationLabel(appInfo).toString()
                            } catch (e: Exception) {
                                packageName
                            }
                            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                            existingRules[packageName] ?: FirewallRule(
                                packageName = packageName,
                                appName = appName,
                                allowWifi = true,
                                allowMobile = true,
                                isSystemApp = isSystem,
                            )
                        } catch (e: Exception) {
                            Log.w(TAG, "Skipping app: ${e.message}")
                            null
                        }
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
            } catch (e: Exception) {
                Log.e(TAG, "loadInstalledApps failed: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load apps: ${e.message}",
                        rules = emptyList(),
                    )
                }
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
            try {
                allRules = allRules.map { rule ->
                    if (rule.packageName == packageName) {
                        val updated = transform(rule)
                        try {
                            getDao()?.insertOrUpdate(updated)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to save rule: ${e.message}", e)
                        }
                        updated
                    } else {
                        rule
                    }
                }
                _uiState.update {
                    it.copy(rules = filterRules(allRules, it.searchQuery, it.showSystemApps))
                }
            } catch (e: Exception) {
                Log.e(TAG, "updateRule failed: ${e.message}", e)
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

    companion object {
        private const val TAG = "FirewallViewModel"

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = extras[APPLICATION_KEY] as Application
                return FirewallViewModel(application) as T
            }
        }
    }
}
