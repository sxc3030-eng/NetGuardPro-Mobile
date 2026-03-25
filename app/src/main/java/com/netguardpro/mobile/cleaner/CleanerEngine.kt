package com.netguardpro.mobile.cleaner

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class JunkCategory {
    CACHE,
    APKS,
    LARGE_FILES,
    TEMP_FILES,
}

data class JunkFile(
    val path: String,
    val name: String,
    val size: Long,
    val category: JunkCategory,
    val lastModified: Long,
)

data class ScanResult(
    val cacheFiles: List<JunkFile> = emptyList(),
    val apkFiles: List<JunkFile> = emptyList(),
    val largeFiles: List<JunkFile> = emptyList(),
    val tempFiles: List<JunkFile> = emptyList(),
) {
    val totalSize: Long
        get() = cacheFiles.sumOf { it.size } + apkFiles.sumOf { it.size } +
                largeFiles.sumOf { it.size } + tempFiles.sumOf { it.size }

    val totalCount: Int
        get() = cacheFiles.size + apkFiles.size + largeFiles.size + tempFiles.size

    fun getByCategory(category: JunkCategory): List<JunkFile> = when (category) {
        JunkCategory.CACHE -> cacheFiles
        JunkCategory.APKS -> apkFiles
        JunkCategory.LARGE_FILES -> largeFiles
        JunkCategory.TEMP_FILES -> tempFiles
    }
}

data class StorageInfo(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
) {
    val usagePercent: Float
        get() = if (totalBytes > 0) usedBytes.toFloat() / totalBytes else 0f
}

class CleanerEngine(private val context: Context) {

