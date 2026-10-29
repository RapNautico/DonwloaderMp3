package com.example.mimusica.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navigation
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mimusica.R
import com.example.mimusica.MiMusicaApplication
import com.example.mimusica.components.MiniPlayer
import com.example.mimusica.presentation.player.PlayerViewModel
import com.example.mimusica.ui.screens.AlbumDetailScreen
import com.example.mimusica.ui.screens.AlbumsScreen
import com.example.mimusica.ui.screens.ArtistDetailScreen
import com.example.mimusica.ui.screens.ArtistsScreen
import com.example.mimusica.ui.screens.DownloadsScreen
import com.example.mimusica.ui.screens.HomeScreen
import com.example.mimusica.ui.screens.LibraryScreen
import com.example.mimusica.ui.screens.LibraryTab
import com.example.mimusica.ui.screens.PlayerScreen
import com.example.mimusica.ui.screens.PlaylistsScreen
import com.example.mimusica.ui.screens.SettingsScreen
import com.example.mimusica.ui.screens.SongDetailScreen
private data class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevelDestination("home", R.string.home, Icons.Filled.Home),
    TopLevelDestination("library", R.string.library, Icons.Filled.LibraryMusic),
    TopLevelDestination("playlists", R.string.playlists, Icons.AutoMirrored.Filled.QueueMusic),
    TopLevelDestination("downloads", R.string.downloads, Icons.Filled.Download),
    TopLevelDestination("settings", R.string.settings, Icons.Filled.Settings),
)

/**
 * Shell de la app: navegación, MiniPlayer persistente y reproductor compartido.
 */
@Composable
fun MiMusicaApp() {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as MiMusicaApplication
    val playerViewModel: PlayerViewModel = viewModel(factory = app.container.viewModelFactory)
    val playbackState by playerViewModel.state.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            if (currentDestination?.route != "player") {
                androidx.compose.foundation.layout.Column {
                    if (playbackState.currentTrack != null) {
                        MiniPlayer(
                            state = playbackState,
                            onTogglePlayPause = playerViewModel::togglePlayPause,
                            onNext = playerViewModel::skipToNext,
                            onExpand = { navController.navigate("player") },
                        )
                    }
                    NavigationBar {
                        topLevelDestinations.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == item.route
                            } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(item.icon, contentDescription = null) },
                                label = { Text(stringResource(item.labelRes)) },
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
        ) {
            // FASE 8.4 — rutas ANIDADAS de la biblioteca (PLAN 8.4): el grafo
            // lleva la ruta "library" para que la bottom bar SIGA resaltada en
            // Artistas/Álbumes/detalle (hierarchy), sin tocarla. El listado
            // principal pasa a "library/songs" y navegar a "library" abre el
            // start destination del grafo. Home/playlists/etc. sin cambios.
            composable("home") { HomeScreen() }
            navigation(startDestination = "library/songs", route = "library") {
                composable("library/songs") {
                    LibraryScreen(
                        onSelectTab = { tab -> navigateLibraryTab(navController, tab) },
                        onOpenSong = { uri -> openSongDetail(navController, uri) },
                        onPlaySongs = { songs, uri ->
                            playerViewModel.playSongs(songs, uri)
                            navController.navigate("player")
                        },
                    )
                }
                composable("library/artists") {
                    ArtistsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenArtist = { name -> openGroup(navController, "library/artist", name) },
                        onSelectTab = { tab -> navigateLibraryTab(navController, tab) },
                    )
                }
                composable("library/albums") {
                    AlbumsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenAlbum = { name -> openGroup(navController, "library/album", name) },
                        onSelectTab = { tab -> navigateLibraryTab(navController, tab) },
                    )
                }
                // nombre opcional en la ruta: ausente = bucket desconocido.
                composable(
                    route = "library/artist?name={name}",
                    arguments = listOf(
                        navArgument("name") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
                ) { backStackEntry ->
                    ArtistDetailScreen(
                        name = backStackEntry.arguments?.getString("name"),
                        onBack = { navController.popBackStack() },
                        onSongOpen = { uri -> openSongDetail(navController, uri) },
                    )
                }
                composable(
                    route = "library/album?name={name}",
                    arguments = listOf(
                        navArgument("name") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
                ) { backStackEntry ->
                    AlbumDetailScreen(
                        name = backStackEntry.arguments?.getString("name"),
                        onBack = { navController.popBackStack() },
                        onSongOpen = { uri -> openSongDetail(navController, uri) },
                    )
                }
                // FASE 8.5 — detalle de canción (bottom sheet M3). La uri REAL
                // (content://...) va codeada; el parámetro tipo query + nullable
                // evita romper con / : % en la URI y, si faltara, la pantalla
                // defiende con el estado NotFound (sin crash).
                composable(
                    route = "library/song?uri={uri}",
                    arguments = listOf(
                        navArgument("uri") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
                ) { backStackEntry ->
                    SongDetailScreen(
                        uri = backStackEntry.arguments?.getString("uri"),
                        onDismiss = { navController.popBackStack() },
                    )
                }
            }
            composable("player") {
                PlayerScreen(
                    viewModel = playerViewModel,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("playlists") { PlaylistsScreen() }
            composable("downloads") { DownloadsScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}

private fun navigateLibraryTab(navController: NavHostController, tab: LibraryTab) {
    navController.navigate(tab.route) {
        popUpTo("library/songs") { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Abre el detalle de un grupo (FASE 8.4). El nombre se CODEA como URI
 * (artistas/álbumes con espacios, "/" o "&" son nombres válidos y llegan
 * intactos a la pantalla). `null` NAVEGA sin parámetro: es el bucket
 * desconocido, destino normal, no una anomalía.
 */
private fun openGroup(navController: NavHostController, baseRoute: String, name: String?) {
    navController.navigate(
        if (name == null) baseRoute else "$baseRoute?name=${Uri.encode(name)}",
    )
}

/**
 * Abre el DETALLE de una canción (FASE 8.5). La uri REAL del archivo
 * (content://...) va codeada; una uri nula navega sin parámetro y la
 * pantalla la defiende con estado NotFound (sin crash).
 */
private fun openSongDetail(navController: NavHostController, uri: String?) {
    navController.navigate(
        if (uri.isNullOrBlank()) "library/song" else "library/song?uri=${Uri.encode(uri)}",
    )
}
