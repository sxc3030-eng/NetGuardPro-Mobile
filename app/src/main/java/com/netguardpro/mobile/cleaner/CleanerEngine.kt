package com.netguardpro.mobile.cleaner

import android.content.Context
import android.os.Environment
import android.os.StatFs
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
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalBytes = stat.blockSizeLong * stat.blockCountLong
        val freeBytes = stat.blockSizeLong * stat.availableBlocksLong
        val usedBytes = totalBytes - freeBytes
        StorageInfo(totalBytes, usedBytes, freeBytes)
    }

    suspend fun scan(): ScanResult = withContext(Dispatchers.IO) {
        val cacheFiles = scanCacheFiles()
        val apkFiles = scanApkFiles()
        val largeFiles = scanLargeFiles()
        val tempFiles = scanTempFiles()

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
        val internalCache = context.cacheDir
        if (internalCache.exists()) {
            collectFiles(internalCache, JunkCategory.CACHE, junkFiles)
        }

        // External cache
        context.externalCacheDir?.let { externalCache ->
            if (externalCache.exists()) {
                collectFiles(externalCache, JunkCategory.CACHE, junkFiles)
            }
        }

        // Code cache
        val codeCache = context.codeCacheDir
        if (codeCache.exists()) {
            collectFiles(codeCache, JunkCategory.CACHE, junkFiles)
        }

        return junkFiles
    }

    private fun scanApkFiles(): List<JunkFile> {
        val junkFiles = mutableListOf<JunkFile>()
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        if (downloadsDir.exists()) {
            downloadsDir.listFiles()?.forEach { file ->
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
            }
        }

        return junkFiles
    }

    private fun scanLargeFiles(): List<JunkFile> {
        val junkFiles = mutableListOf<JunkFile>()
        val threshold = 50L * 1024 * 1024 // 50MB

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloadsDir.exists()) {
            scanDirectoryForLargeFiles(downloadsDir, threshold, junkFiles)
        }

        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (documentsDir.exists()) {
            scanDirectoryForLargeFiles(documentsDir, threshold, junkFiles)
        }

        return junkFiles.sortedByDescending { it.size }
    }

    private fun scanDirectoryForLargeFiles(dir: File, threshold: Long, results: MutableList<JunkFile>) {
        dir.listFiles()?.forEach { file ->
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
            } else if (file.isDirectory && !file.name.startsWith(".")) {
                scanDirectoryForLargeFiles(file, threshold, results)
            }
        }
    }

    private fun scanTempFiles(): List<JunkFile> {
        val junkFiles = mutableListOf<JunkFile>()
        val tempExtensions = setOf("tmp", "temp", "log", "bak", "old", "dmp")

        val dirs = listOfNotNull(
            context.cacheDir,
            context.externalCacheDir,
            context.filesDir,
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        )

        dirs.forEach { dir ->
            if (dir.exists()) {
                scanDirectoryForTempFiles(dir, tempExtensions, junkFiles, maxDepth = 3, currentDepth = 0)
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

        dir.listFiles()?.forEach { file ->
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
            } else if (file.isDirectory && !file.name.startsWith(".")) {
                scanDirectoryForTempFiles(file, extensions, results, maxDepth, currentDepth + 1)
            }
        }
    }

    private fun collectFiles(dir: File, category: JunkCategory, results: MutableList<JunkFile>) {
        dir.listFiles()?.forEach { file ->
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
            } else if (file.isDirectory) {
                collectFiles(file, category, results)
            }
        }
    }

    suspend fun clean(files: List<JunkFile>): Long = withContext(Dispatchers.IO) {
        var cleanedBytes = 0L
        files.forEach { junkFile ->
            try {
                val file = File(junkFile.path)
                if (file.exists() && file.delete()) {
                    cleanedBytes += junkFile.size
                }
            } catch (e: SecurityException) {
                // Permission denied, skip
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
}
