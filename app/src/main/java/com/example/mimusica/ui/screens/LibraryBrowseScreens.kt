package com.example.mimusica.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mimusica.MiMusicaApplication
import com.example.mimusica.R
import com.example.mimusica.components.EmptyState
import com.example.mimusica.components.SongList
import com.example.mimusica.presentation.library.AlbumsViewModel
import com.example.mimusica.presentation.library.ArtistsViewModel
import com.example.mimusica.presentation.library.GroupDetailState
import com.example.mimusica.presentation.library.GroupDetailViewModel

/**
 * Vistas de exploración por ARTISTAS y ÁLBUMES (FASE 8.4, PLAN 8.4):
 * resúmenes REACTIVOS (Room GROUP BY, sin N+1) con nombre + nº de canciones,
 * y DETALLE navegable con las canciones del grupo. El bucket desconocido
 * (nombre `null` del DOMAIN) se rotula SOLO aquí, en presentación, y es un
 * destino NORMAL de navegación: nunca bloquea ni crashea (requisito 8.4).
 *
 * Las pantallas no conocen Room: los ViewModels exponen Domain flows.
 * Estados: Loading · vacía · error (banner errorContainer; se limpia con el
 * siguiente éxito, misma política de 8.3) · Success.
 *
 * La navegación (rutas anidadas dentro del grafo "library") vive en
 * MiMusicaApp; las Tabs definitivas Canciones/Artistas/Álbumes de la
 * biblioteca llegan en 8.8 (DESIGN_SYSTEM §6).
 */

// ---------- Listas de resúmenes ----------

/** Vista de ARTISTAS: fila por artista con su nº de canciones (SQL). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistsScreen(
    onBack: () -> Unit,
    onOpenArtist: (String?) -> Unit,
) {
    val container = (LocalContext.current.applicationContext as MiMusicaApplication).container
    val viewModel: ArtistsViewModel = viewModel(factory = container.viewModelFactory)
    val artists by viewModel.artists.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    GroupScreenScaffold(
        title = stringResource(R.string.library_artists_title),
        onBack = onBack,
    ) {
        GroupErrorBanner(error)
        when {
            isLoading -> Centered { CircularProgressIndicator() }
            artists.isEmpty() -> Centered {
                EmptyState(
                    icon = Icons.Filled.Person,
                    title = stringResource(R.string.library_empty_title),
                    description = stringResource(R.string.library_empty_description),
                )
            }
            else -> LazyColumn {
                items(artists, key = { it.name ?: KEY_UNKNOWN_BUCKET }) { artist ->
                    GroupRow(
                        icon = Icons.Filled.Person,
                        name = artist.name ?: stringResource(R.string.library_unknown_artist),
                        subtitle = songCountText(artist.songCount),
                        onClick = { onOpenArtist(artist.name) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

/** Vista de ÁLBUMES: fila por álbum con su nº de canciones (SQL). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    onBack: () -> Unit,
    onOpenAlbum: (String?) -> Unit,
) {
    val container = (LocalContext.current.applicationContext as MiMusicaApplication).container
    val viewModel: AlbumsViewModel = viewModel(factory = container.viewModelFactory)
    val albums by viewModel.albums.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    GroupScreenScaffold(
        title = stringResource(R.string.library_albums_title),
        onBack = onBack,
    ) {
        GroupErrorBanner(error)
        when {
            isLoading -> Centered { CircularProgressIndicator() }
            albums.isEmpty() -> Centered {
                EmptyState(
                    icon = Icons.Filled.Album,
                    title = stringResource(R.string.library_empty_title),
                    description = stringResource(R.string.library_empty_description),
                )
            }
            else -> LazyColumn {
                items(albums, key = { it.name ?: KEY_UNKNOWN_BUCKET }) { album ->
                    GroupRow(
                        icon = Icons.Filled.Album,
                        name = album.name ?: stringResource(R.string.library_unknown_album),
                        subtitle = songCountText(album.songCount),
                        onClick = { onOpenAlbum(album.name) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

// ---------- Detalle ----------

/** Detalle de UN ARTISTA: sus canciones con el orden SQL de 8.2 (getByArtist). */
@Composable
fun ArtistDetailScreen(
    name: String?,
    onBack: () -> Unit,
) {
    val container = (LocalContext.current.applicationContext as MiMusicaApplication).container
    val viewModel: GroupDetailViewModel = viewModel(
        factory = container.artistDetailViewModelFactory(name),
    )
    val estado by viewModel.state.collectAsState()

    GroupScreenScaffold(
        title = name ?: stringResource(R.string.library_unknown_artist),
        onBack = onBack,
    ) {
        GroupDetailContent(estado)
    }
}

/** Detalle de UN ÁLBUM: sus canciones con el orden SQL de 8.2 (getByAlbum). */
@Composable
fun AlbumDetailScreen(
    name: String?,
    onBack: () -> Unit,
) {
    val container = (LocalContext.current.applicationContext as MiMusicaApplication).container
    val viewModel: GroupDetailViewModel = viewModel(
        factory = container.albumDetailViewModelFactory(name),
    )
    val estado by viewModel.state.collectAsState()

    GroupScreenScaffold(
        title = name ?: stringResource(R.string.library_unknown_album),
        onBack = onBack,
    ) {
        GroupDetailContent(estado)
    }
}

@Composable
private fun GroupDetailContent(estado: GroupDetailState) {
    when (estado) {
        GroupDetailState.Loading -> Centered { CircularProgressIndicator() }
        is GroupDetailState.Content -> {
            if (estado.songs.isEmpty()) {
                Centered {
                    EmptyState(
                        icon = Icons.Filled.Person,
                        title = stringResource(R.string.library_group_empty_title),
                        description = stringResource(R.string.library_group_empty_description),
                    )
                }
            } else {
                SongList(estado.songs)
            }
        }
        is GroupDetailState.Error -> GroupErrorBanner(estado.message)
    }
}

// ---------- Componentes compartidos de estas pantallas ----------

/**
 * Scaffold común de artistas/álbumes y detalles: top bar transparente (como
 * Library) con flecha de retroceso; el título corta con ellipsis (filas de
 * artistas/álbumes largos, DESIGN_SYSTEM §2).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupScreenScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back),
                        )
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
            content()
        }
    }
}

/** Fila de resumen (artista/álbum): icono, NOMBRE y Nº DE CANCIONES reales. */
@Composable
private fun GroupRow(
    icon: ImageVector,
    name: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick) // el bucket desconocido TAMBIÉN navega
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Banner de error (errorContainer) del mismo estilo que el de 8.3. */
@Composable
private fun GroupErrorBanner(error: String?) {
    if (error != null) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.errorContainer,
        ) {
            Text(
                text = stringResource(R.string.library_browse_error),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** Contenido centrado verticalmente (estados Loading / vacío). */
@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}

/** Conteo localizado de canciones ("1 canción" / "N canciones"); dato REAL. */
@Composable
private fun songCountText(count: Int): String =
    pluralStringResource(R.plurals.library_song_count, count, count)

/**
 * Clave estable para la ÚNICA fila del bucket desconocido (name = null):
 * nunca colisiona con un nombre real porque contiene un carácter nulo.
 */
private const val KEY_UNKNOWN_BUCKET = "\u0000"
