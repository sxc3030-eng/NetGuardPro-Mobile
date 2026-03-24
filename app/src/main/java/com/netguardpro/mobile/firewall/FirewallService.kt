package com.netguardpro.mobile.firewall

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.IBinder
import android.os.ParcelFileDescriptor
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
import java.io.FileInputStream
import java.nio.ByteBuffer

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
        startForeground(NOTIFICATION_ID, createNotification())
        _isActive.value = true
        loadRules()

        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (true) {
                checkConnections()
                delay(5000)
            }
        }
    }

    private fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        _isActive.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun loadRules() {
        scope.launch {
            val db = NetGuardApp.instance.database
            val rulesList = db.firewallRuleDao().getAllRules()
            rules = rulesList.associateBy { it.packageName }
        }
    }

    private fun checkConnections() {
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        val activeNetwork = connectivityManager.activeNetwork ?: return
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return

        val isWifi = capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
        val isCellular = capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)

        rules.forEach { (packageName, rule) ->
            val shouldBlock = when {
                isWifi && !rule.allowWifi -> true
                isCellular && !rule.allowMobile -> true
                else -> false
            }
            if (shouldBlock) {
                _blockedCount.value++
                scope.launch {
                    val db = NetGuardApp.instance.database
                    db.firewallRuleDao().incrementBlockedCount(packageName)
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
        stopMonitoring()
        scope.cancel()
        instance = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.netguardpro.mobile.firewall.START"
        const val ACTION_STOP = "com.netguardpro.mobile.firewall.STOP"
        const val ACTION_UPDATE_RULES = "com.netguardpro.mobile.firewall.UPDATE_RULES"
        private const val NOTIFICATION_ID = 1002

        var instance: FirewallService? = null
            private set
    }
}
