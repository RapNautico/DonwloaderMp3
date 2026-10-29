package com.example.mimusica.domain.player

object PlaybackQueuePolicy {
    const val RESTART_PREVIOUS_THRESHOLD_MS = 3_000L

    fun nextIndex(currentIndex: Int, queueSize: Int): Int? =
        (currentIndex + 1).takeIf { currentIndex in 0 until queueSize && it < queueSize }

    fun previousIndex(currentIndex: Int, queueSize: Int, positionMs: Long): Int? {
        if (currentIndex !in 0 until queueSize) return null
        if (positionMs > RESTART_PREVIOUS_THRESHOLD_MS) return currentIndex
        return (currentIndex - 1).takeIf { it >= 0 }
    }

    fun seekTarget(positionMs: Long, offsetMs: Long, durationMs: Long): Long {
        val upperBound = durationMs.takeIf { it > 0L } ?: Long.MAX_VALUE
        return (positionMs + offsetMs).coerceIn(0L, upperBound)
    }
}
