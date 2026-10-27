package com.example.mimusica.di

import android.app.Application
import com.example.mimusica.data.db.AppDatabase
import com.example.mimusica.data.repository.SongRepositoryImpl
import com.example.mimusica.domain.repository.SongRepository

/**
 * Inyección de dependencias MANUAL (decisión de ARCHITECTURE §18: sin Hilt
 * mientras el proyecto sea monomódulo y las dependencias sean pocas).
 * Lazy: la base de datos se crea solo cuando un componente la necesita.
 */
class AppContainer(app: Application) {
    private val database: AppDatabase by lazy {
        AppDatabase.build(app)
    }

    val songRepository: SongRepository by lazy {
        SongRepositoryImpl(database.songDao())
    }
}
