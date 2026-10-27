package com.example.mimusica

import android.app.Application
import com.example.mimusica.di.AppContainer

/**
 * Contenedor vivido a nivel de aplicación: una sola base de datos y repositorio
 * para todo el proceso (doc Room: una instancia por proceso).
 * Se conectará a ViewModels en FASE 5.
 */
class MiMusicaApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
