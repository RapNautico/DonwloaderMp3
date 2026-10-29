package com.example.mimusica.domain.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackQueuePolicyTest {
    @Test
    fun siguiente_no_avanza_mas_alla_del_final_ni_desde_una_cola_vacia() {
        assertEquals(1, PlaybackQueuePolicy.nextIndex(0, 2))
        assertNull(PlaybackQueuePolicy.nextIndex(1, 2))
        assertNull(PlaybackQueuePolicy.nextIndex(-1, 0))
    }

    @Test
    fun anterior_reinicia_la_actual_o_selecciona_la_previa() {
        assertEquals(1, PlaybackQueuePolicy.previousIndex(1, 3, 3_001L))
        assertEquals(0, PlaybackQueuePolicy.previousIndex(1, 3, 3_000L))
        assertNull(PlaybackQueuePolicy.previousIndex(0, 3, 0L))
        assertNull(PlaybackQueuePolicy.previousIndex(-1, 0, 0L))
    }

    @Test
    fun seek_se_limita_a_cero_y_a_la_duracion_conocida() {
        assertEquals(0L, PlaybackQueuePolicy.seekTarget(500L, -10_000L, 20_000L))
        assertEquals(20_000L, PlaybackQueuePolicy.seekTarget(19_000L, 10_000L, 20_000L))
        assertEquals(20_000L, PlaybackQueuePolicy.seekTarget(10_000L, 10_000L, 0L))
    }
}
