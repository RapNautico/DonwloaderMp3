package com.example.mimusica.data.player

import androidx.media3.common.Player
import com.example.mimusica.domain.model.PlaybackRepeatMode
import org.junit.Assert.assertEquals
import org.junit.Test

class Media3PlaybackModesTest {
    @Test
    fun los_modos_de_dominio_se_mapean_a_los_modos_nativos_de_media3() {
        assertEquals(Player.REPEAT_MODE_OFF, PlaybackRepeatMode.OFF.toMedia3RepeatMode())
        assertEquals(Player.REPEAT_MODE_ALL, PlaybackRepeatMode.ALL.toMedia3RepeatMode())
        assertEquals(Player.REPEAT_MODE_ONE, PlaybackRepeatMode.ONE.toMedia3RepeatMode())
    }

    @Test
    fun repeat_mode_de_media3_se_expone_en_el_estado_de_dominio() {
        assertEquals(Player.REPEAT_MODE_OFF.toPlaybackRepeatMode(), PlaybackRepeatMode.OFF)
        assertEquals(Player.REPEAT_MODE_ALL.toPlaybackRepeatMode(), PlaybackRepeatMode.ALL)
        assertEquals(Player.REPEAT_MODE_ONE.toPlaybackRepeatMode(), PlaybackRepeatMode.ONE)
        assertEquals(99.toPlaybackRepeatMode(), PlaybackRepeatMode.OFF)
    }
}
