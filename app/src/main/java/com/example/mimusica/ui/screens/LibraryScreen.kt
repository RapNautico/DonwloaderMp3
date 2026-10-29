package com.example.mimusica.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
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
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mimusica.MiMusicaApplication
import com.example.mimusica.R
import com.example.mimusica.components.EmptyState
import com.example.mimusica.components.LibraryLoadError
import com.example.mimusica.components.LibraryLoadingSkeleton
import com.example.mimusica.components.SongList
import com.example.mimusica.components.SongRowMenu
import com.example.mimusica.domain.model.Song
import com.example.mimusica.domain.model.SongFileAvailability
import com.example.mimusica.domain.model.SongSort
import com.example.mimusica.presentation.library.ImportState
import com.example.mimusica.presentation.library.LibraryViewModel

/**
 * Library (requisito FASE 5 §7): lista REAL de canciones Room.
 * Estados: loading, vacía (EmptyState + CTA), importando (progreso),
 * resultado (n añadidas / rechazadas) y error parcial comprensible.
 *
 * FASE 8.8: las pestañas seleccionan Canciones/Artistas/Álbumes dentro del
 * grafo existente; artistas y álbumes conservan sus resúmenes SQL.
 * FASE 8.5: [onOpenSong] abre el DETALLE (bottom sheet) de una canción.
 * (El menú de gestión llega en 8.6; la comprobación de archivos en 8.7.)
 *
 * La actividad no conoce Room: solo usa `LibraryViewModel` (Domain).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onSelectTab: (LibraryTab) -> Unit,
    onOpenSong: (String) -> Unit,
) {
    val container = (LocalContext.current.applicationContext as MiMusicaApplication).container
    val viewModel: LibraryViewModel = viewModel(factory = container.viewModelFactory)
    val libraryState by viewModel.libraryState.collectAsState()
    val library = libraryState.songs
    val visibleState by viewModel.visibleSongs.collectAsState()
    val visibleSongs = visibleState.songs
    val searchError by viewModel.searchError.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    // Gestión (FASE 8.6): ConfirmDialog obligatorio ANTES de eliminar.
    val manageBusy by viewModel.manageBusy.collectAsState()
    val manageError by viewModel.manageError.collectAsState()
    val fileAvailability by viewModel.fileAvailability.collectAsState()
    var pendingDelete by remember { mutableStateOf<Song?>(null) }
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
    val onImport = { pickerLauncher.launch(arrayOf("audio/*")) }

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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onImport,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.library_add_music)) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LibrarySectionTabs(LibraryTab.SONGS, onSelectTab)

            when (val import = importState) {
                is ImportState.Running -> ImportProgressBanner(import.current, import.total)
                is ImportState.Done -> ImportResultBanner(
                    report = import.report,
                    onDismiss = viewModel::dismissImportResult,
                )
                is ImportState.Failed -> ImportErrorBanner(import.message)
                ImportState.Idle -> Unit
            }

            // Error de búsqueda (FASE 8.3): banner errorContainer, no crashea
            // ni borra la lista; se limpia solo al producirse un éxito.
            if (searchError != null) {
                SearchErrorBanner()
            }

            // Error de GESTIÓN (FASE 8.6): la eliminación falló; no crashea,
            // el listado queda como estaba y se limpia con el próximo éxito.
            if (manageError != null) {
                ManageErrorBanner()
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
                libraryState.error != null && library.isEmpty() -> LibraryLoadError(
                    onRetry = viewModel::retryLibraryLoad,
                )

                libraryState.isLoading && library.isEmpty() -> LibraryLoadingSkeleton()

                visibleState.isLoading -> LibraryLoadingSkeleton()

                library.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Filled.MusicNote,
                        title = stringResource(R.string.library_empty_title),
                        description = stringResource(R.string.library_empty_description),
                        ctaTextRes = R.string.library_add_music,
                        onCtaClick = onImport,
                    )
                }

                searchQuery.trim() != visibleState.query -> LibraryLoadingSkeleton()

                searchQuery.isNotBlank() && visibleSongs.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = Icons.Filled.Search,
                            title = stringResource(R.string.library_search_no_results_title),
                            description = stringResource(R.string.library_search_no_results_description),
                        )
                    }
                }

                else -> {
                    if (libraryState.error != null) {
                        LibraryLoadErrorBanner(onRetry = viewModel::retryLibraryLoad)
                    }
                    SongList(
                        visibleSongs,
                        onSongClick = { song -> onOpenSong(song.uri) },
                        fileAvailability = fileAvailability,
                        menuFactory = { song ->
                            SongRowMenu(
                                onInformation = { onOpenSong(song.uri) },
                                // La eliminación siempre requiere confirmación.
                                onDelete = { pendingDelete = song },
                                onCheckFile = { viewModel.checkLibrarySongFile(song.uri) },
                                isCheckingFile = fileAvailability[song.uri] == SongFileAvailability.CHECKING,
                            )
                        },
                    )
                }
            }
        }
    }

    // ConfirmDialog de eliminación (PLAN 8.6: SOLO registro Room; el archivo
    // físico no se borra). Cancelar cierra sin cambios; Eliminar decidió ya.
    pendingDelete?.let { cancion ->
        AlertDialog(
            onDismissRequest = { if (!manageBusy) pendingDelete = null },
            title = { Text(stringResource(R.string.delete_dialog_title)) },
            text = {
                Text(
                    text = stringResource(R.string.delete_dialog_message, cancion.title),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        viewModel.deleteLibrarySong(cancion.uri)
                    },
                    enabled = !manageBusy,
                ) {
                    Text(
                        stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingDelete = null },
                    enabled = !manageBusy,
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
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
private fun ImportResultBanner(
    report: com.example.mimusica.domain.importing.ImportReport,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (report.failures.isEmpty()) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
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
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.library_import_dismiss),
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

/** Banner de fallo de GESTIÓN (8.6): mismo estilo; no borra la lista. */
@Composable
private fun ManageErrorBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            text = stringResource(R.string.library_delete_error),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp),
        )
    }
}

/**
 * SearchBar full-width (FASE 8.8). El filtrado REAL ocurre en el ViewModel
 * (debounce 500 ms + SQL); el campo no abre resultados de sugerencias porque
 * la lista filtrada permanece en la pantalla.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    val searchBarState = rememberSearchBarState()
    SearchBar(
        state = searchBarState,
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = onQueryChange,
                expanded = false,
                onExpandedChange = {},
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
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun LibraryLoadErrorBanner(onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.library_load_error_description),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.action_retry))
            }
        }
    }
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
