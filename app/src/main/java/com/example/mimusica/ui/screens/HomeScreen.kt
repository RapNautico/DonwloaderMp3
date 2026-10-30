package com.example.mimusica.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mimusica.R
import com.example.mimusica.components.EmptyState
import com.example.mimusica.theme.MiMusicaTheme

/**
 * Home (DESIGN_SYSTEM.md §6.2).
 *
 * FASE 3: muestra el estado Empty REAL de una biblioteca sin contenido todavía.
 * No existen datos fabricados: no hay "canciones falsas" en esta pantalla.
 *
 * La acción "Añadir música" es un botón DESHABILITADO porque la importación
 * con SAF se implementa en FASE 5; no muestra un flujo falso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item {
                EmptyState(
                    icon = Icons.Filled.Add,
                    title = stringResource(R.string.home_empty_title),
                    description = stringResource(R.string.home_empty_description),
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center),
                )
            }
            item {
                // Botón visible pero deshabilitado: en FASE 5 habilita "Importar audio (SAF)"
                FilledTonalButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp),
                ) {
                    Text(stringResource(R.string.home_empty_cta))
                }
            }
        }
    }
}

@Preview(name = "Inicio - claro", showBackground = true)
@Composable
private fun HomeScreenLightPreview() {
    MiMusicaTheme(darkTheme = false, dynamicColor = false) {
        HomeScreen()
    }
}

@Preview(name = "Inicio - oscuro", showBackground = true)
@Composable
private fun HomeScreenDarkPreview() {
    MiMusicaTheme(darkTheme = true, dynamicColor = false) {
        HomeScreen()
    }
}
