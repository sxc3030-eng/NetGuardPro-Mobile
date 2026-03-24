package com.netguardpro.mobile.cleaner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ScanState {
    IDLE,
    SCANNING,
    COMPLETED,
    CLEANING,
}

data class CleanerUiState(
    val scanState: ScanState = ScanState.IDLE,
    val scanResult: ScanResult = ScanResult(),
    val storageInfo: StorageInfo = StorageInfo(0, 0, 0),
    val lastCleanedBytes: Long = 0,
    val cleanedCategories: Set<JunkCategory> = emptySet(),
)

class CleanerViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = CleanerEngine(application)

    private val _uiState = MutableStateFlow(CleanerUiState())
    val uiState: StateFlow<CleanerUiState> = _uiState.asStateFlow()

    init {
        loadStorageInfo()
    }

    private fun loadStorageInfo() {
        viewModelScope.launch {
            val info = engine.getStorageInfo()
            _uiState.update { it.copy(storageInfo = info) }
        }
    }

    fun scan() {
        if (_uiState.value.scanState == ScanState.SCANNING) return

        viewModelScope.launch {
            _uiState.update { it.copy(scanState = ScanState.SCANNING, cleanedCategories = emptySet()) }
            val result = engine.scan()
            val storageInfo = engine.getStorageInfo()
            _uiState.update {
                it.copy(
                    scanState = ScanState.COMPLETED,
                    scanResult = result,
                    storageInfo = storageInfo,
                )
            }
        }
    }

    fun cleanCategory(category: JunkCategory) {
        viewModelScope.launch {
            _uiState.update { it.copy(scanState = ScanState.CLEANING) }
            val cleaned = engine.cleanCategory(_uiState.value.scanResult, category)
            val storageInfo = engine.getStorageInfo()

            _uiState.update {
                val newResult = when (category) {
                    JunkCategory.CACHE -> it.scanResult.copy(cacheFiles = emptyList())
                    JunkCategory.APKS -> it.scanResult.copy(apkFiles = emptyList())
                    JunkCategory.LARGE_FILES -> it.scanResult.copy(largeFiles = emptyList())
                    JunkCategory.TEMP_FILES -> it.scanResult.copy(tempFiles = emptyList())
                }
                it.copy(
                    scanState = ScanState.COMPLETED,
                    scanResult = newResult,
                    storageInfo = storageInfo,
                    lastCleanedBytes = it.lastCleanedBytes + cleaned,
                    cleanedCategories = it.cleanedCategories + category,
                )
            }
        }
    }

    fun cleanAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(scanState = ScanState.CLEANING) }
            val cleaned = engine.cleanAll(_uiState.value.scanResult)
            val storageInfo = engine.getStorageInfo()

            _uiState.update {
                it.copy(
                    scanState = ScanState.COMPLETED,
                    scanResult = ScanResult(),
                    storageInfo = storageInfo,
                    lastCleanedBytes = it.lastCleanedBytes + cleaned,
                    cleanedCategories = JunkCategory.entries.toSet(),
                )
            }
        }
    }
}
