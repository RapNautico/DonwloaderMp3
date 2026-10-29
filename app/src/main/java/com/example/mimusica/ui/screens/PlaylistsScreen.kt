package com.example.mimusica.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mimusica.MiMusicaApplication
import com.example.mimusica.R
import com.example.mimusica.components.EmptyState
import com.example.mimusica.components.PlaybackArtwork
import com.example.mimusica.domain.model.PlaylistDetails
import com.example.mimusica.domain.model.PlaylistSummary
import com.example.mimusica.domain.model.Song
import com.example.mimusica.presentation.playlist.PlaylistDetailViewModel
import com.example.mimusica.presentation.playlist.PlaylistsViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PlaylistsScreen(
    onOpenPlaylist: (Long) -> Unit,
) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as MiMusicaApplication
    val viewModel: PlaylistsViewModel = viewModel(factory = app.container.viewModelFactory)
    val state by viewModel.state.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<PlaylistSummary?>(null) }
    var deleting by remember { mutableStateOf<PlaylistSummary?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.playlists)) },
                actions = {
                    IconButton(onClick = { showCreate = true }) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = stringResource(R.string.playlist_create),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            (state.operationError ?: state.loadError?.takeIf { state.playlists.isNotEmpty() })
                ?.let { PlaylistErrorBanner(it) }
            when {
                state.isLoading && state.playlists.isEmpty() -> Box(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
                state.loadError != null && state.playlists.isEmpty() -> Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.QueueMusic,
                        title = stringResource(R.string.playlist_load_error_title),
                        description = state.loadError.orEmpty(),
                    )
                    TextButton(onClick = viewModel::retry) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
                state.playlists.isEmpty() -> Box(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.QueueMusic,
                        title = stringResource(R.string.playlist_empty_title),
                        description = stringResource(R.string.playlist_empty_description),
                        ctaTextRes = R.string.playlist_create,
                        onCtaClick = { showCreate = true },
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    items(state.playlists, key = { it.id }) { playlist ->
                        PlaylistListItem(
                            playlist = playlist,
                            onClick = { onOpenPlaylist(playlist.id) },
                            onRename = { editing = playlist },
                            onDelete = { deleting = playlist },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }

    if (showCreate) {
        PlaylistNameDialog(
            title = stringResource(R.string.playlist_create),
            confirmLabel = stringResource(R.string.action_create),
            onDismiss = { showCreate = false },
            onConfirm = { name ->
                viewModel.createPlaylist(name)
                showCreate = false
            },
        )
    }
    editing?.let { playlist ->
        PlaylistNameDialog(
            title = stringResource(R.string.playlist_rename),
            confirmLabel = stringResource(R.string.action_save),
            initialName = playlist.name,
            onDismiss = { editing = null },
            onConfirm = { name ->
                viewModel.renamePlaylist(playlist.id, name)
                editing = null
            },
        )
    }
    deleting?.let { playlist ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.playlist_delete_title)) },
            text = { Text(stringResource(R.string.playlist_delete_message, playlist.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePlaylist(playlist.id)
                        deleting = null
                    },
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun PlaylistListItem(
    playlist: PlaylistSummary,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PlaybackArtwork(
            artworkPath = playlist.artworkPath,
            title = playlist.name,
            modifier = Modifier.size(56.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(playlist.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                pluralStringResource(R.plurals.playlist_song_count, playlist.songCount, playlist.songCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box {
            IconButton(
                onClick = { expanded = true },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.playlist_actions, playlist.name))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.playlist_rename)) },
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    onClick = { expanded = false; onRename() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_delete)) },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                    onClick = { expanded = false; onDelete() },
                )
            }
        }
    }
}

