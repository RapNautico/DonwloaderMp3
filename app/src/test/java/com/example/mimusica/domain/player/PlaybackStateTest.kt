package com.example.mimusica.domain.player

import com.example.mimusica.domain.model.PlaybackState
import com.example.mimusica.domain.model.PlaybackTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackStateTest {
    @Test
    fun currentTrack_se_deriva_del_indice_actual_sin_inventar_valores() {
        val selected = PlaybackTrack("content://song/2", "Título", null, null)
        val state = PlaybackState(
            queue = listOf(
                PlaybackTrack("content://song/1", "Otro", "Artista", null),
                selected,
            ),
            currentIndex = 1,
        )

        assertEquals(selected, state.currentTrack)
        assertNull(state.copy(currentIndex = -1).currentTrack)
        assertNull(state.copy(currentIndex = 2).currentTrack)
    }
}
