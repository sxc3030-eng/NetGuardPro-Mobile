package com.netguardpro.mobile

import com.netguardpro.mobile.cleaner.CleanerUiState
import com.netguardpro.mobile.cleaner.JunkCategory
import com.netguardpro.mobile.cleaner.JunkFile
import com.netguardpro.mobile.cleaner.ScanResult
import com.netguardpro.mobile.cleaner.ScanState
import com.netguardpro.mobile.cleaner.StorageInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for Cleaner data classes and state management.
 */
class CleanerTest {

    @Test
    fun `default scan state is idle`() {
        val state = CleanerUiState()
        assertEquals(ScanState.IDLE, state.scanState)
        assertEquals(0L, state.lastCleanedBytes)
        assertTrue(state.cleanedCategories.isEmpty())
    }

    @Test
    fun `scan result total calculation`() {
        val result = ScanResult(
            cacheFiles = listOf(
                JunkFile("/data/cache1.tmp", "cache1.tmp", 1024, JunkCategory.CACHE, 0),
                JunkFile("/data/cache2.tmp", "cache2.tmp", 2048, JunkCategory.CACHE, 0),
            ),
            apkFiles = listOf(
                JunkFile("/sdcard/app.apk", "app.apk", 5000000, JunkCategory.APKS, 0),
            ),
            largeFiles = emptyList(),
            tempFiles = emptyList(),
        )
        assertEquals(3, result.totalCount)
        assertEquals(1024L + 2048L + 5000000L, result.totalSize)
    }

    @Test
    fun `empty scan result`() {
        val result = ScanResult()
        assertEquals(0, result.totalCount)
        assertEquals(0L, result.totalSize)
    }

    @Test
    fun `storage info usage percent`() {
        val info = StorageInfo(
            totalBytes = 64_000_000_000L,
            usedBytes = 48_000_000_000L,
            freeBytes = 16_000_000_000L,
        )
        assertEquals(0.75f, info.usagePercent, 0.01f)
    }

    @Test
    fun `storage info zero total`() {
        val info = StorageInfo(totalBytes = 0, usedBytes = 0, freeBytes = 0)
        assertEquals(0f, info.usagePercent, 0.01f)
    }

    @Test
    fun `junk category enum values`() {
        val categories = JunkCategory.entries
        assertEquals(4, categories.size)
        assertTrue(categories.contains(JunkCategory.CACHE))
        assertTrue(categories.contains(JunkCategory.APKS))
        assertTrue(categories.contains(JunkCategory.LARGE_FILES))
        assertTrue(categories.contains(JunkCategory.TEMP_FILES))
    }

    @Test
    fun `cleaned categories tracking`() {
        var state = CleanerUiState()
        state = state.copy(cleanedCategories = state.cleanedCategories + JunkCategory.CACHE)
        assertTrue(JunkCategory.CACHE in state.cleanedCategories)
        assertEquals(1, state.cleanedCategories.size)

        state = state.copy(cleanedCategories = JunkCategory.entries.toSet())
        assertEquals(4, state.cleanedCategories.size)
    }

    @Test
    fun `scan result after cleaning category`() {
        val result = ScanResult(
            cacheFiles = listOf(JunkFile("/c", "c", 100, JunkCategory.CACHE, 0)),
            apkFiles = listOf(JunkFile("/a", "a", 200, JunkCategory.APKS, 0)),
            largeFiles = emptyList(),
            tempFiles = emptyList(),
        )
        val afterClean = result.copy(cacheFiles = emptyList())
        assertEquals(1, afterClean.totalCount) // only apk remains
        assertEquals(200L, afterClean.totalSize)
    }
}
