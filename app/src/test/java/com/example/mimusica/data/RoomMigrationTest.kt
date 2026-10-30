package com.example.mimusica.data

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.example.mimusica.data.db.AppDatabase
import com.example.mimusica.data.repository.SongRepositoryImpl
import com.example.mimusica.domain.model.Song
import com.example.mimusica.domain.model.SongSourceIdentity
import com.example.mimusica.domain.model.SongSourceRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Migraciones Room históricas hasta v5 en JVM (BundledSQLiteDriver, sin emulador).
 *
 * Estrategia sin `room3-testing` (dependencia NO aprobada): se crea a mano un
 * archivo de base de datos con el esquema exacto de una versión exportada,
 * identity hash y user_version; Room abre el archivo mediante
 * `AppDatabase.build()` y ejecuta la cadena de migraciones registrada.
 *
 * Los identity hashes son constantes de los esquemas exportados; si cambian,
 * las pruebas fallan y obligan a revisar el fixture histórico.
 */
class RoomMigrationTest {

    @get:Rule
    val tmp = TemporaryFolder()

    /** Identity hash del schema v2 exportado (app/schemas/2.json, 2026-10-28). */
    private val identityHashV2 = "0071e59fa966f13cab292a4242891126"
    /** Identity hash del schema v1 exportado (app/schemas/1.json). */
    private val identityHashV1 = "963d146075f2eda33158d0fddb3b140b"
    /** Identity hash del schema v3 exportado (app/schemas/.../3.json). */
    private val identityHashV3 = "08f33a1ce8c8e982c701ef04c5cf88ed"
    /** Identity hash del schema v4 exportado (app/schemas/4.json). */
    private val identityHashV4 = "9926a73f10a3d2a9f31acfc21e66cf29"

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

