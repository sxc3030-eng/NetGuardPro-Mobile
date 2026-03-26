package com.netguardpro.mobile.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.netguardpro.mobile.dns.DnsQueryRecord
import com.netguardpro.mobile.firewall.FirewallRule

@Entity(tableName = "connection_logs")
data class ConnectionLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val remoteAddress: String,
    val remotePort: Int,
    val protocol: String = "TCP",
    val allowed: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val bytesTransferred: Long = 0,
)

@Entity(tableName = "clean_history")
data class CleanHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String,
    val itemsCleaned: Int,
    val bytesFreed: Long,
    val timestamp: Long = System.currentTimeMillis(),
)

@Dao
interface FirewallRuleDao {
    @Query("SELECT * FROM firewall_rules ORDER BY appName ASC")
    suspend fun getAllRules(): List<FirewallRule>

    @Query("SELECT * FROM firewall_rules WHERE packageName = :packageName")
    suspend fun getRule(packageName: String): FirewallRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(rule: FirewallRule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<FirewallRule>)

    @Query("DELETE FROM firewall_rules WHERE packageName = :packageName")
    suspend fun deleteRule(packageName: String)

    @Query("UPDATE firewall_rules SET blockedCount = blockedCount + 1, lastBlocked = :timestamp WHERE packageName = :packageName")
    suspend fun incrementBlockedCount(packageName: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT SUM(blockedCount) FROM firewall_rules")
    suspend fun getTotalBlockedCount(): Long?

    @Query("SELECT * FROM firewall_rules WHERE allowWifi = 0 OR allowMobile = 0")
    suspend fun getBlockedApps(): List<FirewallRule>
}

@Dao
interface ConnectionLogDao {
    @Query("SELECT * FROM connection_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int = 100): List<ConnectionLog>

    @Insert
    suspend fun insert(log: ConnectionLog)

    @Query("SELECT COUNT(*) FROM connection_logs WHERE allowed = 0 AND timestamp > :since")
    suspend fun getBlockedCountSince(since: Long): Long

    @Query("DELETE FROM connection_logs WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("SELECT COUNT(*) FROM connection_logs WHERE timestamp > :since")
    suspend fun getTotalCountSince(since: Long): Long
}

@Dao
interface DnsQueryDao {
    @Query("SELECT * FROM dns_queries ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentQueries(limit: Int = 100): List<DnsQueryRecord>

    @Insert
    suspend fun insert(query: DnsQueryRecord)

    @Query("SELECT COUNT(*) FROM dns_queries WHERE timestamp > :since")
    suspend fun getQueryCountSince(since: Long): Long

    @Query("SELECT COUNT(*) FROM dns_queries WHERE blocked = 1 AND timestamp > :since")
    suspend fun getBlockedCountSince(since: Long): Long

    @Query("DELETE FROM dns_queries WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}

@Dao
interface CleanHistoryDao {
    @Query("SELECT * FROM clean_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentHistory(limit: Int = 50): List<CleanHistory>

    @Insert
    suspend fun insert(entry: CleanHistory)

    @Query("SELECT SUM(bytesFreed) FROM clean_history")
    suspend fun getTotalBytesFreed(): Long?

    @Query("DELETE FROM clean_history WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}

@Database(
    entities = [
        FirewallRule::class,
        ConnectionLog::class,
        DnsQueryRecord::class,
        CleanHistory::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun firewallRuleDao(): FirewallRuleDao
    abstract fun connectionLogDao(): ConnectionLogDao
    abstract fun dnsQueryDao(): DnsQueryDao
    abstract fun cleanHistoryDao(): CleanHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "netguardpro.db",
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    // If first attempt fails, try deleting and recreating
                    try {
                        context.applicationContext.deleteDatabase("netguardpro.db")
                        val instance = Room.databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            "netguardpro.db",
                        )
                            .fallbackToDestructiveMigration()
                            .build()
                        INSTANCE = instance
                        instance
                    } catch (e2: Exception) {
                        throw e2
                    }
                }
            }
        }
    }
}
