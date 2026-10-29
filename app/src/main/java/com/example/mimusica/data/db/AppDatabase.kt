package com.example.mimusica.data.db

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.SQLiteConnection

/**
 * Base de datos local (FASE 4). Versión 1: esquema inicial (exportado a /schemas
 * para migraciones futuras vía el Room Gradle plugin).
 *
 * Driver: BundledSQLiteDriver (recomendación oficial de Room KMP: SQLite compilado
 * con el app, versión consistente y compatible con tests en JVM).
 */
@Database(
    entities = [SongEntity::class, PlaylistEntity::class, PlaylistSongEntity::class],
    version = 4,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        const val DATABASE_NAME = "mimusica.db"

        /** v1 → v2 (FASE 5): campos de metadatos reales y portada. */
        private val MIGRATION_1_2 = object : Migration(startVersion = 1, endVersion = 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                listOf(
                    "ALTER TABLE songs ADD COLUMN sampleRateHz INTEGER",
                    "ALTER TABLE songs ADD COLUMN channels INTEGER",
                    "ALTER TABLE songs ADD COLUMN artworkPath TEXT",
                ).forEach { sql ->
                    connection.prepare(sql).use { statement -> statement.step() }
                }
            }
        }

        /**
         * v2 → v3 (FASE 8.1, decisión D8.1/D8.2 de ARCHITECTURE §22.11):
         * columna genre TEXT y SOLO los índices justificados:
         * album (navegación por álbumes) y dateAddedMs (recientes/orden).
         * Los registros existentes NO se modifican: genre queda NULL.
         */
        private val MIGRATION_2_3 = object : Migration(startVersion = 2, endVersion = 3) {
            override suspend fun migrate(connection: SQLiteConnection) {
                listOf(
                    "ALTER TABLE songs ADD COLUMN genre TEXT",
                    "CREATE INDEX IF NOT EXISTS `index_songs_album` ON `songs` (`album`)",
                    "CREATE INDEX IF NOT EXISTS `index_songs_dateAddedMs` ON `songs` (`dateAddedMs`)",
                ).forEach { sql ->
                    connection.prepare(sql).use { statement -> statement.step() }
                }
            }
        }

        /**
         * v3 → v4 (FASE 10): playlists and their ordered song memberships.
         * Deleting either parent removes membership rows only; song files are
         * never touched by Room.
         */
        private val MIGRATION_3_4 = object : Migration(startVersion = 3, endVersion = 4) {
            override suspend fun migrate(connection: SQLiteConnection) {
                listOf(
                    """
                    CREATE TABLE IF NOT EXISTS `playlists` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL
                    )
                    """.trimIndent(),
                    """
                    CREATE TABLE IF NOT EXISTS `playlist_songs` (
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
                    CREATE INDEX IF NOT EXISTS `index_playlist_songs_playlistId_position`
                    ON `playlist_songs` (`playlistId`, `position`)
                    """.trimIndent(),
                    """
                    CREATE INDEX IF NOT EXISTS `index_playlist_songs_songUri`
                    ON `playlist_songs` (`songUri`)
                    """.trimIndent(),
                ).forEach { sql ->
                    connection.prepare(sql).use { statement -> statement.step() }
                }
            }
        }

        /** Instancia de producción o pruebas, basada en archivo en disco. */
        fun build(path: String): AppDatabase =
            Room.databaseBuilder<AppDatabase>(name = path)
                .setDriver(BundledSQLiteDriver())
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()

        /** Conveniencia para producción con el path estándar de la app. */
        fun build(context: Context): AppDatabase =
            build(context.getDatabasePath(DATABASE_NAME).absolutePath)
    }
}
