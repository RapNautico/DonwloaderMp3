package com.example.mimusica.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mimusica.domain.model.AppSettings
import com.example.mimusica.domain.repository.AppSettingsRepository
import com.example.mimusica.domain.repository.MusicLibraryScanner
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val repository: AppSettingsRepository,
    private val musicScanner: MusicLibraryScanner,
    context: Context,
) : ViewModel() {
    private val appContext = context.applicationContext
    private val mutableStorageBytes = MutableStateFlow<Long?>(null)
    private val mutableStorageError = MutableStateFlow(false)
    private val updateMutex = Mutex()

    val settings = repository.settings
    val scanStatus = musicScanner.status
    val storageBytes = mutableStorageBytes.asStateFlow()
    val storageError = mutableStorageError.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { updateMutex.withLock { repository.update(transform(settings.value)) } }
    }

    fun setAutomaticScanEnabled(enabled: Boolean) {
        viewModelScope.launch {
            updateMutex.withLock {
                repository.update(settings.value.copy(scanMusicFolder = enabled))
            }
            if (enabled) musicScanner.startAutomaticUpdates() else musicScanner.stopAutomaticUpdates()
        }
    }

    fun scanMusicFolder() {
        viewModelScope.launch { musicScanner.scanMusicFolder() }
    }

    fun refreshStorageUsage() {
        viewModelScope.launch {
            try {
                mutableStorageBytes.value = withContext(Dispatchers.IO) {
                    val databaseFile = appContext.getDatabasePath("mimusica.db")
                    listOf(
                        appContext.filesDir,
                        appContext.cacheDir,
                        databaseFile,
                        File(databaseFile.path + "-wal"),
                        File(databaseFile.path + "-shm"),
                    ).sumOf(::directorySize)
                }
                mutableStorageError.value = false
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: SecurityException) {
                mutableStorageError.value = true
            } catch (_: java.io.IOException) {
                mutableStorageError.value = true
            }
        }
    }

    private fun directorySize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        return file.walkTopDown().filter(File::isFile).sumOf(File::length)
    }
}
