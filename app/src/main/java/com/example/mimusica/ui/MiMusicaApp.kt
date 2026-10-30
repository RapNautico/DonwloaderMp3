package com.example.mimusica.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mimusica.R
import com.example.mimusica.components.MiniPlayer
import com.example.mimusica.ui.screens.DownloadsScreen
import com.example.mimusica.ui.screens.HomeScreen
import com.example.mimusica.ui.screens.LibraryScreen
import com.example.mimusica.ui.screens.PlaylistsScreen
import com.example.mimusica.ui.screens.SettingsScreen
import com.example.mimusica.theme.MiMusicaTheme

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

    MiMusicaAppShell(
        selectedRoute = selectedTopLevelRoute(currentDestination),
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
            composable("library") { LibraryScreen() }
            composable("playlists") { PlaylistsScreen() }
            composable("downloads") { DownloadsScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}

@Composable
private fun MiMusicaAppShell(
    selectedRoute: String?,
    onDestinationSelected: (String) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        bottomBar = {
            Column {
                MiniPlayer()
                TopLevelNavigationBar(
                    selectedRoute = selectedRoute,
                    onDestinationSelected = onDestinationSelected,
                )
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
            onDestinationSelected = {},
        ) { contentModifier ->
            Box(contentModifier) {
                HomeScreen()
            }
        }
    }
}

@Preview(name = "Aplicación - oscuro", showBackground = true)
@Composable
private fun MiMusicaAppDarkPreview() {
    MiMusicaTheme(darkTheme = true, dynamicColor = false) {
        MiMusicaAppShell(
            selectedRoute = "home",
            onDestinationSelected = {},
        ) { contentModifier ->
            Box(contentModifier) {
                HomeScreen()
            }
        }
    }
}
