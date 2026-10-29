package com.example.mimusica.data.player

import com.example.mimusica.domain.model.PlaybackTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackSnapshotCodecTest {
    @Test
    fun encode_decode_conserva_cola_posicion_y_metadatos_opcionales() {
        val snapshot = PlaybackSnapshot(
            queue = listOf(
                PlaybackTrack(
                    uri = "content://provider/audio%20one",
                    title = "Título | uno\nsegundo",
                    artist = null,
                    artworkPath = "C:\\music\\covers\\cover.png",
                ),
                PlaybackTrack(
                    uri = "content://provider/audio-two",
                    title = "Otra",
                    artist = "Artista",
                    artworkPath = null,
                ),
            ),
            currentIndex = 1,
            positionMs = 42_500L,
            playWhenReady = true,
        )

        assertEquals(snapshot, PlaybackSnapshotCodec.decode(PlaybackSnapshotCodec.encode(snapshot)))
    }

    @Test
    fun decode_rechaza_versiones_cola_o_indice_invalidos() {
        assertNull(PlaybackSnapshotCodec.decode("2|0|0|false\n"))
        assertNull(PlaybackSnapshotCodec.decode("1|2|0|false\nYQ|Yg|~|~"))
        assertNull(PlaybackSnapshotCodec.decode("1|0|-1|false\nYQ|Yg|~|~"))
        assertNull(PlaybackSnapshotCodec.decode("1|0|0|false\n"))
    }
}
