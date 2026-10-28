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
    entities = [SongEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao

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

        /** Instancia de producción o pruebas, basada en archivo en disco. */
        fun build(path: String): AppDatabase =
            Room.databaseBuilder<AppDatabase>(name = path)
                .setDriver(BundledSQLiteDriver())
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()

        /** Conveniencia para producción con el path estándar de la app. */
        fun build(context: Context): AppDatabase =
            build(context.getDatabasePath(DATABASE_NAME).absolutePath)
    }
}
