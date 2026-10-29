package com.example.mimusica.presentation.library

import com.example.mimusica.domain.model.AlbumSummary
import com.example.mimusica.domain.model.ArtistSummary
import com.example.mimusica.domain.model.Song
import com.example.mimusica.domain.model.SongSort
import com.example.mimusica.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * FASE 8.5: pruebas del DETALLE de canción ([SongDetailModel] y los helpers
 * de formato puros) en JVM. El `load()` es suspend y se llama DIRECTO con
 * runBlocking: sin Main, sin `kotlinx-coroutines-test` (dependencia NO
 * autorizada), sin esperas sensibles (no hay debounce aquí).
 *
 * Contratos reales usados: SongRepository.getSong(uri) (8.2).
 */
class SongDetailModelTest {

    private class FakeRepo : SongRepository {
        val songs = mutableMapOf<String, Song>()
        var getSongError: Throwable? = null

        override suspend fun getSong(uri: String): Song? {
            getSongError?.let { throw it }
            return songs[uri]
        }

        private val stored = mutableListOf<Song>()

        override suspend fun addSongs(songs: List<Song>): Int = 0
        override suspend fun updateSong(song: Song) = Unit
        override suspend fun deleteSong(uri: String) = Unit
        override suspend fun searchSongs(query: String): List<Song> = emptyList()
        override fun observeLibrary(): Flow<List<Song>> = observeLibrary(SongSort.TITLE_ASC)
        override fun observeLibrary(sortedBy: SongSort): Flow<List<Song>> =
            flowOf(emptyList())

        override fun observeRecentSongs(limit: Int): Flow<List<Song>> = flowOf(emptyList())
        override fun observeArtistSummaries(): Flow<List<ArtistSummary>> = flowOf(emptyList())
        override fun observeAlbumSummaries(): Flow<List<AlbumSummary>> = flowOf(emptyList())
        override suspend fun getSongsByArtist(artist: String?): List<Song> = emptyList()
        override suspend fun getSongsByAlbum(album: String?): List<Song> = emptyList()
    }

    private lateinit var repo: FakeRepo

    @Before
    fun setup() {
        repo = FakeRepo()
    }

    private fun songReal(uri: String) = Song(
        uri = uri,
        title = "Röyksopp - Happy Up Here",
        artist = "Röyksopp",
        album = "Melody A.M.",
        durationMs = 193_000L,
        dateAddedMs = 1_700_000_000_000L,
        mimeType = "audio/mpeg",
        sizeBytes = 6_291_456L,
        bitrateBps = 256_000L,
        sampleRateHz = 44_100,
        channels = 2,
        artworkPath = "/data/artwork/abc.jpg",
        genre = null, // el archivo NO informa género: debe llegar null INTACTO
    )

    // ---------- contenido con datos REALES y nulls intactos ----------

    @Test
    fun carga_la_cancion_con_todos_los_campos_reales() = runBlocking {
        val original = songReal("content://a/b")
        repo.songs["content://a/b"] = original
        val model = SongDetailModel(repo, "content://a/b")
        model.load()

        val contenido = model.state.value as SongDetailState.Content
        assertEquals(original, contenido.song) // viaja INTACTO (sin mapeos)
        assertNull((model.state.value as SongDetailState.Content).song.genre)
    }

    @Test
    fun uri_inexistente_da_notfound_sin_error() = runBlocking {
        val model = SongDetailModel(repo, "content://no/existe")
        model.load()
        assertTrue(model.state.value is SongDetailState.NotFound)
    }

    @Test
    fun uri_nula_or_vacia_da_notfound_sin_consultar_repositorio() = runBlocking {
        val modelNulo = SongDetailModel(repo, null)
        modelNulo.load()
        assertTrue(modelNulo.state.value is SongDetailState.NotFound)

        val modelVacio = SongDetailModel(repo, "   ")
        modelVacio.load()
        assertTrue(modelVacio.state.value is SongDetailState.NotFound)
        assertTrue(repo.songs.isEmpty()) // nunca consultó el repositorio
    }

    @Test
    fun fallo_del_repositorio_da_error_sin_crashear() = runBlocking {
        repo.getSongError = IllegalStateException("BD no disponible")
        val model = SongDetailModel(repo, "content://a/b")
        model.load()
        val estado = model.state.value as SongDetailState.Error
        assertEquals("BD no disponible", estado.message)
    }

    @Test
    fun empieza_en_loading_y_no_cambia_sin_llamar_a_load() = runBlocking {
        val model = SongDetailModel(repo, "content://a/b")
        assertEquals(SongDetailState.Loading, model.state.value)
        delay(20) // sin mutaciones asíncronas: sigue Loading
        assertEquals(SongDetailState.Loading, model.state.value)
    }

    // ---------- helpers de formato (PUROS, datos reales) ----------

    @Test
    fun format_tamano_real_en_mb_con_punto_decimal() {
        assertEquals("6.0 MB", formatSongSize(6_291_456L))
        assertEquals("1.5 MB", formatSongSize(1_572_864L))
        assertEquals(null, formatSongSize(null))
        assertEquals(null, formatSongSize(0L)) // 0 = desconocido por política
        assertEquals(null, formatSongSize(-1L)) // imposible: no se inventa
    }

    @Test
    fun format_bitrate_real_en_kbps_obtenido_de_datos_medidos() {
        assertEquals("256.0 kbps", formatSongBitrate(256_000L))
        // Valor medido real no múltiplo de 1000: se muestra con un decimal,
        // sin redondear a engaño (AGENTS §8):
        assertEquals("320.5 kbps", formatSongBitrate(320_530L))
        assertEquals(null, formatSongBitrate(null))
        assertEquals(null, formatSongBitrate(0L))
    }

    @Test
    fun format_frecuencia_real_en_hz() {
        assertEquals("44100 Hz", formatSongSampleRate(44_100))
        assertEquals("48000 Hz", formatSongSampleRate(48_000))
        assertEquals(null, formatSongSampleRate(null))
        assertEquals(null, formatSongSampleRate(0))
    }
}
