package com.example.mimusica.data.player

import com.example.mimusica.domain.model.PlaybackTrack
import com.example.mimusica.domain.model.PlaybackRepeatMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
            repeatMode = PlaybackRepeatMode.ONE,
            shuffleEnabled = true,
            shuffleOrder = listOf(1, 0),
        )

        assertEquals(snapshot, PlaybackSnapshotCodec.decode(PlaybackSnapshotCodec.encode(snapshot)))
    }

    @Test
    fun decode_v1_conserva_cola_y_posicion_con_modos_predeterminados() {
        val snapshot = PlaybackSnapshotCodec.decode("1|0|12500|false\nYQ|Yg|~|~")

        assertEquals(PlaybackRepeatMode.OFF, snapshot?.repeatMode)
        assertFalse(snapshot!!.shuffleEnabled)
        assertNull(snapshot.shuffleOrder)
        assertEquals(12_500L, snapshot.positionMs)
        assertEquals("a", snapshot.queue.single().uri)
        assertEquals("b", snapshot.queue.single().title)
    }

    @Test
    fun decode_preserva_repeat_all_shuffle_y_el_orden_completo() {
        val encoded = "2|1|8400|true|ALL|true|2,0,1\nYQ|YQ|~|~\nYg|Yg|~|~\nYw|Yw|~|~"

        val snapshot = PlaybackSnapshotCodec.decode(encoded)

        assertEquals(1, snapshot?.currentIndex)
        assertEquals(8_400L, snapshot?.positionMs)
        assertTrue(snapshot?.playWhenReady == true)
        assertEquals(PlaybackRepeatMode.ALL, snapshot?.repeatMode)
        assertTrue(snapshot?.shuffleEnabled == true)
        assertEquals(listOf(2, 0, 1), snapshot?.shuffleOrder)
        assertEquals("b", snapshot?.currentTrackUri())
    }

    @Test
    fun decode_rechaza_versiones_cola_indice_y_orden_invalidos() {
        assertNull(PlaybackSnapshotCodec.decode("2|0|0|false\n"))
        assertNull(PlaybackSnapshotCodec.decode("1|2|0|false\nYQ|Yg|~|~"))
        assertNull(PlaybackSnapshotCodec.decode("1|0|-1|false\nYQ|Yg|~|~"))
        assertNull(PlaybackSnapshotCodec.decode("1|0|0|false\n"))
        assertNull(PlaybackSnapshotCodec.decode("2|0|0|false|UNKNOWN|false|~\nYQ|Yg|~|~"))
        assertNull(PlaybackSnapshotCodec.decode("2|0|0|false|ALL|true|0,0\nYQ|Yg|~|~\nYw|Yw|~|~"))
    }

    private fun PlaybackSnapshot.currentTrackUri(): String = queue[currentIndex].uri
}
