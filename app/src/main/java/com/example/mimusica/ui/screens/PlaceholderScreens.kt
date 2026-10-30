package com.example.mimusica.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mimusica.R
import com.example.mimusica.components.EmptyState
import com.example.mimusica.theme.MiMusicaTheme

/**
 * Pantallas placeholder HONESTAS (FASE 3).
 * Indican al usuario que la sección se construirá en fases siguientes,
 * sin inventar datos ni funcionalidad.
 */
@Composable
fun PlaceholderScreen(titleRes: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        EmptyState(
            icon = Icons.Filled.MusicNote,
            title = stringResource(titleRes),
            description = stringResource(R.string.section_not_available_description),
        )
    }
}

@Composable
fun PlaylistsScreen() = PlaceholderScreen(R.string.playlists)

@Composable
fun DownloadsScreen() = PlaceholderScreen(R.string.downloads)
<<<<<<< HEAD

@Composable
fun SettingsScreen() = PlaceholderScreen(R.string.settings)

@Preview(name = "Playlists - placeholder", showBackground = true)
@Composable
private fun PlaylistsScreenPreview() {
    MiMusicaTheme(darkTheme = false, dynamicColor = false) {
        PlaylistsScreen()
    }
}

@Preview(name = "Descargas - placeholder", showBackground = true)
@Composable
private fun DownloadsScreenPreview() {
    MiMusicaTheme(darkTheme = false, dynamicColor = false) {
        DownloadsScreen()
    }
}

@Preview(name = "Ajustes - placeholder", showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    MiMusicaTheme(darkTheme = false, dynamicColor = false) {
        SettingsScreen()
    }
}

@Preview(name = "Placeholder - oscuro", showBackground = true)
@Composable
private fun PlaceholderDarkPreview() {
    MiMusicaTheme(darkTheme = true, dynamicColor = false) {
        PlaceholderScreen(R.string.playlists)
    }
}
=======
>>>>>>> 98d1f25b45786d37b45959e76e9e9a3a07182b12
