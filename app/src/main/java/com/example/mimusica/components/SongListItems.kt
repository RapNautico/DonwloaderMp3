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
import com.example.mimusica.domain.model.SongFileAvailability

/**
 * Fila de canción REUTILIZABLE (FASE 8.4, extracción de LibraryScreen).
 * La usan la lista de la biblioteca (8.3) y los detalles de artista/álbum.
 *
 * FASE 8.5: [onSongClick] abre el DETALLE de la canción (bottom sheet,
 * ruta `library/song?uri=...`). Cuando es null la fila NO es clicable.
 *
 * FASE 8.6/8.7: [menu] activa el menú contextual de gestión y comprobación:
 * Reproducir (inicia la cola de la lista visible), Añadir a playlist
 * (Fase 10, inoperante), comprobar archivo (bajo demanda), Información y
 * Eliminar de biblioteca (requiere ConfirmDialog ANTES de llegar aquí).
 * Cuando es null NO hay menú
 * (detalles de artista/álbum: la gestión vive en la biblioteca por decidir
 * del plan 8.8; no se inventa comportamiento extra).
 */
@Composable
fun SongRow(
    song: Song,
    onSongClick: ((Song) -> Unit)? = null,
    menu: (@Composable () -> Unit)? = null,
    fileAvailability: SongFileAvailability? = null,
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
        ArtworkThumbnail(song.artworkPath, title = song.title)
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(
                    song.artist ?: stringResource(R.string.library_unknown_artist),
                    song.album,
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            fileAvailability?.let { availability ->
                Text(
                    text = fileAvailabilityLabel(availability),
                    style = MaterialTheme.typography.bodySmall,
                    color = when (availability) {
                        SongFileAvailability.CHECKING -> MaterialTheme.colorScheme.onSurfaceVariant
                        SongFileAvailability.AVAILABLE -> MaterialTheme.colorScheme.primary
                        SongFileAvailability.MISSING,
                        SongFileAvailability.INACCESSIBLE,
                        SongFileAvailability.INVALID -> MaterialTheme.colorScheme.error
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
 * Acciones del menú de gestión (FASES 8.6/8.7). Se reciben desde la pantalla
 * de biblioteca; aquí solo se renderizan.
 */
data class SongRowMenu(
    /** Reproduce desde la canción seleccionada y conserva la cola visible. */
    val onPlay: () -> Unit,
    /** Abre la selección de playlist para la canción actual. */
    val onAddToPlaylist: () -> Unit,
    /** Abre el detalle de la canción (bottom sheet de 8.5). */
    val onInformation: () -> Unit,
    /** Pide SIMPLE el borrado (la ConfirmDialog vive en la pantalla). */
    val onDelete: () -> Unit,
    /** Solicita la comprobación bajo demanda del archivo asociado. */
    val onCheckFile: () -> Unit,
    val isCheckingFile: Boolean = false,
)

/** Menú "⋮" de la biblioteca. */
@Composable
fun SongMenuButton(actions: SongRowMenu) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.song_menu_actions),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.song_menu_play)) },
                onClick = {
                    expanded = false
                    actions.onPlay()
                },
            )
            // Callback POR REFUERZO para FASE 10 (PLAN 8.6):
            DropdownMenuItem(
                text = { Text(stringResource(R.string.song_menu_add_playlist)) },
                onClick = {
                    expanded = false
                    actions.onAddToPlaylist()
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (actions.isCheckingFile) {
                                R.string.song_menu_checking_file
                            } else {
                                R.string.song_menu_check_file
                            },
                        ),
                    )
                },
                onClick = {
                    expanded = false
                    actions.onCheckFile()
                },
                enabled = !actions.isCheckingFile,
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
    fileAvailability: Map<String, SongFileAvailability> = emptyMap(),
) {
    LazyColumn {
        items(songs, key = { it.uri }) { song ->
            SongRow(
                song = song,
                onSongClick = onSongClick,
                menu = menuFactory?.let { factory -> { SongMenuButton(factory(song)) } },
                fileAvailability = fileAvailability[song.uri],
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

    }
}

@Composable
private fun fileAvailabilityLabel(availability: SongFileAvailability): String = when (availability) {
    SongFileAvailability.CHECKING -> stringResource(R.string.song_file_checking)
    SongFileAvailability.AVAILABLE -> stringResource(R.string.song_file_available)
    SongFileAvailability.MISSING -> stringResource(R.string.song_file_missing)
    SongFileAvailability.INACCESSIBLE -> stringResource(R.string.song_file_inaccessible)
    SongFileAvailability.INVALID -> stringResource(R.string.song_file_invalid)
}

/**
 * Marcador visual para portadas locales hasta implementar su decodificación.
 * El tamaño predeterminado sigue el objetivo de 48 dp del sistema de diseño.
 */
@Composable
fun ArtworkThumbnail(path: String?, size: Dp = 48.dp, title: String? = null) {
    Surface(
        modifier = Modifier.size(size),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Icon(
            Icons.Filled.MusicNote,
            contentDescription = title?.let {
                stringResource(R.string.song_artwork_description, it)
            },
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
