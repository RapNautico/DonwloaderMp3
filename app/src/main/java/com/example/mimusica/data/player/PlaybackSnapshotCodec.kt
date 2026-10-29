package com.example.mimusica.data.player

import com.example.mimusica.domain.model.PlaybackTrack
import java.nio.charset.StandardCharsets
import java.util.Base64

internal data class PlaybackSnapshot(
    val queue: List<PlaybackTrack>,
    val currentIndex: Int,
    val positionMs: Long,
    val playWhenReady: Boolean,
)

internal object PlaybackSnapshotCodec {
    private const val VERSION = "1"

    fun encode(snapshot: PlaybackSnapshot): String = buildString {
        append(VERSION)
        append('|')
        append(snapshot.currentIndex)
        append('|')
        append(snapshot.positionMs.coerceAtLeast(0L))
        append('|')
        append(snapshot.playWhenReady)
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
        if (header.size != 4 || header[0] != VERSION) return null

        val index = header[1].toIntOrNull() ?: return null
        val position = header[2].toLongOrNull()?.takeIf { it >= 0L } ?: return null
        val playWhenReady = header[3].toBooleanStrictOrNull() ?: return null
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

        return PlaybackSnapshot(queue, index, position, playWhenReady)
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
