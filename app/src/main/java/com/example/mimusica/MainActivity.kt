package com.example.mimusica

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.mimusica.theme.MiMusicaTheme
import com.example.mimusica.ui.MiMusicaApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MiMusicaTheme {
                MiMusicaApp()
            }
        }
    }
}
