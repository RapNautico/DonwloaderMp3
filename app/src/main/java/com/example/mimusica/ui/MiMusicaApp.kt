package com.example.mimusica.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import androidx.navigation.navArgument
import com.example.mimusica.MiMusicaApplication
import com.example.mimusica.R
import com.example.mimusica.components.MiniPlayer
import com.example.mimusica.domain.model.PlaybackState
import com.example.mimusica.domain.model.PlaybackTrack
import com.example.mimusica.presentation.player.PlayerViewModel
import com.example.mimusica.theme.MiMusicaTheme
import com.example.mimusica.ui.screens.AlbumDetailScreen
import com.example.mimusica.ui.screens.AlbumsScreen
import com.example.mimusica.ui.screens.ArtistDetailScreen
import com.example.mimusica.ui.screens.ArtistsScreen
import com.example.mimusica.ui.screens.DownloadsScreen
import com.example.mimusica.ui.screens.HomeScreen
import com.example.mimusica.ui.screens.LibraryScreen
import com.example.mimusica.ui.screens.LibraryTab
import com.example.mimusica.ui.screens.PlayerScreen
import com.example.mimusica.ui.screens.PlaylistDetailScreen
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

@Composable
fun MiMusicaApp() {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as MiMusicaApplication
    val playerViewModel: PlayerViewModel = viewModel(factory = app.container.viewModelFactory)
    val playbackState by playerViewModel.state.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    MiMusicaAppShell(
        selectedRoute = selectedTopLevelRoute(currentDestination),
        isPlayerDestination = currentDestination?.route == "player",
        playbackState = playbackState,
        onTogglePlayPause = playerViewModel::togglePlayPause,
        onNext = playerViewModel::skipToNext,
        onExpandPlayer = { navController.navigate("player") },
        onDestinationSelected = { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        },
    ) { contentModifier ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = contentModifier,
        ) {
            composable("home") { HomeScreen() }
            // The nested graph keeps Library selected while browsing its tabs and details.
            navigation(startDestination = "library/songs", route = "library") {
                composable("library/songs") {
                    LibraryScreen(
                        onSelectTab = { tab -> navigateLibraryTab(navController, tab) },
                        onOpenSong = { uri -> openSongDetail(navController, uri) },
                        onOpenPlaylists = { navController.navigate("playlists") },
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
                composable(
                    route = "library/artist?name={name}",
                    arguments = listOf(
                        navArgument("name") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
                ) { entry ->
                    ArtistDetailScreen(
                        name = entry.arguments?.getString("name"),
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
                ) { entry ->
                    AlbumDetailScreen(
                        name = entry.arguments?.getString("name"),
                        onBack = { navController.popBackStack() },
                        onSongOpen = { uri -> openSongDetail(navController, uri) },
                    )
                }
                composable(
                    route = "library/song?uri={uri}",
                    arguments = listOf(
                        navArgument("uri") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
                ) { entry ->
                    SongDetailScreen(
                        uri = entry.arguments?.getString("uri"),
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
            navigation(startDestination = "playlists/list", route = "playlists") {
                composable("playlists/list") {
                    PlaylistsScreen(
                        onOpenPlaylist = { id -> navController.navigate("playlists/detail/$id") },
                    )
                }
                composable(
                    route = "playlists/detail/{playlistId}",
                    arguments = listOf(navArgument("playlistId") { type = NavType.LongType }),
                ) { entry ->
                    val playlistId = entry.arguments?.getLong("playlistId") ?: return@composable
                    PlaylistDetailScreen(
                        playlistId = playlistId,
                        onBack = { navController.popBackStack() },
                        onPlaySongs = { songs, uri ->
                            playerViewModel.playSongs(songs, uri)
                            navController.navigate("player")
                        },
                    )
                }
            }
            composable("downloads") { DownloadsScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}

@Composable
private fun MiMusicaAppShell(
    selectedRoute: String?,
    isPlayerDestination: Boolean,
    playbackState: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onExpandPlayer: () -> Unit,
    onDestinationSelected: (String) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        bottomBar = {
            if (!isPlayerDestination) {
                Column {
                    if (playbackState.currentTrack != null) {
                        MiniPlayer(
                            state = playbackState,
                            onTogglePlayPause = onTogglePlayPause,
                            onNext = onNext,
                            onExpand = onExpandPlayer,
                        )
                    }
                    TopLevelNavigationBar(
                        selectedRoute = selectedRoute,
                        onDestinationSelected = onDestinationSelected,
                    )
                }
            }
        },
    ) { innerPadding ->
        content(Modifier.padding(innerPadding))
    }
}

private fun selectedTopLevelRoute(currentDestination: NavDestination?): String? =
    topLevelDestinations.firstOrNull { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }?.route

@Composable
private fun TopLevelNavigationBar(
    selectedRoute: String?,
    onDestinationSelected: (String) -> Unit,
) {
    NavigationBar {
        topLevelDestinations.forEach { item ->
            NavigationBarItem(
                selected = selectedRoute == item.route,
                onClick = { onDestinationSelected(item.route) },
                icon = { Icon(item.icon, contentDescription = null) },
                label = { Text(stringResource(item.labelRes)) },
            )
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

private fun openGroup(navController: NavHostController, baseRoute: String, name: String?) {
    navController.navigate(
        if (name == null) baseRoute else "$baseRoute?name=${Uri.encode(name)}",
    )
}

private fun openSongDetail(navController: NavHostController, uri: String?) {
    navController.navigate(
        if (uri.isNullOrBlank()) "library/song" else "library/song?uri=${Uri.encode(uri)}",
    )
}

private fun previewPlaybackState() = PlaybackState(
    queue = listOf(PlaybackTrack("preview://song", "Canción de ejemplo", "Artista de ejemplo", null)),
    currentIndex = 0,
)

@Preview(name = "Navegación - Inicio seleccionado", showBackground = true)
@Composable
private fun TopLevelNavigationBarPreview() {
    MiMusicaTheme(darkTheme = false, dynamicColor = false) {
        TopLevelNavigationBar(selectedRoute = "home", onDestinationSelected = {})
    }
}

@Preview(name = "Navegación - oscuro", showBackground = true)
@Composable
private fun TopLevelNavigationBarDarkPreview() {
    MiMusicaTheme(darkTheme = true, dynamicColor = false) {
        TopLevelNavigationBar(selectedRoute = "home", onDestinationSelected = {})
    }
}

@Preview(name = "Aplicación - claro", showBackground = true)
@Composable
private fun MiMusicaAppLightPreview() {
    MiMusicaTheme(darkTheme = false, dynamicColor = false) {
        MiMusicaAppShell(
            selectedRoute = "home",
            isPlayerDestination = false,
            playbackState = previewPlaybackState(),
            onTogglePlayPause = {},
            onNext = {},
            onExpandPlayer = {},
            onDestinationSelected = {},
        ) { contentModifier ->
            Box(contentModifier) { HomeScreen() }
        }
    }
}

@Preview(name = "Aplicación - oscuro", showBackground = true)
@Composable
private fun MiMusicaAppDarkPreview() {
    MiMusicaTheme(darkTheme = true, dynamicColor = false) {
        MiMusicaAppShell(
            selectedRoute = "home",
            isPlayerDestination = false,
            playbackState = previewPlaybackState(),
            onTogglePlayPause = {},
            onNext = {},
            onExpandPlayer = {},
            onDestinationSelected = {},
        ) { contentModifier ->
            Box(contentModifier) { HomeScreen() }
        }
    }
}
