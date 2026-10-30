package com.example.mimusica.data

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.room3.Room
import com.example.mimusica.data.db.AppDatabase
import com.example.mimusica.data.repository.SongRepositoryImpl
import com.example.mimusica.domain.model.Song
import com.example.mimusica.domain.model.SongFileAvailability
import com.example.mimusica.domain.repository.SongFileValidator
import com.example.mimusica.presentation.library.LibraryFileCheckModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * FASE 4: pruebas de la capa de persistencia en JVM (sin emulador ni Robolectric).
 *
 * Estrategia (doc oficial Room KMP): base de datos Room basada en archivo con
 * BundledSQLiteDriver (SQLite compilado, no usa android.database.sqlite, que está
 * "not mocked" en tests unitarios JVM).
 */
class SongRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var dbFile: String
    private lateinit var repository: SongRepositoryImpl

    @Before
    fun setUp() {
        dbFile = tmp.newFile("test_db.db").absolutePath
        repository = SongRepositoryImpl(openDatabase().songDao())
    }

    @After
    fun tearDown() {
        // Nothing: cada test usa su propio archivo temporal.
    }

    /** Igual que en producción: builder + BundledSQLiteDriver. */
    private fun openDatabase(): AppDatabase =
        Room.databaseBuilder<AppDatabase>(name = dbFile)
            .setDriver(BundledSQLiteDriver())
            .build()

    private fun song(
        uri: String,
        title: String = "Título",
        artist: String? = "Artista",
        album: String? = "Álbum",
    ) = Song(
        uri = uri,
        title = title,
        artist = artist,
        album = album,
        durationMs = 180_000L,
        dateAddedMs = 1_760_000_000_000L,
        mimeType = "audio/mpeg",
        sizeBytes = 4_403_200L,
        bitrateBps = 192_000L,
    )

    @Test
    fun insert_y_consulta_por_id_devuelve_la_cancion() = runBlocking {
        val s = song(uri = "content://media/external/audio/1")
        repository.addSongs(listOf(s))

        val loaded = repository.getSong(s.uri)
        assertEquals(s, loaded)
    }

    @Test
    fun consulta_de_uri_inexistente_devuelve_null() = runBlocking {
        assertNull(repository.getSong("content://media/external/audio/desconocida"))
    }

    @Test
    fun biblioteca_y_busqueda_vacia_funcionan_sin_canciones() = runBlocking {
        assertTrue(repository.observeLibrary().first().isEmpty())
        assertTrue(repository.searchSongs("").isEmpty())
        assertTrue(repository.searchSongs("   ").isEmpty())
    }

    @Test
    fun insertar_el_mismo_archivo_dos_veces_no_duplica_filas() = runBlocking {
        val s = song(uri = "content://media/external/audio/2")
        val firstInsert = repository.addSongs(listOf(s))
        // Re-import: URI repetida (IGNORE conserva la fila original).
        val secondInsert = repository.addSongs(listOf(s.copy(title = "Título modificado")))

        assertEquals(1, firstInsert)
        assertEquals(0, secondInsert)

        val library = repository.observeLibrary().first()
        assertEquals(1, library.size)
        assertEquals(s.title, library.single().title)
    }

    @Test
    fun update_modifica_artist_y_album() = runBlocking {
        val s = song(uri = "content://media/external/audio/3")
        repository.addSongs(listOf(s))

        repository.updateSong(
            s.copy(artist = "Otro artista", album = null, bitrateBps = null),
        )

        val loaded = repository.getSong(s.uri)!!
        assertEquals(
            s.copy(
                artist = "Otro artista",
                album = null,
                bitrateBps = null,
                // Campos nuevos de FASE 5 quedan null cuando no vienen del origen.
            ),
            loaded,
        )
    }

    @Test
    fun delete_elimina_la_fila() = runBlocking {
        val s = song(uri = "content://media/external/audio/4")
        repository.addSongs(listOf(s))
        repository.deleteSong(s.uri)

        assertEquals(null, repository.getSong(s.uri))
        assertTrue(repository.observeLibrary().first().isEmpty())
    }

    @Test
    fun comprobar_archivo_no_disponible_no_elimina_el_registro_room() = runBlocking {
        val s = song(uri = "content://media/external/audio/no-disponible")
        repository.addSongs(listOf(s))
        val results = listOf(
            SongFileAvailability.MISSING,
            SongFileAvailability.INACCESSIBLE,
            SongFileAvailability.INVALID,
        ).iterator()
        val fileCheck = LibraryFileCheckModel(SongFileValidator { results.next() })

        for (expected in listOf(
            SongFileAvailability.MISSING,
            SongFileAvailability.INACCESSIBLE,
            SongFileAvailability.INVALID,
        )) {
            fileCheck.check(s.uri)
            assertEquals(expected, fileCheck.availability.value[s.uri])
            assertEquals(s, repository.getSong(s.uri))
        }
        assertEquals(listOf(s), repository.observeLibrary().first())
    }

    @Test
    fun busqueda_por_titulo_case_insensitive() = runBlocking {
        repository.addSongs(
            listOf(
                song(uri = "content://a/1", title = "Lo Malo de Quererte", artist = "Otra banda"),
                song(uri = "content://a/2", title = "Sin Relación", artist = "Alguien"),
            ),
        )

        val hits = repository.searchSongs("  quererte ")
        assertEquals(1, hits.size)
        assertEquals("Lo Malo de Quererte", hits.single().title)
    }

    @Test
    fun busqueda_por_artista_case_insensitive() = runBlocking {
        repository.addSongs(
            listOf(
                song(uri = "content://a/3", title = "X", artist = "El Gran Grupo"),
                song(uri = "content://a/4", title = "Y", artist = "Otros"),
            ),
        )

        val hits = repository.searchSongs("gran GRUPO")
        assertEquals("El Gran Grupo", hits.single().artist)
    }

    @Test
    fun observeLibrary_emite_la_biblioteca_ordenada_y_reactiva() = runBlocking {
        val a = song(uri = "content://a/5", title = "Canción B")
        val b = song(uri = "content://a/6", title = "Canción A")
        repository.addSongs(listOf(a, b))

        val first = repository.observeLibrary().first()
        assertEquals(listOf(b.title, a.title), first.map { it.title })

        repository.deleteSong(b.uri)
        val second = repository.observeLibrary().first()
        assertEquals(listOf(a.title), second.map { it.title })
    }

    @Test
    fun persiste_tras_cerrar_y_reabrir_la_base_de_datos() = runBlocking {
        val s = song(uri = "content://a/7")
        repository.addSongs(listOf(s))

        // Reabre la MISMA base de datos (nuevo builder, mismo archivo).
        val reopenedRepository = SongRepositoryImpl(openDatabase().songDao())
        assertEquals(s, reopenedRepository.getSong(s.uri))
    }
}