    /** Crea el esquema v1 según el JSON exportado, con una fila histórica. */
    private fun crearBaseV1ConCancion(uri: String, titulo: String, artista: String?) {
        dbFile = tmp.newFile("v1.db").absolutePath
        BundledSQLiteDriver().open(dbFile).use { conn ->
            fun exec(sql: String, bind: (androidx.sqlite.SQLiteStatement.() -> Unit)? = null) {
                conn.prepare(sql).use { stmt ->
                    bind?.invoke(stmt)
                    stmt.step()
                }
            }
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
                    PRIMARY KEY(`uri`)
                )
                """.trimIndent(),
            )
            exec("CREATE INDEX IF NOT EXISTS `index_songs_title` ON `songs` (`title`)")
            exec("CREATE INDEX IF NOT EXISTS `index_songs_artist` ON `songs` (`artist`)")
            exec(
                "CREATE TABLE IF NOT EXISTS `room_master_table` (`id` INTEGER PRIMARY KEY, `identity_hash` TEXT)",
            )
            exec("INSERT OR REPLACE INTO `room_master_table` (`id`, `identity_hash`) VALUES (42, ?)") {
                bindText(1, identityHashV1)
            }
            exec("PRAGMA user_version = 1")
            exec("INSERT INTO `songs` VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)") {
                bindText(1, uri)
                bindText(2, titulo)
                if (artista != null) bindText(3, artista) else bindNull(3)
                bindText(4, "Álbum v1")
                bindLong(5, 205_000L)
                bindLong(6, 777_000_000L)
                bindText(7, "audio/mpeg")
                bindLong(8, 8_000_000L)
                bindLong(9, 320_000L)
            }
        }
    }

    @Test
    fun migracion_1_a_5_conserva_datos_y_crea_las_tablas_de_playlist() = runBlocking {
        crearBaseV1ConCancion("content://sa/v1cancion", "Canción v1", "Artista v1")

        val db = AppDatabase.build(dbFile)
        val repo = SongRepositoryImpl(db.songDao())
        val canciones = repo.observeLibrary().first()

        assertEquals(1, canciones.size)
        val song = canciones.single()
        assertEquals("content://sa/v1cancion", song.uri)
        assertEquals("Canción v1", song.title)
        assertEquals("Artista v1", song.artist)
        assertEquals("Álbum v1", song.album)
        assertEquals(205_000L, song.durationMs)
        assertEquals(777_000_000L, song.dateAddedMs)
        assertEquals("audio/mpeg", song.mimeType)
        assertEquals(8_000_000L, song.sizeBytes)
        assertEquals(320_000L, song.bitrateBps)
        assertNull(song.sampleRateHz)
        assertNull(song.channels)
        assertNull(song.artworkPath)
        assertNull(song.genre)

        db.close()
        BundledSQLiteDriver().open(dbFile).use { conn ->
            val columns = mutableListOf<String>()
            conn.prepare("PRAGMA table_info(`songs`)").use { statement ->
                while (statement.step()) columns += statement.getText(1)
            }
            assertEquals(
                listOf(
                    "uri",
                    "title",
                    "artist",
                    "album",
                    "durationMs",
                    "dateAddedMs",
                    "mimeType",
                    "sizeBytes",
                    "bitrateBps",
                    "sampleRateHz",
                    "channels",
                    "artworkPath",
                    "genre",
                ),
                columns,
            )

            val indices = mutableSetOf<String>()
            conn.prepare("PRAGMA index_list(`songs`)").use { statement ->
                while (statement.step()) indices += statement.getText(1)
            }
            assertEquals(
                setOf(
                    "index_songs_title",
                    "index_songs_artist",
                    "index_songs_album",
                    "index_songs_dateAddedMs",
                ),
                indices.filterNot { it.startsWith("sqlite_autoindex_") }.toSet(),
            )

            conn.prepare("PRAGMA user_version").use { statement ->
                assertTrue(statement.step())
                assertEquals(5L, statement.getLong(0))
                val tables = mutableSetOf<String>()
                conn.prepare("SELECT name FROM sqlite_master WHERE type = 'table'").use { statement ->
                    while (statement.step()) tables += statement.getText(0)
                }
                assertTrue("playlists" in tables)
                assertTrue("playlist_songs" in tables)
                assertTrue("song_source_identities" in tables)
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

    @Test
    fun migracion_3_a_5_conserva_canciones_y_crea_playlist_con_relacion() = runBlocking {
        crearBaseV2ConCancion("content://sa/v3-song", "Canción v3", "Artista v3")
        BundledSQLiteDriver().open(dbFile).use { conn ->
            listOf(
                "ALTER TABLE songs ADD COLUMN genre TEXT",
                "CREATE INDEX IF NOT EXISTS `index_songs_album` ON `songs` (`album`)",
                "CREATE INDEX IF NOT EXISTS `index_songs_dateAddedMs` ON `songs` (`dateAddedMs`)",
                "UPDATE room_master_table SET identity_hash = '$identityHashV3' WHERE id = 1",
                "PRAGMA user_version = 3",
            ).forEach { sql ->
                conn.prepare(sql).use { statement -> statement.step() }
            }
        }

        val db = AppDatabase.build(dbFile)
        val songRepository = SongRepositoryImpl(db.songDao())
        val playlistRepository = com.example.mimusica.data.repository.PlaylistRepositoryImpl(db.playlistDao())
        val existingSong = songRepository.getSong("content://sa/v3-song")
        assertEquals("Canción v3", existingSong?.title)

        val playlistId = playlistRepository.createPlaylist("Favoritas")
        assertTrue(playlistRepository.addSong(playlistId, "content://sa/v3-song"))
        assertEquals(
            listOf("content://sa/v3-song"),
            playlistRepository.observePlaylist(playlistId).first()?.songs?.map { it.uri },
        )
        db.close()
    }

    @Test
    fun migracion_4_a_5_conserva_playlist_y_deduplica_aliases_de_media_store() = runBlocking {
        val originalUri = "content://saf/music-track"
        crearBaseV2ConCancion(originalUri, "Canción importada", "Artista")
        BundledSQLiteDriver().open(dbFile).use { conn ->
            listOf(
                "ALTER TABLE songs ADD COLUMN genre TEXT",
                "CREATE INDEX IF NOT EXISTS `index_songs_album` ON `songs` (`album`)",
                "CREATE INDEX IF NOT EXISTS `index_songs_dateAddedMs` ON `songs` (`dateAddedMs`)",
                """
                CREATE TABLE `playlists` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL
                )
                """.trimIndent(),
                """
                CREATE TABLE `playlist_songs` (
                    `playlistId` INTEGER NOT NULL,
                    `songUri` TEXT NOT NULL,
                    `position` INTEGER NOT NULL,
                    PRIMARY KEY(`playlistId`, `songUri`),
                    FOREIGN KEY(`playlistId`) REFERENCES `playlists`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`songUri`) REFERENCES `songs`(`uri`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
                """
                CREATE INDEX `index_playlist_songs_playlistId_position`
                ON `playlist_songs` (`playlistId`, `position`)
                """.trimIndent(),
                "CREATE INDEX `index_playlist_songs_songUri` ON `playlist_songs` (`songUri`)",
                "INSERT INTO `playlists` (`id`, `name`) VALUES (1, 'Favoritas')",
                "INSERT INTO `playlist_songs` (`playlistId`, `songUri`, `position`) VALUES (1, '$originalUri', 0)",
                "UPDATE room_master_table SET identity_hash = '$identityHashV4' WHERE id = 1",
                "PRAGMA user_version = 4",
            ).forEach { sql ->
                conn.prepare(sql).use { statement -> statement.step() }
            }
        }

        val db = AppDatabase.build(dbFile)
        val repository = SongRepositoryImpl(db.songDao(), db.songSourceIdentityDao())
        val playlists = com.example.mimusica.data.repository.PlaylistRepositoryImpl(db.playlistDao())
        val sourceKey = "music:primary:Music/track.mp3"
        val originalSong = repository.getSong(originalUri)!!

        assertEquals(
            listOf(originalUri),
            playlists.observePlaylist(1L).first()?.songs?.map { it.uri },
        )
        assertEquals(
            0,
            repository.ingest(
                listOf(
                    SongSourceRecord(
                        originalSong,
                        SongSourceIdentity(sourceKey, modifiedAtMs = 1_000L, sizeBytes = 120L),
                    ),
                ),
            ),
        )

        val mediaStoreUri = "content://media/external/audio/media/9"
        assertEquals(
            0,
            repository.ingest(
                listOf(
                    SongSourceRecord(
                        originalSong.copy(
                            uri = mediaStoreUri,
                            title = "Metadata actualizada",
                            sizeBytes = 240L,
                        ),
                        SongSourceIdentity(sourceKey, modifiedAtMs = 2_000L, sizeBytes = 240L),
                    ),
                ),
            ),
        )

        assertEquals("Metadata actualizada", repository.getSong(originalUri)?.title)
        assertNull(repository.getSong(mediaStoreUri))
        assertEquals(
            listOf(originalUri),
            playlists.observePlaylist(1L).first()?.songs?.map { it.uri },
        )
        assertEquals(
            mapOf(sourceKey to com.example.mimusica.domain.model.SongSourceState(2_000L, 240L)),
            repository.findKnownIdentities(listOf(sourceKey)),
        )
        db.close()
    }
}
