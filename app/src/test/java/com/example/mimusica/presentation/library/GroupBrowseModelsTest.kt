package com.example.mimusica.presentation.library

import com.example.mimusica.domain.model.AlbumSummary
import com.example.mimusica.domain.model.ArtistSummary
import com.example.mimusica.domain.model.Song
import com.example.mimusica.domain.model.SongSort
import com.example.mimusica.domain.model.toComparator
import com.example.mimusica.domain.repository.SongRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
 * FASE 8.4: pruebas de la lógica de exploración por artistas/álbumes
 * ([GroupSummariesModel] y [GroupDetailModel]) en JVM con scope propio y
 * tiempo real determinista por PUNTO DE ESPERA (await por predicado con
 * tope de 2 s): no hay debounce aquí, así que las pruebas son rápidas y
 * sin bordes de tiempo sensibles.
 *
 * Sin dependencias nuevas (kotlinx-coroutines-test sigue sin aprobarse).
 */
class GroupBrowseModelsTest {

    /**
     * Fake de Domain: resúmenes como MutableStateFlow (emulación fiel a Room,
     * que re-emite al cambiar la tabla) y registro de la llamada al detalle.
     */
    private class FakeRepo : SongRepository {
        val artistsState = MutableStateFlow<List<ArtistSummary>>(emptyList())
        val albumsState = MutableStateFlow<List<AlbumSummary>>(emptyList())

        var songsByArtist: Map<String?, List<Song>> = emptyMap()
        var songsByAlbum: Map<String?, List<Song>> = emptyMap()
        var lastDetailKind: GroupKind? = null
        var lastDetailName: String? = null
        var detailError: Throwable? = null
        var detailStarted: CompletableDeferred<Unit>? = null
        var detailGate: CompletableDeferred<Unit>? = null

        override fun observeArtistSummaries(): Flow<List<ArtistSummary>> = artistsState

        override fun observeAlbumSummaries(): Flow<List<AlbumSummary>> = albumsState

        override suspend fun getSongsByArtist(artist: String?): List<Song> =
            recordDetail(GroupKind.ARTIST, artist, songsByArtist[artist])

        override suspend fun getSongsByAlbum(album: String?): List<Song> =
            recordDetail(GroupKind.ALBUM, album, songsByAlbum[album])

        private suspend fun recordDetail(
            kind: GroupKind,
            name: String?,
            found: List<Song>?,
        ): List<Song> {
            lastDetailKind = kind
            lastDetailName = name
            detailStarted?.complete(Unit)
            detailGate?.await()
            if (detailError != null) throw detailError!!
            return found ?: emptyList()
        }

        private val stored = mutableListOf<Song>()

        override suspend fun addSongs(songs: List<Song>): Int = 0
        override suspend fun updateSong(song: Song) = Unit
        override suspend fun deleteSong(uri: String) = Unit
        override suspend fun getSong(uri: String): Song? = null
        override suspend fun searchSongs(query: String): List<Song> = emptyList()
        override fun observeLibrary(): Flow<List<Song>> = observeLibrary(SongSort.TITLE_ASC)
        override fun observeLibrary(sortedBy: SongSort): Flow<List<Song>> =
            flowOf(stored.sortedWith(sortedBy.toComparator()))
        override fun observeRecentSongs(limit: Int): Flow<List<Song>> = flowOf(emptyList())
    }

    private lateinit var repo: FakeRepo
    private lateinit var scope: CoroutineScope

    private fun song(uri: String, title: String) = Song(
        uri = uri,
        title = title,
        artist = null,
        album = null,
        durationMs = 1_000L,
        dateAddedMs = 1L,
    )

    @Before
    fun setup() {
        repo = FakeRepo()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    /** Espera ACTIVA y acotada (2 s) un predicado sobre el estado en test. */
    private suspend fun await(timeoutMs: Long = 2_000, predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!predicate() && System.currentTimeMillis() < deadline) delay(10)
    }

    // ---------- Resúmenes (vista de ARTISTAS) ----------

    @Test
    fun artistas_por_defecto_lista_la_cantidad_de_cada_grupo() = runBlocking {
        repo.artistsState.value = listOf(
            ArtistSummary("Alpha", 3),
            ArtistSummary(null, 2), // bucket desconocido: null INTACTO del Domain
        )
        val model = GroupSummariesModel(repo.observeArtistSummaries())
        model.startIn(scope)

        await { !model.isLoading.value && model.items.value.size == 2 }
        assertFalse(model.isLoading.value)
        assertNull(model.error.value)
        // El nombre null NO se sustituye en la lógica (solo en la UI "Desconocido"):
        assertEquals(3, model.items.value[0].songCount)
        assertEquals(2, model.items.value[1].songCount)
        ArtistsSummaryNameCheck(model.items.value[1])
    }

    @Test
    fun artistas_es_reactivo_a_cambios_de_la_biblioteca() = runBlocking {
        val model = GroupSummariesModel(repo.observeArtistSummaries())
        model.startIn(scope)

        await { !model.isLoading.value } // primera emisión (vacía): loading ya acabó
        assertTrue(model.items.value.isEmpty()) // biblioteca vacía: estado explícito

        repo.artistsState.value = listOf(ArtistSummary("Beta", 1))
        await { model.items.value.size == 1 }
        assertEquals("Beta", model.items.value[0].name)
        assertNull(model.error.value)
    }

