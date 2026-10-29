package com.example.mimusica.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mimusica.R
import com.example.mimusica.domain.model.PlaybackState
import com.example.mimusica.domain.model.PlaybackTrack
import com.example.mimusica.theme.MiMusicaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

@Composable
fun MiniPlayer(
    state: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = state.currentTrack ?: return
    val playbackDescription = stringResource(
        if (state.isPlaying) R.string.player_pause else R.string.player_play,
    )
    val nextDescription = stringResource(R.string.player_next)
    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = onExpand,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 6.dp,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 64.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlaybackArtwork(
                artworkPath = track.artworkPath,
                title = track.title,
                modifier = Modifier.size(44.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = track.title },
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
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
            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = playbackDescription },
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                )
            }
            IconButton(
                onClick = onNext,
                enabled = state.canSkipNext,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = nextDescription },
            ) {
                Icon(Icons.Filled.SkipNext, contentDescription = null)
            }
        }
    }
}

@Composable
fun PlaybackArtwork(
    artworkPath: String?,
    title: String,
    modifier: Modifier = Modifier,
) {
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null,
        key1 = artworkPath,
    ) {
        value = withContext(Dispatchers.IO) { decodeArtwork(artworkPath) }
    }
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(12.dp),
    ) {
        if (bitmap == null) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                modifier = Modifier.padding(8.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        } else {
            Image(
                bitmap = bitmap!!,
                contentDescription = stringResource(R.string.song_artwork_description, title),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun decodeArtwork(path: String?): androidx.compose.ui.graphics.ImageBitmap? {
    if (path.isNullOrBlank()) return null
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val sampleSize = maxOf(1, maxOf(bounds.outWidth, bounds.outHeight) / 512)
        BitmapFactory.decodeFile(
            path,
            BitmapFactory.Options().apply { inSampleSize = sampleSize },
        )?.asImageBitmap()
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun MiniPlayerPreview() {
    MiMusicaTheme {
        MiniPlayer(
            state = PlaybackState(
                queue = listOf(PlaybackTrack("content://demo", "Canción", "Artista", null)),
                currentIndex = 0,
            ),
            onTogglePlayPause = {},
            onNext = {},
            onExpand = {},
        )
    }
}
