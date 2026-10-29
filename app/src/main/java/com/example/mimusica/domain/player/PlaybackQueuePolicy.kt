package com.example.mimusica.domain.player

import com.example.mimusica.domain.model.PlaybackRepeatMode

object PlaybackQueuePolicy {
    const val RESTART_PREVIOUS_THRESHOLD_MS = 3_000L

    fun nextRepeatMode(mode: PlaybackRepeatMode): PlaybackRepeatMode =
        when (mode) {
            PlaybackRepeatMode.OFF -> PlaybackRepeatMode.ALL
            PlaybackRepeatMode.ALL -> PlaybackRepeatMode.ONE
            PlaybackRepeatMode.ONE -> PlaybackRepeatMode.OFF
        }

    fun shouldRestartCurrent(positionMs: Long): Boolean =
        positionMs > RESTART_PREVIOUS_THRESHOLD_MS

    fun seekTarget(positionMs: Long, offsetMs: Long, durationMs: Long): Long {
        val upperBound = durationMs.takeIf { it > 0L } ?: Long.MAX_VALUE
        return (positionMs + offsetMs).coerceIn(0L, upperBound)
    }
}
