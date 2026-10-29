package com.example.mimusica.data

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.room3.Room
import com.example.mimusica.data.db.AppDatabase
import com.example.mimusica.data.repository.PlaylistRepositoryImpl
import com.example.mimusica.data.repository.SongRepositoryImpl
import com.example.mimusica.domain.model.Song
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PlaylistRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun playlist_persiste_nombre_conteo_portada_y_orden_propios() = runBlocking {
        val db = openDatabase()
        val playlists = PlaylistRepositoryImpl(db.playlistDao())
        val songs = SongRepositoryImpl(db.songDao())
        songs.addSongs(
            listOf(
                song("content://audio/one", "Uno", "C:\\art\\one.jpg"),
                song("content://audio/two", "Dos", "C:\\art\\two.jpg"),
            ),
        )

        val firstPlaylist = playlists.createPlaylist("  Favoritas  ")
        val secondPlaylist = playlists.createPlaylist("Entrenamiento")
        assertTrue(playlists.addSong(firstPlaylist, "content://audio/two"))
        assertTrue(playlists.addSong(firstPlaylist, "content://audio/one"))
        assertTrue(playlists.addSong(secondPlaylist, "content://audio/one"))
        assertFalse(playlists.addSong(firstPlaylist, "content://audio/two"))

        val first = playlists.observePlaylist(firstPlaylist).first()!!
        assertEquals("Favoritas", first.playlist.name)
        assertEquals(2, first.playlist.songCount)
        assertEquals("C:\\art\\two.jpg", first.playlist.artworkPath)
        assertEquals(listOf("Dos", "Uno"), first.songs.map { it.title })

        playlists.renamePlaylist(secondPlaylist, "Para entrenar")
        playlists.reorderSongs(firstPlaylist, listOf("content://audio/one", "content://audio/two"))
        assertEquals(
            listOf("Uno", "Dos"),
            playlists.observePlaylist(firstPlaylist).first()?.songs?.map { it.title },
        )
        val renamed = playlists.observePlaylists().first().first { it.id == secondPlaylist }
        assertEquals("Para entrenar", renamed.name)
        assertEquals(1, renamed.songCount)
        db.close()
    }

    @Test
    fun eliminar_playlist_o_cancion_elimina_solo_la_relacion() = runBlocking {
        val db = openDatabase()
        val playlists = PlaylistRepositoryImpl(db.playlistDao())
        val songs = SongRepositoryImpl(db.songDao())
        val song = song("content://audio/retained", "Conservar")
        songs.addSongs(listOf(song))
        val playlistId = playlists.createPlaylist("Temporal")
        playlists.addSong(playlistId, song.uri)

        songs.deleteSong(song.uri)
        assertTrue(playlists.observePlaylist(playlistId).first()?.songs.orEmpty().isEmpty())

        songs.addSongs(listOf(song))
        playlists.addSong(playlistId, song.uri)
        playlists.deletePlaylist(playlistId)
        assertEquals(song, songs.getSong(song.uri))
        assertTrue(playlists.observePlaylists().first().isEmpty())
        db.close()
    }

    @Test
    fun rechaza_nombres_vacios_y_ordenes_que_no_pertenecen_a_la_playlist() = runBlocking {
        val db = openDatabase()
        val playlists = PlaylistRepositoryImpl(db.playlistDao())
        try {
            playlists.createPlaylist("  ")
            throw AssertionError("El nombre vacío debía ser rechazado.")
        } catch (_: IllegalArgumentException) {
            // Nombre validado por el contrato de dominio/repositorio.
        }

        val playlistId = playlists.createPlaylist("Orden")
        try {
            playlists.reorderSongs(playlistId, listOf("content://audio/unknown"))
            throw AssertionError("No debe aceptar URIs ajenas al orden actual.")
        } catch (_: IllegalArgumentException) {
            // El DAO verifica el conjunto completo dentro de la transacción.
        }
        db.close()
    }

    private fun openDatabase(): AppDatabase =
        Room.databaseBuilder<AppDatabase>(
            name = temporaryFolder.newFile("playlists.db").absolutePath,
        )
            .setDriver(BundledSQLiteDriver())
            .build()

    private fun song(uri: String, title: String, artwork: String? = null) = Song(
        uri = uri,
        title = title,
        artist = null,
        album = null,
        durationMs = 1_000L,
        dateAddedMs = 10L,
        artworkPath = artwork,
    )
}
