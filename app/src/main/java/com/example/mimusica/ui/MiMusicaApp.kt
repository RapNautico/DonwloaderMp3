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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import android.net.Uri
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
import com.example.mimusica.components.MiniPlayer
import com.example.mimusica.ui.screens.AlbumDetailScreen
import com.example.mimusica.ui.screens.AlbumsScreen
import com.example.mimusica.ui.screens.ArtistDetailScreen
import com.example.mimusica.ui.screens.ArtistsScreen
import com.example.mimusica.ui.screens.DownloadsScreen
import com.example.mimusica.ui.screens.HomeScreen
import com.example.mimusica.ui.screens.LibraryScreen
import com.example.mimusica.ui.screens.PlaylistsScreen
import com.example.mimusica.ui.screens.SettingsScreen
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
 * Shell de la app (DESIGN.md §2): NavigationHost + barra inferior + MiniPlayer visual.
 *
 * IMPORTANTE en FASE 3: solo existe la estructura base. El MiniPlayer es VISUAL y
 * los botones están deshabilitados (no simula reproducción real).
 */
@Composable
fun MiMusicaApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            androidx.compose.foundation.layout.Column {
                MiniPlayer()
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
                        icon = { Icon(item.icon, contentDescription = null) }, // la etiqueta describe semánticamente
                        label = { Text(stringResource(item.labelRes)) },
                    )
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
                        onOpenArtists = { navController.navigate("library/artists") },
                        onOpenAlbums = { navController.navigate("library/albums") },
                    )
                }
                composable("library/artists") {
                    ArtistsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenArtist = { name -> openGroup(navController, "library/artist", name) },
                    )
                }
                composable("library/albums") {
                    AlbumsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenAlbum = { name -> openGroup(navController, "library/album", name) },
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
                    )
                }
            }
            composable("playlists") { PlaylistsScreen() }
            composable("downloads") { DownloadsScreen() }
            composable("settings") { SettingsScreen() }
        }
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
