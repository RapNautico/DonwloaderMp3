package com.example.mimusica.data

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.example.mimusica.data.db.AppDatabase
import com.example.mimusica.data.repository.SongRepositoryImpl
import com.example.mimusica.domain.model.Song
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * FASE 8.1: prueba de la migración Room v2→v3 en JVM (BundledSQLiteDriver, sin emulador).
 *
 * Estrategia sin `room3-testing` (dependencia NO aprobada): se crea a mano un
 * archivo de base de datos con el esquema EXACTO de la versión 2 (tabla `songs`
 * de 12 columnas, sus dos índices y `room_master_table` con el identity hash
 * real del schema exportado `app/schemas/.../2.json`), y a continuación se abra
 * con Room vía `AppDatabase.build()` (que registra MIGRATION_2_3): Room debe
 * detectar la versión 2, ejecutar la migración y conservar TODO el dato.
 *
 * El identity hash de la v2 es constante (esquema congelado y exportado); si
 * cambiara, este test fallará y obligará a revisar la migración.
 */
class RoomMigrationTest {

    @get:Rule
    val tmp = TemporaryFolder()

    /** Identity hash del schema v2 exportado (app/schemas/2.json, 2026-10-28). */
    private val identityHashV2 = "0071e59fa966f13cab292a4242891126"

    private lateinit var dbFile: String

    /** Crea a mano el esquema de la VERSION 2 con una canción ya dentro. */
    private fun crearBaseV2ConCancion(uri: String, titulo: String, artista: String?) {
        dbFile = tmp.newFile("v2.db").absolutePath
        BundledSQLiteDriver().open(dbFile).use { conn ->
            fun exec(sql: String, bind: (androidx.sqlite.SQLiteStatement.() -> Unit)? = null) {
                conn.prepare(sql).use { stmt ->
                    bind?.invoke(stmt)
                    stmt.step()
                }
            }
            // DDL idéntico al esquema v2 (app/schemas/.../2.json):
            exec(
                """
                CREATE TABLE IF NOT EXISTS `songs` (
                    `uri` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `artist` TEXT,
                    `album` TEXT,
                    `durationMs` INTEGER NOT NULL,
                    `dateAddedMs` INTEGER NOT NULL,
                    `mimeType` TEXT,
                    `sizeBytes` INTEGER,
                    `bitrateBps` INTEGER,
                    `sampleRateHz` INTEGER,
                    `channels` INTEGER,
                    `artworkPath` TEXT,
                    PRIMARY KEY(`uri`)
                )
                """.trimIndent(),
            )
            exec("CREATE INDEX IF NOT EXISTS `index_songs_title` ON `songs` (`title`)")
            exec("CREATE INDEX IF NOT EXISTS `index_songs_artist` ON `songs` (`artist`)")
            exec(
                "CREATE TABLE IF NOT EXISTS `room_master_table` (`id` INTEGER PRIMARY KEY, `identity_hash` TEXT)",
            )
            exec("INSERT INTO `room_master_table` (`id`, `identity_hash`) VALUES (1, ?)") {
                bindText(1, identityHashV2)
            }
            // Versión de usuario de una installs antigua (v2): sin esto Room trataría
            // el archivo como "pre-packaged" versión 0 y lo validaría contra el schema v3.
            exec("PRAGMA user_version = 2")
            // Canción v2 de ejemplo (artista real, álbum null, sin portada):
            exec("INSERT INTO `songs` VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)") {
                bindText(1, uri)
                bindText(2, titulo)
                if (artista != null) bindText(3, artista) else bindNull(3)
                bindNull(4) // album
                bindLong(5, 205_000L) // durationMs
                bindLong(6, 777_000_000L) // dateAddedMs
                bindText(7, "audio/mpeg")
                bindLong(8, 8_000_000L) // sizeBytes
                bindLong(9, 320_000L) // bitrateBps
                bindLong(10, 44_100L) // sampleRateHz
                bindLong(11, 2L) // channels
                bindNull(12) // artworkPath
            }
        }
    }

    @Test
    fun migracion_2_a_3_conserva_los_registros_y_genre_queda_null() = runBlocking {
        crearBaseV2ConCancion("content://sa/v2cancion", "Canción v2", "Artista v2")

        val db = AppDatabase.build(dbFile) // registra MIGRATION_2_3
        val repo = SongRepositoryImpl(db.songDao())

        // La migración se ejecuta al primer uso; la biblioteca debe contener EXACTAMENTE
        // la canción original, con TODOS sus datos intactos:
        val filas = repo.observeLibrary().first()
        assertEquals(1, filas.size)
        val song = filas.single()
        assertEquals("content://sa/v2cancion", song.uri)
        assertEquals("Canción v2", song.title)
        assertEquals("Artista v2", song.artist)
        assertNull(song.album)
        assertEquals(205_000L, song.durationMs)
        assertEquals(777_000_000L, song.dateAddedMs)
        assertEquals("audio/mpeg", song.mimeType)
        assertEquals(8_000_000L, song.sizeBytes)
        assertEquals(320_000L, song.bitrateBps)
        assertEquals(44_100, song.sampleRateHz)
        assertEquals(2, song.channels)
        assertNull(song.artworkPath)
        // La columna nueva queda NULL para registros anteriores (decisión D8.1/D8.2):
        assertNull(song.genre)

        db.close()
    }

    @Test
    fun despues_de_migrar_persisten_canciones_con_genre() = runBlocking {
        crearBaseV2ConCancion("content://sa/reimportable", "Antes", null)
        val db = AppDatabase.build(dbFile)
        val repo = SongRepositoryImpl(db.songDao())

        // Reimportación del URI existente con genre real: IGNORE conserva la fila v2 intacta.
        val insertadas = repo.addSongs(
            listOf(
                Song(
                    uri = "content://sa/reimportable",
                    title = "Antes",
                    artist = null,
                    album = "Álbum real",
                    durationMs = 205_000L,
                    dateAddedMs = 777_000_000L,
                    mimeType = "audio/mpeg",
                    genre = "Rock",
                ),
            ),
        )
        assertEquals(0, insertadas) // ya existía: no duplicada
        assertNull(repo.getSong("content://sa/reimportable")?.genre) // fila original intacta

        // Canción NUEVA tras la migración: el género real se persiste y se lee.
        repo.addSongs(
            listOf(
                Song(
                    uri = "content://sa/nueva",
                    title = "Nueva con género",
                    artist = "Artista",
                    album = "Álbum",
                    durationMs = 1_000L,
                    dateAddedMs = 888_000_000L,
                    genre = "Jazz",
                ),
            ),
        )
        assertEquals("Jazz", repo.getSong("content://sa/nueva")?.genre)
        // Y la coexistencia en la misma biblioteca es correcta:
        assertEquals(2, repo.observeLibrary().first().size)

        db.close()
    }
}
