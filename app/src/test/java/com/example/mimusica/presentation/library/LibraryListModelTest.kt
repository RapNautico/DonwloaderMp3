package com.example.mimusica.presentation.library

import com.example.mimusica.domain.model.Song
import com.example.mimusica.domain.model.SongSort
import com.example.mimusica.domain.model.toComparator
import com.example.mimusica.domain.repository.SongRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * FASE 8.3: pruebas de la lógica de búsqueda/orden de la biblioteca
 * ([LibraryListModel]) en JVM con tiempo REAL (sin `kotlinx-coroutines-test`,
 * dependencia NO autorizada): debounce de 500 ms verificado con márgenes
 * holgados (emisión a 250 ms < 500 ms; esperas de 550-800 ms > 500 ms), que es
 * determinista en la práctica: el debounce solo emite tras 500 ms de silencio.
 */
class LibraryListModelTest {

    /** Fake de Domain: en memoria; cuenta las búsquedas y puede fallar a pedido. */
    private class FakeRepo : SongRepository {
        val stored = mutableListOf<Song>()
        var searchCalls = 0
        var lastQuery: String? = null
        var failQueries = setOf<String>()

        override suspend fun addSongs(songs: List<Song>): Int {
            val nuevas = songs.filter { nueva -> stored.none { it.uri == nueva.uri } }
            stored.addAll(nuevas)
            return nuevas.size
        }

        override suspend fun updateSong(song: Song) {
            stored.removeAll { it.uri == song.uri }
            stored.add(song)
        }

        override suspend fun deleteSong(uri: String) {
            stored.removeAll { it.uri == uri }
        }

        override suspend fun getSong(uri: String): Song? = stored.firstOrNull { it.uri == uri }

        override suspend fun searchSongs(query: String): List<Song> {
            searchCalls++
            lastQuery = query
            if (query in failQueries) throw IllegalStateException("fallo simulado de búsqueda")
            return stored.filter { it.title.contains(query, ignoreCase = true) }
        }

        override fun observeLibrary(): Flow<List<Song>> =
            observeLibrary(SongSort.TITLE_ASC)

        override fun observeLibrary(sortedBy: SongSort): Flow<List<Song>> =
            flow { emit(stored.sortedWith(sortedBy.toComparator())) }

        override fun observeRecentSongs(limit: Int): Flow<List<Song>> =
            flowOf(stored.sortedByDescending { it.dateAddedMs }.take(limit))

        override suspend fun getSongsByArtist(artist: String?): List<Song> =
            stored.filter { it.artist == artist }

        override suspend fun getSongsByAlbum(album: String?): List<Song> =
            stored.filter { it.album == album }

        override fun observeArtistSummaries(): Flow<List<com.example.mimusica.domain.model.ArtistSummary>> =
            flowOf(stored.groupBy { it.artist }.map { (nombre, ss) ->
                com.example.mimusica.domain.model.ArtistSummary(nombre, ss.size)
            })

        override fun observeAlbumSummaries(): Flow<List<com.example.mimusica.domain.model.AlbumSummary>> =
            flowOf(stored.groupBy { it.album }.map { (nombre, ss) ->
                com.example.mimusica.domain.model.AlbumSummary(nombre, ss.size)
            })
    }

    private lateinit var repo: FakeRepo
    private lateinit var modelo: LibraryListModel
    private lateinit var scope: CoroutineScope

    private fun song(uri: String, title: String, duracionMs: Long = 1_000L) = Song(
        uri = uri,
        title = title,
        artist = null,
        album = null,
        durationMs = duracionMs,
        dateAddedMs = 1L,
    )

