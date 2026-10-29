package com.example.mimusica.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mimusica.MiMusicaApplication
import com.example.mimusica.R
import com.example.mimusica.components.ArtworkThumbnail
import com.example.mimusica.components.formatDuration
import com.example.mimusica.presentation.library.SongDetailState
import com.example.mimusica.presentation.library.SongDetailViewModel
import com.example.mimusica.presentation.library.formatSongBitrate
import com.example.mimusica.presentation.library.formatSongSampleRate
import com.example.mimusica.presentation.library.formatSongSize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * DETALLE de canción (FASE 8.5, PLAN 8.5): bottom sheet M3 con los
 * metadatos REALES del registro Room (título, artista, álbum, género,
 * duración, formato/mime, tamaño, bitrate, muestreo, canales, añadido).
 *
 * Regla de datos (AGENTS §8/D8.1): `null` del DOMAIN → "Desconocido" en la
 * UI; NADA de bitrate/calidad/codec inventados. La duración usa el helper
 * compartido (0 = desconocida → "--:--").
 *
 * Archivo YA NO disponible: el registro sigue en Room; su detección
 * bajo demanda y el badge visible son la FASE 8.7 (ahí no se toca aquí).
 * Una canción/URI inexistente en la biblioteca → estado [SongDetailState.NotFound]
 * visible sin crash; un fallo del repositorio → [SongDetailState.Error] sin crash.
 *
 * Se abre como DESTINO de navegación (`library/song?uri=...` en MiMusicaApp)
 * desde cualquier fila de canción existente (biblioteca y detalles de
 * artista/álbum), reutilizando [ArtworkThumbnail] y `formatDuration`.
 * Cerrar el sheet (swipe o back) hace popBackStack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailScreen(
    uri: String?,
    onDismiss: () -> Unit,
) {
    val container = (LocalContext.current.applicationContext as MiMusicaApplication).container
    val viewModel: SongDetailViewModel = viewModel(
        factory = container.songDetailViewModelFactory(uri),
    )
    val estado by viewModel.state.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        SongDetailContent(estado)
    }
}

@Composable
private fun SongDetailContent(estado: SongDetailState) {
    when (estado) {
        SongDetailState.Loading -> Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        SongDetailState.NotFound -> Text(
            text = stringResource(R.string.song_detail_not_found),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        )
        is SongDetailState.Error -> Text(
            text = stringResource(R.string.song_detail_error),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        )
        is SongDetailState.Content -> SongDetailRows(estado.song)
    }
}

@Composable
private fun SongDetailRows(song: com.example.mimusica.domain.model.Song) {
    Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        // Cabecera: portada (copia local o placeholder del design system en 64 dp).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ArtworkThumbnail(path = song.artworkPath, size = 64.dp)
            Column {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song.artist ?: stringResource(R.string.song_detail_unknown),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 12.dp))

        DetailRow(stringResource(R.string.song_detail_album), song.album)
        DetailRow(stringResource(R.string.song_detail_genre), song.genre)
        DetailRow(
            stringResource(R.string.song_detail_duration),
            formatDuration(song.durationMs),
        )
        DetailRow(
            stringResource(R.string.song_detail_format),
            song.mimeType,
        )
        DetailRow(
            stringResource(R.string.song_detail_size),
            formatSongSize(song.sizeBytes),
        )
        DetailRow(
            stringResource(R.string.song_detail_bitrate),
            formatSongBitrate(song.bitrateBps),
        )
        DetailRow(
            stringResource(R.string.song_detail_sample_rate),
            formatSongSampleRate(song.sampleRateHz),
        )
        DetailRow(
            stringResource(R.string.song_detail_channels),
            song.channels?.toString(),
        )
        DetailRow(
            stringResource(R.string.song_detail_added),
            formatDateAdded(song.dateAddedMs),
        )
    }
}

/** Par etiqueta/valor; valor null del DOMAIN → "Desconocido" (único punto). */
@Composable
private fun DetailRow(label: String, value: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value ?: stringResource(R.string.song_detail_unknown),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Fecha REAL de incorporación (epoch ms del registro Room 8.1). */
private fun formatDateAdded(dateAddedMs: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(dateAddedMs))
