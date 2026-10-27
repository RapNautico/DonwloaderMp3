package com.example.mimusica.data.db

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

/**
 * Base de datos local (FASE 4). Versión 1: esquema inicial (exportado a /schemas
 * para migraciones futuras vía el Room Gradle plugin).
 *
 * Driver: BundledSQLiteDriver (recomendación oficial de Room KMP: SQLite compilado
 * con el app, versión consistente y compatible con tests en JVM).
 */
@Database(
    entities = [SongEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao

    companion object {
        const val DATABASE_NAME = "mimusica.db"

        /** Instancia de producción o pruebas, basada en archivo en disco. */
        fun build(path: String): AppDatabase =
            Room.databaseBuilder<AppDatabase>(name = path)
                .setDriver(BundledSQLiteDriver())
                .build()

        /** Conveniencia para producción con el path estándar de la app. */
        fun build(context: Context): AppDatabase =
            build(context.getDatabasePath(DATABASE_NAME).absolutePath)
    }
}
