package com.netguardpro.mobile.firewall

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "firewall_rules")
data class FirewallRule(
    @PrimaryKey
    val packageName: String,
    val appName: String = "",
    val allowWifi: Boolean = true,
    val allowMobile: Boolean = true,
    val blockedCount: Long = 0,
    val isSystemApp: Boolean = false,
    val lastBlocked: Long = 0,
)

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val uid: Int,
)
