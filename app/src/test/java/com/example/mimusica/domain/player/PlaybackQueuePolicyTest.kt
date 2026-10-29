package com.example.mimusica.domain.player

import com.example.mimusica.domain.model.PlaybackRepeatMode
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackQueuePolicyTest {
    @Test
    fun el_modo_repeticion_alterna_off_all_one_y_vuelve_a_off() {
        assertEquals(
            PlaybackRepeatMode.ALL,
            PlaybackQueuePolicy.nextRepeatMode(PlaybackRepeatMode.OFF),
        )
        assertEquals(
            PlaybackRepeatMode.ONE,
            PlaybackQueuePolicy.nextRepeatMode(PlaybackRepeatMode.ALL),
        )
        assertEquals(
            PlaybackRepeatMode.OFF,
            PlaybackQueuePolicy.nextRepeatMode(PlaybackRepeatMode.ONE),
        )
    }

    @Test
    fun anterior_reinicia_la_cancion_solo_despues_del_umbral() {
        assertEquals(false, PlaybackQueuePolicy.shouldRestartCurrent(3_000L))
        assertEquals(true, PlaybackQueuePolicy.shouldRestartCurrent(3_001L))
    }

    @Test
    fun seek_se_limita_a_cero_y_a_la_duracion_conocida() {
        assertEquals(0L, PlaybackQueuePolicy.seekTarget(500L, -10_000L, 20_000L))
        assertEquals(20_000L, PlaybackQueuePolicy.seekTarget(19_000L, 10_000L, 20_000L))
        assertEquals(20_000L, PlaybackQueuePolicy.seekTarget(10_000L, 10_000L, 0L))
    }
}
