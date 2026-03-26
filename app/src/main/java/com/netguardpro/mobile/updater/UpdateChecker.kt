package com.netguardpro.mobile.updater

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val hasUpdate: Boolean = false,
    val latestVersion: String = "",
    val currentVersion: String = "",
    val downloadUrl: String = "",
    val releaseNotes: String = "",
    val isChecking: Boolean = false,
    val error: String? = null,
)

object UpdateChecker {

    private const val TAG = "UpdateChecker"
    private const val GITHUB_API = "https://api.github.com/repos/sxc3030-eng/NetGuardPro-Mobile/releases"
    const val CURRENT_VERSION = "1.1.0"

    suspend fun checkForUpdate(): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode != 200) {
                return@withContext UpdateInfo(
                    currentVersion = CURRENT_VERSION,
                    error = "Server error: ${connection.responseCode}",
                )
            }

            val response = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            val releases = JSONArray(response)
            if (releases.length() == 0) {
                return@withContext UpdateInfo(
                    currentVersion = CURRENT_VERSION,
                    error = "No releases found",
                )
            }

            val latest = releases.getJSONObject(0)
            val tagName = latest.getString("tag_name").removePrefix("v")
            val body = latest.optString("body", "")

            // Find APK download URL
            var apkUrl = ""
            val assets = latest.getJSONArray("assets")
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.getString("name")
                if (name.endsWith(".apk")) {
                    apkUrl = asset.getString("browser_download_url")
                    break
                }
            }

            val hasUpdate = isNewerVersion(tagName, CURRENT_VERSION)

            UpdateInfo(
                hasUpdate = hasUpdate,
                latestVersion = tagName,
                currentVersion = CURRENT_VERSION,
                downloadUrl = apkUrl,
                releaseNotes = body,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed: ${e.message}", e)
            UpdateInfo(
                currentVersion = CURRENT_VERSION,
                error = "Connection error",
            )
        }
    }

    private fun isNewerVersion(remote: String, local: String): Boolean {
        try {
            val remoteParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
            val localParts = local.split(".").map { it.toIntOrNull() ?: 0 }

            for (i in 0 until maxOf(remoteParts.size, localParts.size)) {
                val r = remoteParts.getOrElse(i) { 0 }
                val l = localParts.getOrElse(i) { 0 }
                if (r > l) return true
                if (r < l) return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Version compare failed: ${e.message}")
        }
        return false
    }
}
