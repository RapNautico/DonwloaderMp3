package com.example.mimusica.domain.model

data class PlaybackTrack(
    val uri: String,
    val title: String,
    val artist: String?,
    val artworkPath: String?,
)

enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    READY,
    ENDED,
    ERROR,
}

data class PlaybackState(
    val queue: List<PlaybackTrack> = emptyList(),
    val currentIndex: Int = -1,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val isPlaying: Boolean = false,
    val volume: Float = 1f,
    val error: String? = null,
) {
    val currentTrack: PlaybackTrack?
        get() = queue.getOrNull(currentIndex)
}
