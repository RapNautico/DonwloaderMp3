package com.example.mimusica.domain.importing

import com.example.mimusica.domain.model.Song
import com.example.mimusica.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Requisito FASE 5 Â§11: lÃ³gica de importaciÃ³n probada en JVM con FAKES
 * (sin dispositivo): vÃ¡lido, no compatible, metadatos incompletos, fallback de
 * tÃ­tulo, duplicado, lote mÃºltiple, y un archivo que falla sin detener el resto.
 * La persistencia Room se prueba en `data/SongRepositoryTest` (JVM con SQLite).
 */
class ImportAudioUseCaseTest {

    /** Fake de Domain: guarda en memoria; deduplica por URI como el repo Room real. */
    private class FakeSongRepository : SongRepository {
        val stored = mutableListOf<Song>()

        override suspend fun addSongs(songs: List<Song>): Int {
            var added = 0
            songs.forEach { song ->
                if (stored.none { it.uri == song.uri }) {
                    stored.add(song)
                    added++
                }
            }
            return added
        }

        override suspend fun updateSong(song: Song) {
            stored.removeAll { it.uri == song.uri }
            stored.add(song)
        }

        override suspend fun deleteSong(uri: String) {
            stored.removeAll { it.uri == uri }
        }

        override suspend fun getSong(uri: String): Song? = stored.firstOrNull { it.uri == uri }

        override suspend fun searchSongs(query: String): List<Song> = stored.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.artist?.contains(query, ignoreCase = true) == true
        }

        override fun observeLibrary(): Flow<List<Song>> = flowOf(stored.toList())
    }

    /** Fake de Data: devuelve los PreparedAudio definidos en cada test. */
    private class FakeImporter(private val artifacts: List<PreparedAudio>) : LocalAudioImporter {
        var requested: List<String> = emptyList()
            private set
        var progressEvents: MutableList<Int> = mutableListOf()
            private set

        override suspend fun prepare(
            uris: List<String>,
            onFileProcessed: suspend (index: Int, total: Int) -> Unit,
        ): List<PreparedAudio> {
            requested = uris
            uris.indices.forEach { index -> onFileProcessed(index + 1, uris.size) }
            return artifacts
        }
    }

    private fun validSong(uri: String, title: String = "TÃ­tulo $uri") = Song(
        uri = uri,
        title = title,
        artist = null,
        album = null,
        durationMs = 10_000L,
        dateAddedMs = 123L,
    )

    @Test
    fun `archivo valido se registra y devuelve informe de exito`() = runBlocking {
        val importer = FakeImporter(listOf(PreparedAudio("u1", "a.mp3", song = validSong("u1"))))
        val repo = FakeSongRepository()

        val report = ImportAudioUseCase(importer, repo)(listOf("u1"))

        assertEquals(1, report.songsAdded)
        assertEquals(0, report.rejectedCount)
        assertEquals(1, repo.stored.size)
    }

    @Test
    fun `archivo no compatible se rechaza sin insertarse`() = runBlocking {
        val importer = FakeImporter(
            listOf(
                PreparedAudio("u1", "video.mp4", failure = ImportFailureReason.UNSUPPORTED_FORMAT),
            ),
        )
        val report = ImportAudioUseCase(importer, FakeSongRepository())(listOf("u1"))

        assertEquals(0, report.songsAdded)
        assertEquals(1, report.rejectedCount)
        assertEquals(ImportFailureReason.UNSUPPORTED_FORMAT, report.failures.single().reason)
    }

    @Test
    fun `metadata incompleta se persiste con nulls sin inventar`() = runBlocking {
        val song = Song(uri = "u1", title = "Audio sin tÃ­tulo", artist = null, album = null,
            durationMs = 0L, dateAddedMs = 0L)
        val repo = FakeSongRepository()
        ImportAudioUseCase(FakeImporter(listOf(PreparedAudio("u1", "a.mp3", song = song))), repo)(listOf("u1"))

        assertEquals(song, repo.stored.single())
    }

    @Test
    fun `reimportar la misma uri no duplica y se informa como ya importada`() = runBlocking {
        val song = validSong("mismo.mp3")
        val repo = FakeSongRepository()
        val useCase = ImportAudioUseCase(
            FakeImporter(listOf(PreparedAudio("mismo.mp3", "mismo.mp3", song = song))),
            repo,
        )

        val first = useCase(listOf("mismo.mp3"))
        val second = useCase(listOf("mismo.mp3"))

        assertEquals(1, first.songsAdded)
        assertEquals(0, second.songsAdded)
        assertEquals(1, repo.stored.size)
        assertEquals(1, second.duplicatesSkipped)
    }

    @Test
    fun `lote con multiples archivos valida todos y muestra progreso`() = runBlocking {
        val uris = (1..8).map { "u$it" }
        val importer = FakeImporter(uris.map { PreparedAudio(it, "$it.mp3", song = validSong(it)) })
        val progress = mutableListOf<Pair<Int, Int>>()

        val report = ImportAudioUseCase(importer, FakeSongRepository())(uris, onProgress = { c, t ->
            progress.add(c to t)
        })

        assertEquals(8, report.songsAdded)
        assertEquals(8, progress.last().first)
        assertEquals(8, progress.last().second)
        assertEquals(uris, importer.requested)
    }

    @Test
    fun `un archivo invalido no detiene a los demas del lote`() = runBlocking {
        val importer = FakeImporter(
            listOf(
                PreparedAudio("valido1", "a.mp3", song = validSong("valido1")),
                PreparedAudio("corrupto", "b.opus", failure = ImportFailureReason.UNREADABLE),
                PreparedAudio("valido3", "c.flac", song = validSong("valido3")),
            ),
        )
        val repo = FakeSongRepository()

        val report = ImportAudioUseCase(importer, repo)(
            listOf("valido1", "corrupto", "valido3"),
        )

        assertEquals(2, report.songsAdded)
        assertEquals(listOf("valido1", "valido3"), repo.stored.map { it.uri })
        assertEquals(1, report.rejectedCount)
        // verifica el mensaje de UI: "No se pudieron importar %d archivos"
        assertTrue(report.songsAdded + report.rejectedCount == report.totalRequested)
    }

    @Test
    fun `importer recibe las uris tal cual pasadas por la presentacion`() = runBlocking {
        val importer = FakeImporter(emptyList())
        ImportAudioUseCase(importer, FakeSongRepository())(listOf("x1", "x2"))

        assertEquals(listOf("x1", "x2"), importer.requested)
    }
}
