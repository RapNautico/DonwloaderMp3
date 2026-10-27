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
import androidx.compose.ui.unit.dp
import com.example.mimusica.R
import com.example.mimusica.components.EmptyState

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
fun LibraryScreen() = PlaceholderScreen(R.string.library)

@Composable
fun PlaylistsScreen() = PlaceholderScreen(R.string.playlists)

@Composable
fun DownloadsScreen() = PlaceholderScreen(R.string.downloads)

@Composable
fun SettingsScreen() = PlaceholderScreen(R.string.settings)