    @Test
    fun resumenes_con_error_conserva_lo_cargado_y_avisa() = runBlocking {
        // Emite datos y LUEGO falla (p. ej. una re-lectura de tablas corruptas):
        repo.albumsState.value = listOf(AlbumSummary("Disco", 5))
        val conFallo = flow<List<AlbumSummary>> {
            emit(listOf(AlbumSummary("Disco", 5)))
            throw IllegalStateException("BD no disponible")
        }
        class AlbumsSource : SongRepository by repo {
            override fun observeAlbumSummaries() = conFallo
        }
        val model = GroupSummariesModel(AlbumsSource().observeAlbumSummaries())
        model.startIn(scope)

        await { model.error.value != null }
        assertEquals(5, model.items.value[0].songCount) // lista previa NO se borra
        assertFalse(model.isLoading.value)
    }

    @Test
    fun albumes_llegan_con_su_conteo_renueva_en_cambios() = runBlocking {
        val model = GroupSummariesModel(repo.observeAlbumSummaries())
        model.startIn(scope)

        repo.albumsState.value = listOf(
            AlbumSummary("A", 1),
            AlbumSummary(null, 4),
        )
        await { model.items.value.size == 2 }
        assertEquals(1, model.items.value[0].songCount)
        assertNull(model.items.value[1].name)
        assertEquals(4, model.items.value[1].songCount)
    }

    // ---------- Detalle (nombre null = bucket desconocido NAVEGABLE) ----------

    @Test
    fun detalle_artista_desconocido_consulta_el_bucket_null() = runBlocking {
        repo.songsByArtist = mapOf<String?, List<Song>>(
            null to listOf(song("u-1", "Sin artista 1"), song("u-2", "Sin artista 2")),
        )
        val model = GroupDetailModel(repo, GroupKind.ARTIST, groupName = null)
        model.startIn(scope)

        await { model.state.value is GroupDetailState.Content }
        assertEquals(GroupKind.ARTIST, repo.lastDetailKind)
        assertNull(repo.lastDetailName) // null llega INTACTO al repositorio
        val contenido = model.state.value as GroupDetailState.Content
        assertEquals(listOf("u-1", "u-2"), contenido.songs.map { it.uri })
        assertEquals(null, model.groupName) // la UI etiqueta "Artista desconocido"
    }

    @Test
    fun detalle_album_con_nombre_especial_y_espacios() = runBlocking {
        val nombre = "AC/DC & Los 100% - \"Greatest\" Hits"
        repo.songsByAlbum = mapOf(nombre to listOf(song("u-9", "Pista")))
        val model = GroupDetailModel(repo, GroupKind.ALBUM, groupName = nombre)
        model.startIn(scope)

        await { model.state.value is GroupDetailState.Content }
        assertEquals(GroupKind.ALBUM, repo.lastDetailKind)
        assertEquals(nombre, repo.lastDetailName)
        val contenido = model.state.value as GroupDetailState.Content
        assertEquals(listOf("u-9"), contenido.songs.map { it.uri })
    }

    @Test
    fun detalle_con_fallo_del_repositorio_da_estado_error_sin_crashear() = runBlocking {
        repo.detailError = IllegalStateException("fallo simulado")
        val model = GroupDetailModel(repo, GroupKind.ARTIST, groupName = "Alpha")
        model.startIn(scope)

        await { model.state.value is GroupDetailState.Error }
        assertNotNull(model.state.value)
        val estado = model.state.value as GroupDetailState.Error
        assertTrue(estado.message.isNotBlank())
        assertFalse(estado.message.isEmpty())
    }

    @Test
    fun cancelacion_del_detalle_no_se_convierte_en_estado_error() = runBlocking {
        val cancelled = kotlinx.coroutines.CancellationException("query cancelled")
        val started = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        val parent = kotlinx.coroutines.SupervisorJob()
        val detailScope = CoroutineScope(parent + Dispatchers.Unconfined)
        repo.detailError = cancelled
        repo.detailStarted = started
        repo.detailGate = gate
        val model = GroupDetailModel(repo, GroupKind.ARTIST, groupName = "Alpha")

        try {
            model.startIn(detailScope)
            started.await()
            val operation = parent.children.single()
            gate.complete(Unit)
            operation.join()

            assertTrue(operation.isCancelled)
            assertEquals(GroupDetailState.Loading, model.state.value)
        } finally {
            detailScope.cancel()
        }
    }

    @Test
    fun detalle_artista_inexistente_da_contenido_vacio_sin_error() = runBlocking {
        repo.songsByArtist = emptyMap()
        val model = GroupDetailModel(repo, GroupKind.ARTIST, groupName = "Nadie")
        model.startIn(scope)

        await { model.state.value is GroupDetailState.Content }
        val contenido = model.state.value as GroupDetailState.Content
        assertTrue(contenido.songs.isEmpty())
    }

    /** El bucket desconocido conserva name = null SIN cadena mágica. */
    private fun ArtistsSummaryNameCheck(artista: ArtistSummary) {
        assertNull(artista.name)
    }
}
