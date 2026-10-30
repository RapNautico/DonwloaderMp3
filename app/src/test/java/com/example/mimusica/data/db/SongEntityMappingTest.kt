package com.example.mimusica.data.db

import com.example.mimusica.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * FASE 8.1: mapeo explícito Entity <-> Domínio, incluyendo los campos que la
 * auditoría descubrió caídos antes del fix (sampleRateHz, channels, artworkPath)
 * y el nuevo `genre` (D8.1). Ambas direcciones deben conservar TODO el dato.
 */
class SongEntityMappingTest {

    private val songCompleta = Song(
        uri = "content://sa/x",
        title = "Título",
        artist = "Artista",
        album = "Álbum",
        durationMs = 12_345L,
        dateAddedMs = 9_999L,
        mimeType = "audio/ogg",
        sizeBytes = 1_234L,
        bitrateBps = 160_000L,
        sampleRateHz = 48_000,
        channels = 1,
        artworkPath = "/data/artwork/x.png",
        genre = "Metal",
    )

    private val songSinOpcionales = Song(
        uri = "content://sa/y",
        title = "Sin nada",
        artist = null,
        album = null,
        durationMs = 1L,
        dateAddedMs = 2L,
    )

    @Test
    fun domain_a_entity_conserva_todo() {
        val e = songCompleta.toEntity()
        assertEquals(songCompleta.uri, e.uri)
        assertEquals(songCompleta.title, e.title)
        assertEquals(songCompleta.artist, e.artist)
        assertEquals(songCompleta.album, e.album)
        assertEquals(songCompleta.durationMs, e.durationMs)
        assertEquals(songCompleta.dateAddedMs, e.dateAddedMs)
        assertEquals(songCompleta.mimeType, e.mimeType)
        assertEquals(songCompleta.sizeBytes, e.sizeBytes)
        assertEquals(songCompleta.bitrateBps, e.bitrateBps)
        assertEquals(songCompleta.sampleRateHz, e.sampleRateHz)
        assertEquals(songCompleta.channels, e.channels)
        assertEquals(songCompleta.artworkPath, e.artworkPath)
        assertEquals("Metal", e.genre)
    }

    @Test
    fun entity_a_domain_conserva_todo() {
        val s = songCompleta.toEntity().toSong()
        assertEquals(songCompleta, s)
    }

    @Test
    fun nulos_se_mantienen_nulos_en_ambas_direcciones() {
        val e = songSinOpcionales.toEntity()
        assertNull(e.artist)
        assertNull(e.album)
        assertNull(e.mimeType)
        assertNull(e.sizeBytes)
        assertNull(e.bitrateBps)
        assertNull(e.sampleRateHz)
        assertNull(e.channels)
        assertNull(e.artworkPath)
        assertNull(e.genre)
        assertEquals(songSinOpcionales, e.toSong())
    }
}
