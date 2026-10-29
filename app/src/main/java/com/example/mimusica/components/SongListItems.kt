package com.example.mimusica.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mimusica.R
import com.example.mimusica.domain.model.Song

/**
 * Fila de canción REUTILIZABLE (FASE 8.4, extracción de LibraryScreen).
 * La usan la lista de la biblioteca (8.3) y los detalles de artista/álbum.
 *
 * FASE 8.5: [onSongClick] abre el DETALLE de la canción (bottom sheet,
 * ruta `library/song?uri=...`). Cuando es null la fila NO es clicable
 * (comportamiento por defecto: la reproducción real llega en FASE 9).
 *
 * FASE 8.6: [menu] activa el MENÚ CONTEXTUAL de gestión (PLAN 8.6):
 * Reproducir (Fase 9, inoperante), Añadir a playlist (Fase 10, inoperante),
 * Información (abre el detalle 8.5) y Eliminar de biblioteca (requiere
 * ConfirmDialog ANTES de llegar aquí). Cuando es null NO hay menú
 * (detalles de artista/álbum: la gestión vive en la biblioteca por decidir
 * del plan 8.8; no se inventa comportamiento extra).
 */
@Composable
fun SongRow(
    song: Song,
    onSongClick: ((Song) -> Unit)? = null,
    menu: (@Composable () -> Unit)? = null,
) {
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
        // Menú contextual "⋮" (FASE 8.6); solo cuando la pantalla lo activa.
        menu?.invoke()
    }
}

/**
 * Acciones del MENÚ CONTEXTUAL de gestión (FASE 8.6, PLAN 8.6). Se reciben
 * YA decididas por la pantalla (biblioteca); aquí solo se renderizan.
 */
data class SongRowMenu(
    /** Abre el detalle de la canción (bottom sheet de 8.5). */
    val onInformation: () -> Unit,
    /** Pide SIMPLE el borrado (la ConfirmDialog vive en la pantalla). */
    val onDelete: () -> Unit,
)

/** Menú "⋮" con las 4 acciones del PLAN 8.6 (Reproducir/playlist inoperantes hoy). */
@Composable
fun SongMenuButton(actions: SongRowMenu) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(24.dp),
        ) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.song_menu_actions),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            // Callback POR REFUERZO para FASE 9 (PLAN 8.6): item presente e
            // inoperante; sin lógica de reproducción (fuera de FASE 8).
            DropdownMenuItem(
                text = { Text(stringResource(R.string.song_menu_play)) },
                onClick = { expanded = false },
                enabled = false, // reproducción real: FASE 9
            )
            // Callback POR REFUERZO para FASE 10 (PLAN 8.6):
            DropdownMenuItem(
                text = { Text(stringResource(R.string.song_menu_add_playlist)) },
                onClick = { expanded = false },
                enabled = false, // playlists: FASE 10
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.song_menu_information)) },
                onClick = {
                    expanded = false
                    actions.onInformation()
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.song_menu_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                onClick = {
                    expanded = false
                    actions.onDelete() // la pantalla exige ConfirmDialog
                },
            )
        }
    }
}

/** Listado vertical de canciones con separadores (comportamiento estable por `uri`). */
@Composable
fun SongList(
    songs: List<Song>,
    onSongClick: ((Song) -> Unit)? = null,
    menuFactory: ((Song) -> SongRowMenu)? = null,
) {
    LazyColumn {
        items(songs, key = { it.uri }) { song ->
            SongRow(
                song = song,
                onSongClick = onSongClick,
                menu = menuFactory?.let { factory -> { SongMenuButton(factory(song)) } },
            )
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
