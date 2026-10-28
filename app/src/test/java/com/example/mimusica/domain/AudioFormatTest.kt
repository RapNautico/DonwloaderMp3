package com.example.mimusica.domain

import com.example.mimusica.domain.model.AudioFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Requisito FASE 5 §11: validación de formatos por MIME REAL (no extensión). */
class AudioFormatTest {

    @Test
    fun reconoce_los_formatos_minimos_especificados() {
        assertEquals(AudioFormat.MP3, AudioFormat.fromMime("audio/mpeg"))
        assertEquals(AudioFormat.AAC, AudioFormat.fromMime("audio/aac"))
        assertEquals(AudioFormat.M4A, AudioFormat.fromMime("audio/mp4"))
        assertEquals(AudioFormat.M4A, AudioFormat.fromMime("audio/x-m4a"))
        assertEquals(AudioFormat.FLAC, AudioFormat.fromMime("audio/flac"))
        assertEquals(AudioFormat.WAV, AudioFormat.fromMime("audio/wav"))
        assertEquals(AudioFormat.OGG, AudioFormat.fromMime("audio/ogg")) // vorbis/ogg
        assertEquals(AudioFormat.OPUS, AudioFormat.fromMime("audio/opus"))
    }

    @Test
    fun mimes_reales_con_parametros_se_reconocen() {
        assertEquals(AudioFormat.OGG, AudioFormat.fromMime("audio/ogg; opus")?.let { it })
    }

    @Test
    fun lo_que_no_es_audio_no_se_reconoce() {
        assertNull(AudioFormat.fromMime("video/mp4"))
        assertNull(AudioFormat.fromMime("image/png"))
        assertNull(AudioFormat.fromMime("application/pdf"))
        assertNull(AudioFormat.fromMime(null))
        assertNull(AudioFormat.fromMime("   "))
        assertNull(AudioFormat.fromMime("audio/desconocido-fabrica"))
    }
}