    @Before
    fun setup() {
        repo = FakeRepo()
        repo.stored.addAll(
            listOf(
                song("u-b", "Beta Rock", 20_000L),
                song("u-a", "Alfa Rock", 50_000L),
                song("u-c", "Otra cosa", 5_000L),
            ),
        )
        modelo = LibraryListModel(songRepository = repo, searchDebounceMs = SEARCH_MS)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        modelo.startIn(scope)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    private suspend fun esperarQuiescencia(ms: Long = QUIESC_MS) = delay(ms)

    // ---------- debounce (requisito 1) ----------

    @Test
    fun debounce_consulta_que_no_se_estabiliza_no_busca() = runBlocking {
        esperarQuiescencia()
        assertEquals(
            "la entrada inicial usa observeLibrary, NO searchSongs",
            0,
            repo.searchCalls,
        )

        modelo.onQueryChange("rock")
        delay(250) // por debajo del debounce de 500 ms
        assertEquals("con debounce activo no debe buscar aún", 0, repo.searchCalls)

        delay(550) // cruza los 500 ms de silencio
        assertEquals(1, repo.searchCalls)
        assertEquals("rock", repo.lastQuery)
    }

    @Test
    fun cambios_rapidos_de_consulta_solo_buscan_la_ultima() = runBlocking {
        esperarQuiescencia()
        repeat(20) { i ->
            modelo.onQueryChange("q$i")
            delay(10) // 200 ms totales: todas las pulsaciones dentro de la ventana
        }
        delay(800) // supera 500 ms tras la última pulsación
        assertEquals(1, repo.searchCalls)
        assertEquals("q19", repo.lastQuery)
        // Sin RESULTADOS para "q19": nada obsoleto de consultas rápidas anteriores:
        assertTrue(modelo.visibleSongs.value.songs.isEmpty())
        assertNull(modelo.error.value)
    }

    // ---------- resultados (requisitos 1-4) ----------

    @Test
    fun sin_consulta_usa_biblioteca_completa_ordenada_y_reactiva() = runBlocking {
        esperarQuiescencia()
        assertEquals(0, repo.searchCalls) // la biblioteca completa NO se "busca"
        assertEquals(listOf("u-a", "u-b", "u-c"), modelo.visibleSongs.value.songs.map { it.uri })

        // Reactivo: la lista visible también refleja canciones añadidas luego:
        repo.stored.add(song("u-n", "Nueva", 1_000L))
        modelo.onSortChange(SongSort.TITLE_DESC)
        delay(600 + SEARCH_MS)
        modelo.onSortChange(SongSort.TITLE_ASC)
        delay(600 + SEARCH_MS)
        // TITLE_ASC NOCASE (ASCII): "Alfa" < "Beta" < "Nueva" < "Otra":
        assertEquals(
            listOf("u-a", "u-b", "u-n", "u-c"),
            modelo.visibleSongs.value.songs.map { it.uri },
        )
    }

    @Test
    fun resultados_ordenados_con_orden_seleccionado() = runBlocking {
        esperarQuiescencia()
        modelo.onSortChange(SongSort.DURATION_DESC)
        modelo.onQueryChange("rock")
        delay(600 + SEARCH_MS)
        val resultados = modelo.visibleSongs.value.songs
        assertEquals(listOf("u-a", "u-b"), resultados.map { it.uri }) // 50k antes que 20k
        assertNull(modelo.error.value)
    }

    @Test
    fun busqueda_sin_coincidencias_da_lista_vacia_sin_error() = runBlocking {
        esperarQuiescencia()
        modelo.onQueryChange("xxxxxxxxxxxx")
        delay(600 + SEARCH_MS)
        assertTrue(modelo.visibleSongs.value.songs.isEmpty())
        assertNull(modelo.error.value)
        assertFalse(modelo.query.value.isEmpty())
    }

    // ---------- errores (requisito 5) ----------

    @Test
    fun fallo_de_busqueda_muestra_error_y_conserva_la_lista_previa() = runBlocking {
        esperarQuiescencia()
        val previa = modelo.visibleSongs.value.songs.map { it.uri }
        assertEquals(3, previa.size) // biblioteca completa

        repo.failQueries = setOf("rock")
        modelo.onQueryChange("rock")
        delay(600 + SEARCH_MS)
        assertEquals(1, repo.searchCalls)
        assertNotNull(modelo.error.value)
        // La lista previa NO se borra (la UI mantiene lo último válido):
        assertEquals(previa, modelo.visibleSongs.value.songs.map { it.uri })
    }

    @Test
    fun el_siguiente_resultado_exitoso_limpia_el_error() = runBlocking {
        esperarQuiescencia()
        repo.failQueries = setOf("boom")
        modelo.onQueryChange("boom")
        delay(600 + SEARCH_MS)
        assertNotNull(modelo.error.value)

        modelo.onQueryChange("rock")
        delay(600 + SEARCH_MS)
        assertNull(modelo.error.value)
        assertEquals(listOf("u-a", "u-b"), modelo.visibleSongs.value.songs.map { it.uri })
    }

    companion object {
        /** Debounce REAL del modelo para las pruebas (500 ms, decisión 2026-10-29). */
        const val SEARCH_MS = 500L
        /** Espera para estabilizar el estado inicial de flujos (margen > debounce). */
        const val QUIESC_MS = 700L
    }
}
