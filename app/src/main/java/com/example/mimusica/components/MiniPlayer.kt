package com.example.mimusica.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mimusica.R
import com.example.mimusica.theme.MiMusicaTheme

/**
 * MiniPlayer VISUAL (DESIGN_SYSTEM.md §6.1, §5).
 *
 * IMPORTANTE: no simula reproducción. Solo refleja la acción estética;
 * la lógica real de Media3 llega en FASE 10 y no existe todavía.
 * Botones deshabilitados (enabled = false) para no engañar al usuario.
 */
@Composable
fun MiniPlayer(
    modifier: Modifier = Modifier,
    onExpand: () -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
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
            // Portada placeholder: sin carátula real todavía
            Surface(
                modifier = Modifier.size(44.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null, // decorativo
                    modifier = Modifier.padding(8.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = true, onClick = onExpand)
                    .semantics {
                        contentDescription = "Reproducción no activa"
                    },
            ) {
                Text(
                    text = stringResource(R.string.mini_player_no_track),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.mini_player_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // Botones VISUALES: inactivos hasta que exista reproductor real (FASE 10)
            IconButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.semantics {
                    contentDescription = "Pausar"
                },
            ) {
                Icon(Icons.Filled.Pause, contentDescription = null)
            }
            IconButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.semantics {
                    contentDescription = "Siguiente"
                },
            ) {
                Icon(Icons.Filled.SkipNext, contentDescription = null)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MiniPlayerPreview() {
    MiMusicaTheme(dynamicColor = false) {
        MiniPlayer()
    }
}

@Preview(name = "MiniPlayer - oscuro", showBackground = true)
@Composable
private fun MiniPlayerDarkPreview() {
    MiMusicaTheme(darkTheme = true, dynamicColor = false) {
        MiniPlayer()
    }
}
