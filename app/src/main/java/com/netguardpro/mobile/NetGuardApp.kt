package com.netguardpro.mobile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.netguardpro.mobile.data.AppDatabase

class NetGuardApp : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        val vpnChannel = NotificationChannel(
            CHANNEL_VPN,
            "VPN Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows VPN connection status"
            setShowBadge(false)
        }

        val firewallChannel = NotificationChannel(
            CHANNEL_FIREWALL,
            "Firewall Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows firewall monitoring status"
            setShowBadge(false)
        }

        val alertsChannel = NotificationChannel(
            CHANNEL_ALERTS,
            "Security Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Important security notifications"
        }

        manager.createNotificationChannels(listOf(vpnChannel, firewallChannel, alertsChannel))
    }

    companion object {
        const val CHANNEL_VPN = "vpn_service"
        const val CHANNEL_FIREWALL = "firewall_service"
        const val CHANNEL_ALERTS = "security_alerts"

        lateinit var instance: NetGuardApp
            private set
    }
}
