package com.example.mimusica

import android.app.Application
import android.app.Activity
import android.os.Bundle
import com.example.mimusica.di.AppContainer

/**
 * Contenedor vivido a nivel de aplicación: una sola base de datos y repositorio
 * para todo el proceso (doc Room: una instancia por proceso), además del
 * controlador compartido del reproductor.
 */
class MiMusicaApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var startedActivities = 0

            override fun onActivityStarted(activity: Activity) {
                startedActivities += 1
                if (startedActivities == 1) container.startMusicMonitoring()
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivities = (startedActivities - 1).coerceAtLeast(0)
                if (startedActivities == 0) container.stopMusicMonitoring()
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    override fun onTerminate() {
        container.release()
        super.onTerminate()
    }
}
