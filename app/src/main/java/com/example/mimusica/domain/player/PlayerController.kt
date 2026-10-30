package com.example.mimusica.domain.player

import com.example.mimusica.domain.model.PlaybackState
import com.example.mimusica.domain.model.PlaybackRepeatMode
import com.example.mimusica.domain.model.PlaybackTrack
import kotlinx.coroutines.flow.StateFlow

interface PlayerController {
    val state: StateFlow<PlaybackState>

    suspend fun playQueue(queue: List<PlaybackTrack>, startIndex: Int)
    suspend fun togglePlayPause()
    suspend fun skipToNext()
    suspend fun skipToPrevious()
    suspend fun seekTo(positionMs: Long)
    suspend fun seekBy(offsetMs: Long)
    suspend fun setVolume(volume: Float)
    suspend fun setRepeatMode(mode: PlaybackRepeatMode)
    suspend fun setShuffleEnabled(enabled: Boolean)
    suspend fun stop()
}
