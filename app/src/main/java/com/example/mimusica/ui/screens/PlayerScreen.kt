package com.example.mimusica.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mimusica.R
import com.example.mimusica.components.EmptyState
import com.example.mimusica.components.PlaybackArtwork
import com.example.mimusica.domain.model.PlaybackState
import com.example.mimusica.domain.model.PlaybackStatus
import com.example.mimusica.domain.model.PlaybackRepeatMode
import com.example.mimusica.domain.model.PlaybackTrack
import com.example.mimusica.presentation.player.PlayerViewModel
import com.example.mimusica.theme.MiMusicaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    PlayerScreenContent(
        state = state,
        onBack = onBack,
        onSeekTo = viewModel::seekTo,
        onPrevious = viewModel::skipToPrevious,
        onBackTen = { viewModel.seekBy(-10_000L) },
        onToggle = viewModel::togglePlayPause,
        onForwardTen = { viewModel.seekBy(10_000L) },
        onNext = viewModel::skipToNext,
        onSetVolume = viewModel::setVolume,
        onCycleRepeatMode = viewModel::cycleRepeatMode,
        onToggleShuffle = viewModel::toggleShuffle,
        onPlayTrackAt = viewModel::playTrackAt,
        onStop = viewModel::stop,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerScreenContent(
    state: PlaybackState,
    onBack: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onPrevious: () -> Unit,
    onBackTen: () -> Unit,
    onToggle: () -> Unit,
    onForwardTen: () -> Unit,
    onNext: () -> Unit,
    onSetVolume: (Float) -> Unit,
    onCycleRepeatMode: () -> Unit,
    onToggleShuffle: () -> Unit,
    onPlayTrackAt: (Int) -> Unit,
    onStop: () -> Unit,
) {
    val track = state.currentTrack
    var seekValue by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }
    var showStopConfirmation by remember { mutableStateOf(false) }
    val progressDescription = stringResource(R.string.player_progress)
    val volumeDescription = stringResource(R.string.player_volume)
    val stopDescription = stringResource(R.string.player_stop)

    LaunchedEffect(track?.uri) {
        seekValue = state.positionMs.toFloat()
        isSeeking = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.player_fullscreen)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (track == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                if (state.status == PlaybackStatus.ERROR) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(
                            text = stringResource(R.string.player_status_error),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                } else {
                    EmptyState(
                        icon = Icons.Filled.MusicNote,
                        title = stringResource(R.string.player_no_track),
                        description = stringResource(R.string.player_no_track_description),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 24.dp,
                    vertical = 16.dp,
                ),
            ) {
                item {
                    PlayerArtwork(track)
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                    )
                    Text(
                        text = track.artist ?: stringResource(R.string.library_unknown_artist),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PlayerStatus(state.status, state.isPlaying)
                    Slider(
                        value = if (isSeeking) {
                            seekValue
                        } else {
                            state.positionMs.toFloat().coerceIn(
                                0f,
                                state.durationMs.coerceAtLeast(1L).toFloat(),
                            )
                        },
                        onValueChange = {
                            seekValue = it
                            isSeeking = true
                        },
                        onValueChangeFinished = {
                            if (isSeeking) onSeekTo(seekValue.toLong())
                            isSeeking = false
                        },
                        valueRange = 0f..state.durationMs.coerceAtLeast(1L).toFloat(),
                        enabled = state.durationMs > 0L,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = progressDescription
                            },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(formatDuration(state.positionMs), style = MaterialTheme.typography.labelSmall)
                        Text(formatDuration(state.durationMs), style = MaterialTheme.typography.labelSmall)
                    }
                    PlaybackControls(
                        state = state,
                        onPrevious = onPrevious,
                        onBackTen = onBackTen,
                        onToggle = onToggle,
                        onForwardTen = onForwardTen,
                        onNext = onNext,
                    )
                    PlaybackModeControls(
                        state = state,
                        onCycleRepeatMode = onCycleRepeatMode,
                        onToggleShuffle = onToggleShuffle,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = stringResource(R.string.player_volume),
                        )
                        Slider(
                            value = state.volume,
                            onValueChange = onSetVolume,
                            valueRange = 0f..1f,
                            modifier = Modifier
                                .weight(1f)
                                .semantics {
                                    contentDescription = volumeDescription
                                },
                        )
                        IconButton(
                            onClick = { showStopConfirmation = true },
                            modifier = Modifier
                                .size(48.dp)
                                .semantics {
                                    contentDescription = stopDescription
                                },
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = null)
                        }
                    }
                    Text(
                        text = stringResource(R.string.player_queue),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp, bottom = 8.dp),
                    )
                }
                itemsIndexed(state.queue, key = { index, item -> "${item.uri}-$index" }) { index, item ->
                    QueueItem(
                        track = item,
                        isCurrent = index == state.currentIndex,
                        onClick = { onPlayTrackAt(index) },
                    )
                }
            }
        }
    }

    if (showStopConfirmation) {
        AlertDialog(
            onDismissRequest = { showStopConfirmation = false },
            title = { Text(stringResource(R.string.player_stop)) },
            text = { Text(stringResource(R.string.player_stop_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStopConfirmation = false
                        onStop()
                    },
                ) {
                    Text(stringResource(R.string.player_stop))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopConfirmation = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun PlaybackModeControls(
    state: com.example.mimusica.domain.model.PlaybackState,
    onCycleRepeatMode: () -> Unit,
    onToggleShuffle: () -> Unit,
) {
    val repeatDescription = stringResource(R.string.player_repeat_toggle)
    val repeatStateDescription = stringResource(
        when (state.repeatMode) {
            PlaybackRepeatMode.OFF -> R.string.player_repeat_off
            PlaybackRepeatMode.ALL -> R.string.player_repeat_all
            PlaybackRepeatMode.ONE -> R.string.player_repeat_one
        },
    )
    val shuffleDescription = stringResource(
        if (state.shuffleEnabled) {
            R.string.player_shuffle_disable
        } else {
            R.string.player_shuffle_enable
        },
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onCycleRepeatMode,
            modifier = Modifier
                .size(48.dp)
                .semantics {
                    contentDescription = repeatDescription
                    stateDescription = repeatStateDescription
                },
        ) {
            Icon(
                imageVector = if (state.repeatMode == PlaybackRepeatMode.ONE) {
                    Icons.Filled.RepeatOne
                } else {
                    Icons.Filled.Repeat
                },
                contentDescription = null,
                tint = if (state.repeatMode == PlaybackRepeatMode.OFF) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        }
        IconButton(
            onClick = onToggleShuffle,
            modifier = Modifier
                .size(48.dp)
                .semantics {
                    contentDescription = shuffleDescription
                    stateDescription = shuffleDescription
                },
        ) {
            Icon(
                imageVector = Icons.Filled.Shuffle,
                contentDescription = null,
                tint = if (state.shuffleEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun PlayerArtwork(track: PlaybackTrack) {
    PlaybackArtwork(
        artworkPath = track.artworkPath,
        title = track.title,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    )
}

@Composable
private fun PlayerStatus(status: PlaybackStatus, isPlaying: Boolean) {
    val message = when (status) {
        PlaybackStatus.IDLE -> stringResource(R.string.player_status_paused)
        PlaybackStatus.BUFFERING -> stringResource(R.string.player_status_loading)
        PlaybackStatus.READY -> stringResource(
            if (isPlaying) R.string.player_status_playing else R.string.player_status_paused,
        )
        PlaybackStatus.ENDED -> stringResource(R.string.player_status_ended)
        PlaybackStatus.ERROR -> stringResource(R.string.player_status_error)
    }
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = if (status == PlaybackStatus.ERROR) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    )
}

@Composable
private fun PlaybackControls(
    state: com.example.mimusica.domain.model.PlaybackState,
    onPrevious: () -> Unit,
    onBackTen: () -> Unit,
    onToggle: () -> Unit,
    onForwardTen: () -> Unit,
    onNext: () -> Unit,
) {
    val previousDescription = stringResource(R.string.player_previous)
    val backDescription = stringResource(R.string.player_seek_back)
    val toggleDescription = stringResource(
        if (state.isPlaying) R.string.player_pause else R.string.player_play,
    )
    val forwardDescription = stringResource(R.string.player_seek_forward)
    val nextDescription = stringResource(R.string.player_next)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerIconButton(previousDescription, onPrevious, state.canSkipPrevious) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = null)
        }
        PlayerIconButton(backDescription, onBackTen, state.durationMs > 0L) {
            Icon(Icons.Filled.FastRewind, contentDescription = null)
        }
        PlayerIconButton(toggleDescription, onToggle, state.currentTrack != null) {
            Icon(
                if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
            )
        }
        PlayerIconButton(forwardDescription, onForwardTen, state.durationMs > 0L) {
            Icon(Icons.Filled.FastForward, contentDescription = null)
        }
        PlayerIconButton(nextDescription, onNext, state.canSkipNext) {
            Icon(Icons.Filled.SkipNext, contentDescription = null)
        }
    }
}

@Composable
private fun PlayerIconButton(
    description: String,
    onClick: () -> Unit,
    enabled: Boolean,
    icon: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = description },
    ) {
        icon()
    }
}

@Composable
private fun QueueItem(
    track: PlaybackTrack,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        color = if (isCurrent) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = track.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist ?: stringResource(R.string.library_unknown_artist),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val seconds = durationMs.coerceAtLeast(0L) / 1_000L
    return "%d:%02d".format(seconds / 60L, seconds % 60L)
}

@Preview(name = "Reproductor - sin canción", showBackground = true)
@Composable
private fun PlayerEmptyPreview() {
    MiMusicaTheme(darkTheme = false, dynamicColor = false) {
        PlayerScreenContent(
            state = PlaybackState(),
            onBack = {},
            onSeekTo = {},
            onPrevious = {},
            onBackTen = {},
            onToggle = {},
            onForwardTen = {},
            onNext = {},
            onSetVolume = {},
            onCycleRepeatMode = {},
            onToggleShuffle = {},
            onPlayTrackAt = {},
            onStop = {},
        )
    }
}

@Preview(name = "Reproductor - cola de ejemplo", showBackground = true)
@Composable
private fun PlayerQueuePreview() {
    MiMusicaTheme(darkTheme = false, dynamicColor = false) {
        PlayerScreenContent(
            state = PlaybackState(
                queue = listOf(
                    PlaybackTrack("preview://song-1", "Canción de ejemplo", "Artista de ejemplo", null),
                    PlaybackTrack("preview://song-2", "Otra canción", "Otro artista", null),
                ),
                currentIndex = 0,
                positionMs = 62_000L,
                durationMs = 180_000L,
                status = PlaybackStatus.READY,
                isPlaying = true,
                canSkipNext = true,
            ),
            onBack = {},
            onSeekTo = {},
            onPrevious = {},
            onBackTen = {},
            onToggle = {},
            onForwardTen = {},
            onNext = {},
            onSetVolume = {},
            onCycleRepeatMode = {},
            onToggleShuffle = {},
            onPlayTrackAt = {},
            onStop = {},
        )
    }
}

@Preview(name = "Reproductor - pausa y shuffle oscuro", showBackground = true)
@Composable
private fun PlayerPausedDarkPreview() {
    MiMusicaTheme(darkTheme = true, dynamicColor = false) {
        PlayerScreenContent(
            state = PlaybackState(
                queue = listOf(
                    PlaybackTrack("preview://song", "Canción de ejemplo", "Artista de ejemplo", null),
                ),
                currentIndex = 0,
                durationMs = 180_000L,
                status = PlaybackStatus.READY,
                repeatMode = PlaybackRepeatMode.ALL,
                shuffleEnabled = true,
            ),
            onBack = {},
            onSeekTo = {},
            onPrevious = {},
            onBackTen = {},
            onToggle = {},
            onForwardTen = {},
            onNext = {},
            onSetVolume = {},
            onCycleRepeatMode = {},
            onToggleShuffle = {},
            onPlayTrackAt = {},
            onStop = {},
        )
    }
}

@Preview(name = "Reproductor - error oscuro", showBackground = true)
@Composable
private fun PlayerErrorPreview() {
    MiMusicaTheme(darkTheme = true, dynamicColor = false) {
        PlayerScreenContent(
            state = PlaybackState(status = PlaybackStatus.ERROR),
            onBack = {},
            onSeekTo = {},
            onPrevious = {},
            onBackTen = {},
            onToggle = {},
            onForwardTen = {},
            onNext = {},
            onSetVolume = {},
            onCycleRepeatMode = {},
            onToggleShuffle = {},
            onPlayTrackAt = {},
            onStop = {},
        )
    }
}
