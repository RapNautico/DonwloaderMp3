package com.example.mimusica.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mimusica.R
import com.example.mimusica.domain.model.Song

/**
 * Fila de canción REUTILIZABLE (FASE 8.4, extraction de LibraryScreen).
 * La usan la lista de la biblioteca (8.3) y los detalles de artista/álbum.
 *
 * FASE 8.5: [onSongClick] abre el DETALLE de la canción (bottom sheet,
 * ruta `library/song?uri=...`). Cuando es null la fila NO es clicable
 * (comportamiento por defecto: la reproducción real llega en FASE 9).
 */
@Composable
fun SongRow(song: Song, onSongClick: ((Song) -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = onSongClick != null,
                onClick = { onSongClick?.invoke(song) },
            )
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

/** Listado vertical de canciones con separadores (comportamiento estable por `uri`). */
@Composable
fun SongList(songs: List<Song>, onSongClick: ((Song) -> Unit)? = null) {
    LazyColumn {
        items(songs, key = { it.uri }) { song ->
            SongRow(song, onSongClick)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/**
 * Miniatura de portada (FASE 5): placeholder SIEMPRE mientras no exista
 * decodificación de carátulas; la copia local (artworkPath) está lista para
 * FASE 9/10. FASE 8.5: parámetro opcional de tamaño para el detalle
 * (64 dp) mientras las filas siguen usando 40 dp.
 */
@Composable
fun ArtworkThumbnail(path: String?, size: Dp = 40.dp) {
    Surface(
        modifier = Modifier.size(size),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Icon(
            Icons.Filled.MusicNote,
            contentDescription = null,
            modifier = Modifier.padding(size / 4),
        )
    }
}

/**
 * Duración legible a partir del valor REAL en milisegundos
 * (0 = desconocida, se muestra "--:--"; jamás se inventa una duración).
 */
internal fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "--:--"
    val total = durationMs / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
