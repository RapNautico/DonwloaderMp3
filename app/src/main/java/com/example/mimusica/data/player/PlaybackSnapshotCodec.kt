package com.example.mimusica.data.player

import com.example.mimusica.domain.model.PlaybackTrack
import com.example.mimusica.domain.model.PlaybackRepeatMode
import java.nio.charset.StandardCharsets
import java.util.Base64

internal data class PlaybackSnapshot(
    val queue: List<PlaybackTrack>,
    val currentIndex: Int,
    val positionMs: Long,
    val playWhenReady: Boolean,
    val repeatMode: PlaybackRepeatMode = PlaybackRepeatMode.OFF,
    val shuffleEnabled: Boolean = false,
    val shuffleOrder: List<Int>? = null,
)

internal object PlaybackSnapshotCodec {
    private const val VERSION_1 = "1"
    private const val VERSION = "2"

    fun encode(snapshot: PlaybackSnapshot): String = buildString {
        append(VERSION)
        append('|')
        append(snapshot.currentIndex)
        append('|')
        append(snapshot.positionMs.coerceAtLeast(0L))
        append('|')
        append(snapshot.playWhenReady)
        append('|')
        append(snapshot.repeatMode.name)
        append('|')
        append(snapshot.shuffleEnabled)
        append('|')
        append(snapshot.shuffleOrder?.joinToString(",") ?: "~")
        snapshot.queue.forEach { track ->
            append('\n')
            append(track.uri.encodeField())
            append('|')
            append(track.title.encodeField())
            append('|')
            append(track.artist.encodeNullableField())
            append('|')
            append(track.artworkPath.encodeNullableField())
        }
    }

    fun decode(encoded: String): PlaybackSnapshot? {
        val lines = encoded.split('\n')
        val header = lines.firstOrNull()?.split('|') ?: return null
        val version = header.firstOrNull() ?: return null
        if (version !in setOf(VERSION_1, VERSION)) return null
        if (version == VERSION_1 && header.size != 4) return null
        if (version == VERSION && header.size != 7) return null

        val index = header[1].toIntOrNull() ?: return null
        val position = header[2].toLongOrNull()?.takeIf { it >= 0L } ?: return null
        val playWhenReady = header[3].toBooleanStrictOrNull() ?: return null
        val repeatMode = if (version == VERSION_1) {
            PlaybackRepeatMode.OFF
        } else {
            runCatching { PlaybackRepeatMode.valueOf(header[4]) }.getOrNull() ?: return null
        }
        val shuffleEnabled = if (version == VERSION_1) {
            false
        } else {
            header[5].toBooleanStrictOrNull() ?: return null
        }
        val encodedShuffleOrder = if (version == VERSION_1 || header[6] == "~") {
            null
        } else {
            header[6].split(',').map { it.toIntOrNull() ?: return null }
        }
        val queue = lines.drop(1).map { line ->
            val fields = line.split('|')
            if (fields.size != 4) return null
            val uri = fields[0].decodeField() ?: return null
            val title = fields[1].decodeField() ?: return null
            if (uri.isBlank() || title.isBlank()) return null
            if (fields[2] != "~" && fields[2].decodeField() == null) return null
            if (fields[3] != "~" && fields[3].decodeField() == null) return null
            PlaybackTrack(
                uri = uri,
                title = title,
                artist = fields[2].decodeNullableField(),
                artworkPath = fields[3].decodeNullableField(),
            )
        }
        if (queue.isEmpty() || index !in queue.indices) return null
        if (
            encodedShuffleOrder != null &&
            (encodedShuffleOrder.size != queue.size ||
                encodedShuffleOrder.toSet().size != queue.size ||
                encodedShuffleOrder.any { it !in queue.indices })
        ) {
            return null
        }

        return PlaybackSnapshot(
            queue = queue,
            currentIndex = index,
            positionMs = position,
            playWhenReady = playWhenReady,
            repeatMode = repeatMode,
            shuffleEnabled = shuffleEnabled,
            shuffleOrder = encodedShuffleOrder,
        )
    }

    private fun String.encodeField(): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(toByteArray(StandardCharsets.UTF_8))

    private fun String?.encodeNullableField(): String = this?.encodeField() ?: "~"

    private fun String.decodeField(): String? = try {
        String(Base64.getUrlDecoder().decode(this), StandardCharsets.UTF_8)
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun String.decodeNullableField(): String? =
        if (this == "~") null else decodeField()
}
