package com.example.mimusica.presentation.library

import com.example.mimusica.domain.model.AlbumSummary
import com.example.mimusica.domain.model.ArtistSummary
import com.example.mimusica.domain.model.Song
import com.example.mimusica.domain.model.SongSort
import com.example.mimusica.domain.repository.SongRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * FASE 8.6: pruebas de la GESTIÓN ([LibraryManagerModel]): eliminar de
 * biblioteca SOLO el registro (D8.3), con ConfirmDialog OBLIGATORIO en la UI
 * (aquí llega la DECISIÓN ya confirmada por el usuario).
 *
 * JVM con runBlocking directo ([deleteSong] es suspend): sin Main ni
 * `kotlinx-coroutines-test` (dependencia NO autorizada). El fake retarda la
 * operación 150 ms para observar [LibraryManagerModel.busy] en tiempo real.
 */
class LibraryManagerModelTest {

    private class FakeRepo : SongRepository {
        val deleted = mutableListOf<String>() // URIs de DELETE recibidas
        var deleteError: Throwable? = null

        private val inFlightMs = 150L

        override suspend fun deleteSong(uri: String) {
            delay(inFlightMs) // ventana observable de busy
            deleteError?.let { throw it }
            deleted.add(uri)
        }

        override suspend fun addSongs(songs: List<Song>): Int = 0
        override suspend fun updateSong(song: Song) = Unit
        override suspend fun getSong(uri: String): Song? = null
        override suspend fun searchSongs(query: String): List<Song> = emptyList()
        override fun observeLibrary(): Flow<List<Song>> = flowOf(emptyList())
        override fun observeLibrary(sortedBy: SongSort): Flow<List<Song>> = flowOf(emptyList())
        override fun observeRecentSongs(limit: Int): Flow<List<Song>> = flowOf(emptyList())
        override fun observeArtistSummaries(): Flow<List<ArtistSummary>> = flowOf(emptyList())
        override fun observeAlbumSummaries(): Flow<List<AlbumSummary>> = flowOf(emptyList())
        override suspend fun getSongsByArtist(artist: String?): List<Song> = emptyList()
        override suspend fun getSongsByAlbum(album: String?): List<Song> = emptyList()
    }

    private lateinit var repo: FakeRepo
    private lateinit var model: LibraryManagerModel

    @Before
    fun setup() {
        repo = FakeRepo()
        model = LibraryManagerModel(repo)
    }

    @Test
    fun elimina_solo_el_registro_recibiendo_la_uri_real() = runBlocking {
        model.deleteSong("content://a/b")
        assertEquals(listOf("content://a/b"), repo.deleted)
        assertNull(model.error.value)
        assertFalse(model.busy.value)
    }

    @Test
    fun fallo_fija_error_sin_crashear_y_libera_busy() = runBlocking {
        repo.deleteError = IllegalStateException("Room bloqueada")
        model.deleteSong("content://a/b")
        assertEquals("Room bloqueada", model.error.value)
        // busy SIEMPRE se libera (el fold va DESPUÉS del desbloqueo):
        assertFalse(model.busy.value)
    }

    @Test
    fun cancelacion_de_eliminacion_se_propaga_y_libera_busy_sin_error() = runBlocking {
        val cancelled = CancellationException("delete cancelled")
        repo.deleteError = cancelled

        var caught: CancellationException? = null
        try {
            model.deleteSong("content://a/b")
        } catch (actual: CancellationException) {
            caught = actual
        }

        assertSame(cancelled, caught)
        assertNull(model.error.value)
        assertFalse(model.busy.value)
        assertTrue(repo.deleted.isEmpty())
    }

    @Test
    fun el_siguiente_exito_limpia_el_error_previo() = runBlocking {
        repo.deleteError = IllegalStateException("fallo 1")
        model.deleteSong("content://x")
        assertEquals("fallo 1", model.error.value)

        repo.deleteError = null
        model.deleteSong("content://y")
        assertNull(model.error.value)
        assertEquals(listOf("content://y"), repo.deleted)
    }

    @Test
    fun busy_es_visible_durante_la_operacion() = runBlocking {
        val job = launch { model.deleteSong("content://slow") }
        // Espera ACTIVA acotada (2 s) hasta que busy se vuelva visible:
        val deadline = System.currentTimeMillis() + 2_000
        while (!model.busy.value && System.currentTimeMillis() < deadline) delay(5)
        assertTrue(model.busy.value)
        job.join()
        assertFalse(model.busy.value)
        assertEquals(listOf("content://slow"), repo.deleted)
    }

    @Test
    fun segunda_eliminacion_concurrente_se_ignora() = runBlocking {
        val primera = launch { model.deleteSong("content://primera") }
        // Mientras la primera está en curso (fake retardado), la segunda DEBE
        // ignorarse (una sola eliminación a la vez, regla del modelo):
        val deadline = System.currentTimeMillis() + 2_000
        while (!model.busy.value && System.currentTimeMillis() < deadline) delay(5)
        model.deleteSong("content://segunda") // busy: devuelve sin hacer nada
        primera.join()
        assertEquals(listOf("content://primera"), repo.deleted)
        assertNull(model.error.value)
        assertFalse(model.busy.value)
    }
}
