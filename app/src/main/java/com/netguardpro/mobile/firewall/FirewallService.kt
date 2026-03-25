package com.netguardpro.mobile.firewall

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.netguardpro.mobile.MainActivity
import com.netguardpro.mobile.NetGuardApp
import com.netguardpro.mobile.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FirewallService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null

    private val _blockedCount = MutableStateFlow(0L)
    val blockedCount: StateFlow<Long> = _blockedCount.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private var rules: Map<String, FirewallRule> = emptyMap()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_START -> {
                startMonitoring()
                START_STICKY
            }
            ACTION_STOP -> {
                stopMonitoring()
                START_NOT_STICKY
            }
            ACTION_UPDATE_RULES -> {
                loadRules()
                START_STICKY
            }
            else -> START_NOT_STICKY
        }
    }

    private fun startMonitoring() {
        try {
            startForeground(NOTIFICATION_ID, createNotification())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground: ${e.message}", e)
            stopSelf()
            return
        }
        _isActive.value = true
        loadRules()

        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (true) {
                try {
                    checkConnections()
                } catch (e: Exception) {
                    Log.e(TAG, "checkConnections error: ${e.message}", e)
                }
                delay(5000)
            }
        }
    }

    private fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        _isActive.value = false
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            Log.e(TAG, "stopForeground error: ${e.message}", e)
        }
        stopSelf()
    }

    private fun loadRules() {
        scope.launch {
            try {
                val db = NetGuardApp.instance.database
                val rulesList = db.firewallRuleDao().getAllRules()
                rules = rulesList.associateBy { it.packageName }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load rules: ${e.message}", e)
            }
        }
    }

    private fun checkConnections() {
        val connectivityManager = getSystemService(ConnectivityManager::class.java) ?: return
        val activeNetwork = connectivityManager.activeNetwork ?: return
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return

        val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

        rules.forEach { (packageName, rule) ->
            val shouldBlock = when {
                isWifi && !rule.allowWifi -> true
                isCellular && !rule.allowMobile -> true
                else -> false
            }
            if (shouldBlock) {
                _blockedCount.value++
                scope.launch {
                    try {
                        val db = NetGuardApp.instance.database
                        db.firewallRuleDao().incrementBlockedCount(packageName)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to increment blocked count: ${e.message}", e)
                    }
                }
            }
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, NetGuardApp.CHANNEL_FIREWALL)
            .setContentTitle("NetGuardPro Firewall Active")
            .setContentText("Monitoring network traffic")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onDestroy() {
        try {
            stopMonitoring()
        } catch (e: Exception) {
            Log.e(TAG, "onDestroy error: ${e.message}", e)
        }
        scope.cancel()
        instance = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "FirewallService"
        const val ACTION_START = "com.netguardpro.mobile.firewall.START"
        const val ACTION_STOP = "com.netguardpro.mobile.firewall.STOP"
        const val ACTION_UPDATE_RULES = "com.netguardpro.mobile.firewall.UPDATE_RULES"
        private const val NOTIFICATION_ID = 1002

        var instance: FirewallService? = null
            private set
    }
}
