package com.netguardpro.mobile.dns

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dns_queries")
data class DnsQueryRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domain: String,
    val queryType: String = "A",
    val blocked: Boolean = false,
    val blocklistSource: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val responseTimeMs: Long = 0,
)

data class DnsDailyStats(
    val date: String,
    val totalQueries: Long,
    val blockedQueries: Long,
    val uniqueDomains: Long,
) {
    val blockPercentage: Float
        get() = if (totalQueries > 0) (blockedQueries.toFloat() / totalQueries) * 100f else 0f
}

data class DnsTopDomain(
    val domain: String,
    val queryCount: Long,
    val blocked: Boolean,
)