    suspend fun getStorageInfo(): StorageInfo = withContext(Dispatchers.IO) {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val totalBytes = stat.blockSizeLong * stat.blockCountLong
            val freeBytes = stat.blockSizeLong * stat.availableBlocksLong
            val usedBytes = totalBytes - freeBytes
            StorageInfo(totalBytes, usedBytes, freeBytes)
        } catch (e: Exception) {
            Log.e(TAG, "getStorageInfo failed: ${e.message}", e)
            StorageInfo(0, 0, 0)
        }
    }

    suspend fun scan(): ScanResult = withContext(Dispatchers.IO) {
        val cacheFiles = try {
            scanCacheFiles()
        } catch (e: Exception) {
            Log.e(TAG, "scanCacheFiles failed: ${e.message}", e)
            emptyList()
        }
        val apkFiles = try {
            scanApkFiles()
        } catch (e: Exception) {
            Log.e(TAG, "scanApkFiles failed: ${e.message}", e)
            emptyList()
        }
        val largeFiles = try {
            scanLargeFiles()
        } catch (e: Exception) {
            Log.e(TAG, "scanLargeFiles failed: ${e.message}", e)
            emptyList()
        }
        val tempFiles = try {
            scanTempFiles()
        } catch (e: Exception) {
            Log.e(TAG, "scanTempFiles failed: ${e.message}", e)
            emptyList()
        }

        ScanResult(
            cacheFiles = cacheFiles,
            apkFiles = apkFiles,
            largeFiles = largeFiles,
            tempFiles = tempFiles,
        )
    }

    private fun scanCacheFiles(): List<JunkFile> {
        val junkFiles = mutableListOf<JunkFile>()

        // Internal cache
        try {
            val internalCache = context.cacheDir
            if (internalCache != null && internalCache.exists() && internalCache.canRead()) {
                collectFiles(internalCache, JunkCategory.CACHE, junkFiles)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cannot scan internal cache: ${e.message}")
        }

        // External cache
        try {
            context.externalCacheDir?.let { externalCache ->
                if (externalCache.exists() && externalCache.canRead()) {
                    collectFiles(externalCache, JunkCategory.CACHE, junkFiles)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cannot scan external cache: ${e.message}")
        }

        // Code cache
        try {
            val codeCache = context.codeCacheDir
            if (codeCache != null && codeCache.exists() && codeCache.canRead()) {
                collectFiles(codeCache, JunkCategory.CACHE, junkFiles)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cannot scan code cache: ${e.message}")
        }

        return junkFiles
    }

    private fun scanApkFiles(): List<JunkFile> {
        val junkFiles = mutableListOf<JunkFile>()

        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir != null && downloadsDir.exists() && downloadsDir.canRead()) {
                downloadsDir.listFiles()?.forEach { file ->
                    try {
                        if (file.isFile && file.extension.equals("apk", ignoreCase = true)) {
                            junkFiles.add(
                                JunkFile(
                                    path = file.absolutePath,
                                    name = file.name,
                                    size = file.length(),
                                    category = JunkCategory.APKS,
                                    lastModified = file.lastModified(),
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Cannot read file: ${file.name}: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cannot scan downloads for APKs: ${e.message}")
        }

        return junkFiles
    }

    private fun scanLargeFiles(): List<JunkFile> {
        val junkFiles = mutableListOf<JunkFile>()
        val threshold = 50L * 1024 * 1024 // 50MB

        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir != null && downloadsDir.exists() && downloadsDir.canRead()) {
                scanDirectoryForLargeFiles(downloadsDir, threshold, junkFiles)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cannot scan downloads for large files: ${e.message}")
        }

        try {
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (documentsDir != null && documentsDir.exists() && documentsDir.canRead()) {
                scanDirectoryForLargeFiles(documentsDir, threshold, junkFiles)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cannot scan documents for large files: ${e.message}")
        }

        return junkFiles.sortedByDescending { it.size }
    }

    private fun scanDirectoryForLargeFiles(dir: File, threshold: Long, results: MutableList<JunkFile>) {
        try {
            dir.listFiles()?.forEach { file ->
                try {
                    if (file.isFile && file.length() > threshold) {
                        results.add(
                            JunkFile(
                                path = file.absolutePath,
                                name = file.name,
                                size = file.length(),
                                category = JunkCategory.LARGE_FILES,
                                lastModified = file.lastModified(),
                            )
                        )
                    } else if (file.isDirectory && !file.name.startsWith(".") && file.canRead()) {
                        scanDirectoryForLargeFiles(file, threshold, results)
                    }
                } catch (e: SecurityException) {
                    Log.w(TAG, "Permission denied: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "scanDirectoryForLargeFiles error: ${e.message}")
        }
    }

    private fun scanTempFiles(): List<JunkFile> {
        val junkFiles = mutableListOf<JunkFile>()
        val tempExtensions = setOf("tmp", "temp", "log", "bak", "old", "dmp")

        val dirs = mutableListOf<File>()
        try { context.cacheDir?.let { if (it.exists()) dirs.add(it) } } catch (_: Exception) {}
        try { context.externalCacheDir?.let { if (it.exists()) dirs.add(it) } } catch (_: Exception) {}
        try { context.filesDir?.let { if (it.exists()) dirs.add(it) } } catch (_: Exception) {}
        try {
            val dl = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (dl != null && dl.exists()) dirs.add(dl)
        } catch (_: Exception) {}

        dirs.forEach { dir ->
            try {
                if (dir.canRead()) {
                    scanDirectoryForTempFiles(dir, tempExtensions, junkFiles, maxDepth = 3, currentDepth = 0)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cannot scan dir for temp files: ${dir.name}: ${e.message}")
            }
        }

        return junkFiles
    }

    private fun scanDirectoryForTempFiles(
        dir: File,
        extensions: Set<String>,
        results: MutableList<JunkFile>,
        maxDepth: Int,
        currentDepth: Int,
    ) {
        if (currentDepth > maxDepth) return

        try {
            dir.listFiles()?.forEach { file ->
                try {
                    if (file.isFile && extensions.contains(file.extension.lowercase())) {
                        results.add(
                            JunkFile(
                                path = file.absolutePath,
                                name = file.name,
                                size = file.length(),
                                category = JunkCategory.TEMP_FILES,
                                lastModified = file.lastModified(),
                            )
                        )
                    } else if (file.isDirectory && !file.name.startsWith(".") && file.canRead()) {
                        scanDirectoryForTempFiles(file, extensions, results, maxDepth, currentDepth + 1)
                    }
                } catch (e: SecurityException) {
                    // Permission denied, skip
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "scanDirectoryForTempFiles error: ${e.message}")
        }
    }

    private fun collectFiles(dir: File, category: JunkCategory, results: MutableList<JunkFile>) {
        try {
            dir.listFiles()?.forEach { file ->
                try {
                    if (file.isFile) {
                        results.add(
                            JunkFile(
                                path = file.absolutePath,
                                name = file.name,
                                size = file.length(),
                                category = category,
                                lastModified = file.lastModified(),
                            )
                        )
                    } else if (file.isDirectory && file.canRead()) {
                        collectFiles(file, category, results)
                    }
                } catch (e: SecurityException) {
                    // Permission denied, skip
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "collectFiles error: ${e.message}")
        }
    }

    suspend fun clean(files: List<JunkFile>): Long = withContext(Dispatchers.IO) {
        var cleanedBytes = 0L
        files.forEach { junkFile ->
            try {
                val file = File(junkFile.path)
                if (file.exists() && file.canWrite() && file.delete()) {
                    cleanedBytes += junkFile.size
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "Cannot delete ${junkFile.name}: ${e.message}")
            } catch (e: Exception) {
                Log.w(TAG, "Cannot delete ${junkFile.name}: ${e.message}")
            }
        }
        cleanedBytes
    }

    suspend fun cleanCategory(scanResult: ScanResult, category: JunkCategory): Long {
        return clean(scanResult.getByCategory(category))
    }

    suspend fun cleanAll(scanResult: ScanResult): Long {
        return clean(
            scanResult.cacheFiles + scanResult.apkFiles +
                    scanResult.largeFiles + scanResult.tempFiles
        )
    }

    companion object {
        private const val TAG = "CleanerEngine"
    }
}
