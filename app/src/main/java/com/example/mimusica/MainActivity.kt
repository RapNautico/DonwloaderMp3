package com.example.mimusica

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.mimusica.theme.MiMusicaTheme
import com.example.mimusica.ui.MiMusicaApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val app = application as MiMusicaApplication
            val settings by app.container.appSettingsRepository.settings.collectAsState()
            val darkTheme = when (settings.themeMode) {
                com.example.mimusica.domain.model.AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                com.example.mimusica.domain.model.AppThemeMode.LIGHT -> false
                com.example.mimusica.domain.model.AppThemeMode.DARK -> true
            }
            MiMusicaTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.dynamicColor,
            ) {
                MiMusicaApp()
            }
        }
    }
}
