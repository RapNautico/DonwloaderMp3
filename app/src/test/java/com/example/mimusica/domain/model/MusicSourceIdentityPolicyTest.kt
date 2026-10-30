package com.example.mimusica.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicSourceIdentityPolicyTest {
    @Test
    fun `identidad media store y saf comparte volumen y ruta music`() {
        assertEquals(
            "music:primary:Music/Albums/song.mp3",
            MusicSourceIdentityKey.fromRelativePath("external_primary", "Music/Albums/song.mp3"),
        )
        assertEquals(
            MusicSourceIdentityKey.fromRelativePath("primary", "Music/Albums/song.mp3"),
            MusicSourceIdentityKey.fromRelativePath("external", "Music/Albums/song.mp3"),
        )
    }

    @Test
    fun `identidad limita la carpeta music y rechaza path traversal`() {
        assertEquals(null, MusicSourceIdentityKey.fromRelativePath("primary", "Download/song.mp3"))
        assertEquals(null, MusicSourceIdentityKey.fromRelativePath("primary", "music/song.mp3"))
        assertEquals(null, MusicSourceIdentityKey.fromRelativePath("primary", "Music/../Pictures/song.mp3"))
    }

    @Test
    fun `identidad de Audio conserva volumen y normaliza mayusculas de carpeta`() {
        assertEquals(
            "music:primary:Audio/Albums/song.mp3",
            MusicSourceIdentityKey.fromRelativePath("external_primary", "audio/Albums/song.mp3"),
        )
    }

    @Test
    fun `solo reimporta una fuente nueva o con metadata de origen cambiada`() {
        val source = SongSourceIdentity("music:primary:Music/song.mp3", 1_000L, 400L)
        val unchanged = SongSourceState(1_000L, 400L)

        assertTrue(source.requiresRefresh(null))
        assertFalse(source.requiresRefresh(unchanged))
        assertTrue(source.requiresRefresh(unchanged.copy(modifiedAtMs = 2_000L)))
        assertTrue(source.requiresRefresh(unchanged.copy(sizeBytes = 500L)))
    }

    @Test
    fun `resumen cuenta como conocidos los archivos filtrados antes del importador`() {
        val source = SongSourceIdentity("music:primary:Music/song.mp3", 1_000L, 400L)
        val status = MusicScanStatus.completed(
            indexedSources = listOf(source),
            knownSources = mapOf(source.key to SongSourceState(1_000L, 400L)),
            addedSongs = 0,
            duplicateImports = 0,
            failedFiles = 0,
        )

        assertEquals(1, status.indexedFiles)
        assertEquals(0, status.addedSongs)
        assertEquals(1, status.duplicates)
        assertEquals(0, status.failedFiles)
        assertTrue(status.hasScanned)
    }
}
