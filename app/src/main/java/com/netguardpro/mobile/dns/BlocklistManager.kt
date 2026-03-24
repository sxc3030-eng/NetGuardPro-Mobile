package com.netguardpro.mobile.dns

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

enum class BlocklistCategory {
    ADS,
    TRACKERS,
    MALWARE,
    PHISHING,
    CUSTOM,
}

data class BlocklistSource(
    val name: String,
    val url: String,
    val category: BlocklistCategory,
    val enabled: Boolean = true,
    val domainCount: Int = 0,
    val lastUpdated: Long = 0,
)

class BlocklistManager(private val context: Context) {

    private val cacheDir = File(context.filesDir, "blocklists")
    private val sources = mutableListOf<BlocklistSource>()

    init {
        cacheDir.mkdirs()
        sources.addAll(DnsFilter.DEFAULT_BLOCKLISTS)
    }

    fun getSources(): List<BlocklistSource> = sources.toList()

    fun addSource(source: BlocklistSource) {
        sources.removeAll { it.url == source.url }
        sources.add(source)
    }

    fun removeSource(url: String) {
        sources.removeAll { it.url == url }
        val cacheFile = getCacheFile(url)
        cacheFile.delete()
    }

    fun toggleSource(url: String, enabled: Boolean) {
        val index = sources.indexOfFirst { it.url == url }
        if (index >= 0) {
            sources[index] = sources[index].copy(enabled = enabled)
        }
    }

    suspend fun downloadBlocklist(source: BlocklistSource): List<String> = withContext(Dispatchers.IO) {
        val domains = mutableListOf<String>()

        try {
            val connection = URL(source.url).openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.requestMethod = "GET"

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                reader.useLines { lines ->
                    lines.forEach { line ->
                        val domain = parseLine(line)
                        if (domain != null) {
                            domains.add(domain)
                        }
                    }
                }

                // Cache the parsed domains
                val cacheFile = getCacheFile(source.url)
                cacheFile.writeText(domains.joinToString("\n"))

                val idx = sources.indexOfFirst { it.url == source.url }
                if (idx >= 0) {
                    sources[idx] = sources[idx].copy(
                        domainCount = domains.size,
                        lastUpdated = System.currentTimeMillis(),
                    )
                }
            }
            connection.disconnect()
        } catch (e: Exception) {
            // Try loading from cache
            val cacheFile = getCacheFile(source.url)
            if (cacheFile.exists()) {
                domains.addAll(cacheFile.readLines().filter { it.isNotBlank() })
            }
        }

        domains
    }

    suspend fun downloadAllEnabled(): List<String> = withContext(Dispatchers.IO) {
        val allDomains = mutableListOf<String>()
        sources.filter { it.enabled }.forEach { source ->
            allDomains.addAll(downloadBlocklist(source))
        }
        allDomains.distinct()
    }

    suspend fun loadCachedDomains(): List<String> = withContext(Dispatchers.IO) {
        val allDomains = mutableListOf<String>()
        sources.filter { it.enabled }.forEach { source ->
            val cacheFile = getCacheFile(source.url)
            if (cacheFile.exists()) {
                allDomains.addAll(cacheFile.readLines().filter { it.isNotBlank() })
            }
        }
        allDomains.distinct()
    }

    private fun parseLine(line: String): String? {
        val trimmed = line.trim()

        // Skip empty lines and comments
        if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) return null

        // Handle hosts file format: "0.0.0.0 domain.com" or "127.0.0.1 domain.com"
        if (trimmed.startsWith("0.0.0.0") || trimmed.startsWith("127.0.0.1")) {
            val parts = trimmed.split("\\s+".toRegex())
            if (parts.size >= 2) {
                val domain = parts[1].trim()
                if (domain.isNotEmpty() && domain != "localhost" && domain.contains(".")) {
                    return domain.lowercase()
                }
            }
            return null
        }

        // Handle "||domain.com^" format (AdBlock style)
        if (trimmed.startsWith("||") && trimmed.endsWith("^")) {
            val domain = trimmed.removePrefix("||").removeSuffix("^").trim()
            if (domain.isNotEmpty() && domain.contains(".")) {
                return domain.lowercase()
            }
            return null
        }

        // Plain domain format
        if (trimmed.contains(".") && !trimmed.contains("/") && !trimmed.contains(" ")) {
            return trimmed.lowercase()
        }

        return null
    }

    private fun getCacheFile(url: String): File {
        val hash = url.hashCode().toString(16)
        return File(cacheDir, "blocklist_$hash.txt")
    }
}
