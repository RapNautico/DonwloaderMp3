package com.example.mimusica.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mimusica.MiMusicaApplication
import com.example.mimusica.R
import com.example.mimusica.components.EmptyState
import com.example.mimusica.domain.model.SongSort
import com.example.mimusica.presentation.library.ImportState
import com.example.mimusica.presentation.library.LibraryViewModel

/**
 * Library (requisito FASE 5 §7): lista REAL de canciones Room.
 * Estados: loading, vacía (EmptyState + CTA), importando (progreso),
 * resultado (n añadidas / rechazadas) y error parcial comprensible.
 *
 * La actividad no conoce Room: solo usa `LibraryViewModel` (Domain).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen() {
    val container = (LocalContext.current.applicationContext as MiMusicaApplication).container
    val viewModel: LibraryViewModel = viewModel(factory = container.viewModelFactory)
    val library by viewModel.library.collectAsState()
    val visibleState by viewModel.visibleSongs.collectAsState()
    val visibleSongs = visibleState.songs
    val searchError by viewModel.searchError.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var pendingUris by remember { mutableStateOf<List<String>?>(null) }

    // El permiso persistible SE PIDE en el contexto de la actividad (tras el pick).
    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            container.takePersistableUriPermissions(uris)
            pendingUris = uris.map { it.toString() }
        }
    }

    // Consume URIs pendientes una vez (no re-importar al recomponer).
    LaunchedEffect(pendingUris) {
        pendingUris?.let { uris ->
            pendingUris = null
            viewModel.importFiles(uris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.library)) },
                actions = {
                    IconButton(onClick = { pickerLauncher.launch(arrayOf("audio/*")) }) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.library_add_music))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val import = importState) {
                is ImportState.Running -> ImportProgressBanner(import.current, import.total)
                is ImportState.Done -> ImportResultBanner(import.report)
                is ImportState.Failed -> ImportErrorBanner(import.message)
                ImportState.Idle -> Unit
            }

            // Error de búsqueda (FASE 8.3): banner errorContainer, no crashea
            // ni borra la lista; se limpia solo al producirse un éxito.
            if (searchError != null) {
                SearchErrorBanner()
            }

            // Búsqueda + orden (FASE 8.3, DESIGN_SYSTEM §6.3: SearchBar full-width,
            // debounce en el ViewModel, "≡" de orden bajo la búsqueda):
            SearchField(
                query = searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                onClear = viewModel::clearSearch,
            )
            SortMenu(onSortChange = viewModel::onSortChange)

            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                library.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Filled.MusicNote,
                        title = stringResource(R.string.library_empty_title),
                        description = stringResource(R.string.library_empty_description),
                    )
                }

                searchQuery.isNotBlank() && visibleSongs.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = Icons.Filled.Search,
                            title = stringResource(R.string.library_search_no_results_title),
                            description = stringResource(R.string.library_search_no_results_description),
                        )
                    }
                }

                else -> SongList(visibleSongs)
            }
        }
    }
}

@Composable
private fun ImportProgressBanner(current: Int, total: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = stringResource(R.string.library_importing_progress, current, total),
                style = MaterialTheme.typography.labelLarge,
            )
            LinearProgressIndicator(
                progress = { current / total.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun ImportResultBanner(report: com.example.mimusica.domain.importing.ImportReport) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (report.failures.isEmpty()) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = stringResource(R.string.library_import_done, report.songsAdded),
                style = MaterialTheme.typography.labelLarge,
            )
            if (report.duplicatesSkipped > 0) {
                Text(
                    text = stringResource(R.string.library_import_duplicates, report.duplicatesSkipped),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (report.failures.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.library_import_partially_failed, report.failures.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun ImportErrorBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun SongList(songs: List<com.example.mimusica.domain.model.Song>) {
    LazyColumn {
        items(songs, key = { it.uri }) { song ->
            SongRow(song)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun SongRow(song: com.example.mimusica.domain.model.Song) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = false) { /* la reproducción real llega en FASE 10 */ }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Portada: la copia local si existe; placeholder del design system si no.
        ArtworkThumbnail(song.artworkPath)
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(song.artist, song.album).joinToString(" · ")
                    .ifEmpty { stringResource(R.string.library_unknown_artist) },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Duración real (0 = desconocida) — nunca inventada
        Text(
            text = formatDuration(song.durationMs),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
fun ArtworkThumbnail(path: String?) {
    // FASE 5: placeholder SIEMPRE (no decodificamos Bitmap todavía; la copia se
    // hace disponible para reproducir/miniaturas sin reabrir la URI de SAF).
    Surface(
        modifier = Modifier.size(40.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Icon(
            Icons.Filled.MusicNote,
            contentDescription = null,
            modifier = Modifier.padding(8.dp),
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "--:--"
    val total = durationMs / 1000
    return "%d:%02d".format(total / 60, total % 60)
}

@Composable
private fun SearchErrorBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            text = stringResource(R.string.library_search_error),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp),
        )
    }
}

/**
 * Campo de búsqueda full-width (FASE 8.3). El filtrado REAL ocurre en el
 * ViewModel (debounce 500 ms + SQL); aquí solo se edita la consulta y se
 * ofrece el borrado. La caja final de M3 SearchBar (`DockedSearchBar`) se
 * ajustará en 8.8; el comportamiento funcional es el definitivo.
 */
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        singleLine = true,
        placeholder = { Text(stringResource(R.string.library_search_hint)) },
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.library_search_clear),
                    )
                }
            }
        },
        shape = MaterialTheme.shapes.large,
    )
}

/** Selector de orden "≡" (FASE 8.3): Título A-Z/Z-A, Artista, Álbum, Recientes, Duración. */
@Composable
private fun SortMenu(
    onSortChange: (SongSort) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.padding(horizontal = 16.dp)) {
        TextButton(onClick = { expanded = true }) {
            Text(stringResource(R.string.library_sort_label))
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SongSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(sortLabel(option)) },
                    onClick = {
                        onSortChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun sortLabel(sort: SongSort): String = when (sort) {
    SongSort.TITLE_ASC -> stringResource(R.string.library_sort_title_asc)
    SongSort.TITLE_DESC -> stringResource(R.string.library_sort_title_desc)
    SongSort.ARTIST_ASC -> stringResource(R.string.library_sort_artist_asc)
    SongSort.ALBUM_ASC -> stringResource(R.string.library_sort_album_asc)
    SongSort.DATE_ADDED_DESC -> stringResource(R.string.library_sort_recent_first)
    SongSort.DATE_ADDED_ASC -> stringResource(R.string.library_sort_oldest_first)
    SongSort.DURATION_DESC -> stringResource(R.string.library_sort_duration_desc)
}