@Composable
private fun PlaylistNameDialog(
    title: String,
    confirmLabel: String,
    initialName: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.playlist_name)) },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PlaylistDetailScreen(
    playlistId: Long,
    onBack: () -> Unit,
    onPlaySongs: (List<Song>, String) -> Unit,
) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as MiMusicaApplication
    val viewModel: PlaylistDetailViewModel = viewModel(
        key = "playlist-detail-$playlistId",
        factory = app.container.playlistDetailViewModelFactory(playlistId),
    )
    val state by viewModel.state.collectAsState()
    var showSongPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.playlist?.playlist?.name ?: stringResource(R.string.playlists)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            state.error != null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    icon = Icons.Filled.MusicNote,
                    title = stringResource(R.string.playlist_load_error_title),
                    description = state.error.orEmpty(),
                )
            }
            state.playlist == null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    title = stringResource(R.string.playlist_not_found),
                    description = stringResource(R.string.playlist_not_found_description),
                    ctaTextRes = R.string.nav_back,
                    onCtaClick = onBack,
                )
            }
            else -> PlaylistSongsContent(
                playlist = state.playlist!!,
                operationError = state.operationError,
                onAddSongs = { showSongPicker = true },
                onPlay = { song -> onPlaySongs(state.playlist!!.songs, song.uri) },
                onMove = viewModel::moveSong,
                onRemove = viewModel::removeSong,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        }
    }

    if (showSongPicker) {
        AddSongsDialog(
            librarySongs = state.librarySongs,
            existingUris = state.playlist?.songs?.mapTo(mutableSetOf()) { it.uri }.orEmpty(),
            onAdd = viewModel::addSong,
            onDismiss = { showSongPicker = false },
        )
    }
}

@Composable
private fun PlaylistSongsContent(
    playlist: PlaylistDetails,
    operationError: String?,
    onAddSongs: () -> Unit,
    onPlay: (Song) -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        operationError?.let { PlaylistErrorBanner(it) }
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlaybackArtwork(playlist.playlist.artworkPath, playlist.playlist.name, Modifier.size(88.dp))
            Column(Modifier.weight(1f)) {
                Text(playlist.playlist.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    pluralStringResource(R.plurals.playlist_song_count, playlist.songs.size, playlist.songs.size),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { playlist.songs.firstOrNull()?.let(onPlay) },
                        enabled = playlist.songs.isNotEmpty(),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Text(stringResource(R.string.playlist_play_all))
                    }
                    OutlinedButton(onClick = onAddSongs) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(stringResource(R.string.playlist_add_songs))
                    }
                }

            }
        }
        if (playlist.songs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Filled.MusicNote,
                    title = stringResource(R.string.playlist_songs_empty_title),
                    description = stringResource(R.string.playlist_songs_empty_description),
                    ctaTextRes = R.string.playlist_add_songs,
                    onCtaClick = onAddSongs,
                )
            }
        } else {
            LazyColumn {
                items(playlist.songs, key = { it.uri }) { song ->
                    val index = playlist.songs.indexOf(song)
                    PlaylistSongRow(
                        song = song,
                        index = index,
                        size = playlist.songs.size,
                        onClick = { onPlay(song) },
                        onMoveUp = { onMove(index, index - 1) },
                        onMoveDown = { onMove(index, index + 1) },
                        onRemove = { onRemove(song.uri) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun PlaylistErrorBanner(message: String) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun PlaylistSongRow(
    song: Song,
    index: Int,
    size: Int,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f).clickable(onClick = onClick).padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlaybackArtwork(song.artworkPath, song.title, Modifier.size(48.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    song.artist ?: stringResource(R.string.library_unknown_artist),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column {
            IconButton(onClick = onMoveUp, enabled = index > 0) {
                Icon(Icons.Filled.ArrowUpward, contentDescription = stringResource(R.string.playlist_move_up, song.title))
            }
            IconButton(onClick = onMoveDown, enabled = index < size - 1) {
                Icon(Icons.Filled.ArrowDownward, contentDescription = stringResource(R.string.playlist_move_down, song.title))
            }
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.playlist_remove_song, song.title))
        }
    }
}

@Composable
private fun AddSongsDialog(
    librarySongs: List<Song>,
    existingUris: Set<String>,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.playlist_add_songs)) },
        text = {
            if (librarySongs.isEmpty()) {
                Text(stringResource(R.string.playlist_library_empty))
            } else {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(librarySongs, key = { it.uri }) { song ->
                        val alreadyAdded = song.uri in existingUris
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    song.artist ?: stringResource(R.string.library_unknown_artist),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            TextButton(
                                onClick = { onAdd(song.uri) },
                                enabled = !alreadyAdded,
                            ) {
                                Text(
                                    stringResource(
                                        if (alreadyAdded) R.string.playlist_song_added else R.string.action_add,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) }
        },
    )
}
