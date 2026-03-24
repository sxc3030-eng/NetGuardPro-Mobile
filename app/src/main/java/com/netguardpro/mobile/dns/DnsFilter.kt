package com.netguardpro.mobile.dns

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class DnsFilter {

    private val blockedDomains = HashSet<String>(100_000)
    private val whitelist = HashSet<String>()
    private val blacklist = HashSet<String>()
    private val mutex = Mutex()

    private var totalQueries = 0L
    private var blockedQueries = 0L

    suspend fun loadBlocklist(domains: List<String>) {
        mutex.withLock {
            blockedDomains.addAll(domains)
        }
    }

    suspend fun addToWhitelist(domain: String) {
        mutex.withLock {
            whitelist.add(domain.lowercase())
            blacklist.remove(domain.lowercase())
        }
    }

    suspend fun addToBlacklist(domain: String) {
        mutex.withLock {
            blacklist.add(domain.lowercase())
            whitelist.remove(domain.lowercase())
        }
    }

    suspend fun removeFromWhitelist(domain: String) {
        mutex.withLock { whitelist.remove(domain.lowercase()) }
    }

    suspend fun removeFromBlacklist(domain: String) {
        mutex.withLock { blacklist.remove(domain.lowercase()) }
    }

    suspend fun shouldBlock(domain: String): Boolean {
        val normalized = domain.lowercase().trimEnd('.')
        totalQueries++

        mutex.withLock {
            if (whitelist.contains(normalized)) return false
            if (blacklist.contains(normalized)) {
                blockedQueries++
                return true
            }

            if (blockedDomains.contains(normalized)) {
                blockedQueries++
                return true
            }

            // Check parent domains for wildcard blocking
            val parts = normalized.split(".")
            for (i in 1 until parts.size) {
                val parent = parts.subList(i, parts.size).joinToString(".")
                if (blockedDomains.contains(parent)) {
                    blockedQueries++
                    return true
                }
            }
        }

        return false
    }

    fun processDnsPacket(packet: ByteArray): ByteArray? {
        if (packet.size < 12) return null

        val buffer = ByteBuffer.wrap(packet)
        val transactionId = buffer.short
        val flags = buffer.short
        val questionCount = buffer.short.toInt() and 0xFFFF

        if (questionCount < 1) return null

        // Skip to question section
        buffer.position(12)

        val domain = parseDomainName(buffer)
        val qType = if (buffer.remaining() >= 2) buffer.short else return null
        val qClass = if (buffer.remaining() >= 2) buffer.short else return null

        return null // Return null means don't block, handled by caller
    }

    fun createNxDomainResponse(queryPacket: ByteArray): ByteArray {
        if (queryPacket.size < 12) return queryPacket

        val response = queryPacket.copyOf()
        val buffer = ByteBuffer.wrap(response)

        // Set response flag and NXDOMAIN rcode
        buffer.position(2)
        val originalFlags = buffer.short.toInt()
        buffer.position(2)
        // QR=1, Opcode=0, AA=1, TC=0, RD=1, RA=1, RCODE=3 (NXDOMAIN)
        buffer.putShort(0x8583.toShort())

        // Zero answer, authority, and additional counts
        buffer.position(6)
        buffer.putShort(0) // ANCOUNT
        buffer.putShort(0) // NSCOUNT
        buffer.putShort(0) // ARCOUNT

        return response
    }

    private fun parseDomainName(buffer: ByteBuffer): String {
        val parts = mutableListOf<String>()
        while (buffer.hasRemaining()) {
            val length = buffer.get().toInt() and 0xFF
            if (length == 0) break
            if (length >= 192) {
                // Pointer, skip
                if (buffer.hasRemaining()) buffer.get()
                break
            }
            if (buffer.remaining() < length) break
            val bytes = ByteArray(length)
            buffer.get(bytes)
            parts.add(String(bytes))
        }
        return parts.joinToString(".")
    }

    fun getStats(): DnsFilterStats = DnsFilterStats(
        totalQueries = totalQueries,
        blockedQueries = blockedQueries,
        blocklistSize = blockedDomains.size.toLong(),
        whitelistSize = whitelist.size.toLong(),
        blacklistSize = blacklist.size.toLong(),
    )

    fun resetStats() {
        totalQueries = 0
        blockedQueries = 0
    }

    companion object {
        val DEFAULT_BLOCKLISTS = listOf(
            BlocklistSource("AdGuard DNS", "https://adguardteam.github.io/AdGuardSDNSFilter/Filters/filter.txt", BlocklistCategory.ADS),
            BlocklistSource("EasyList", "https://easylist.to/easylist/easylist.txt", BlocklistCategory.ADS),
            BlocklistSource("Malware Domains", "https://malware-filter.gitlab.io/malware-filter/urlhaus-filter-domains.txt", BlocklistCategory.MALWARE),
            BlocklistSource("Phishing Army", "https://phishing.army/download/phishing_army_blocklist.txt", BlocklistCategory.PHISHING),
            BlocklistSource("Peter Lowe Trackers", "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=nohtml", BlocklistCategory.TRACKERS),
        )
    }
}

data class DnsFilterStats(
    val totalQueries: Long = 0,
    val blockedQueries: Long = 0,
    val blocklistSize: Long = 0,
    val whitelistSize: Long = 0,
    val blacklistSize: Long = 0,
) {
    val blockRate: Float
        get() = if (totalQueries > 0) blockedQueries.toFloat() / totalQueries else 0f
}
