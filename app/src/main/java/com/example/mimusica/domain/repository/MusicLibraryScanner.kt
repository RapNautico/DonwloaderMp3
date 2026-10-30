package com.example.mimusica.domain.repository

import com.example.mimusica.domain.model.MusicScanStatus
import kotlinx.coroutines.flow.StateFlow

interface MusicLibraryScanner {
    val status: StateFlow<MusicScanStatus>

    suspend fun scanMusicFolder()

    fun startAutomaticUpdates()

    fun stopAutomaticUpdates()
}
