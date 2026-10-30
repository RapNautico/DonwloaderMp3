package com.example.mimusica.ui.screens

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mimusica.MiMusicaApplication
import com.example.mimusica.R
import com.example.mimusica.domain.model.AppSettings
import com.example.mimusica.domain.model.AppThemeMode
import com.example.mimusica.domain.model.LibraryDisplayMode
import com.example.mimusica.domain.model.MusicScanStatus
import com.example.mimusica.domain.model.PlaybackRepeatMode
import com.example.mimusica.domain.model.SongSort
import com.example.mimusica.presentation.settings.SettingsViewModel
import com.example.mimusica.theme.MiMusicaTheme

private enum class SettingsChoice {
    THEME,
    SORT,
    DISPLAY,
    REPEAT,
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen() {
    val context = LocalContext.current
    val container = (context.applicationContext as MiMusicaApplication).container
    val viewModel: SettingsViewModel = viewModel(factory = container.viewModelFactory)
    val settings by viewModel.settings.collectAsState()
    val scanStatus by viewModel.scanStatus.collectAsState()
    val storageBytes by viewModel.storageBytes.collectAsState()
    val storageError by viewModel.storageError.collectAsState()
    var enableAutoScanAfterPermission by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_AUDIO
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted && enableAutoScanAfterPermission) {
            viewModel.setAutomaticScanEnabled(true)
        } else {
            viewModel.scanMusicFolder()
        }
        enableAutoScanAfterPermission = false
    }

    LaunchedEffect(Unit) { viewModel.refreshStorageUsage() }

    val versionName = runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
            ?: context.getString(R.string.settings_version_unknown)
    }.getOrElse { context.getString(R.string.settings_version_unknown) }

    SettingsScreenContent(
        settings = settings,
        scanStatus = scanStatus,
        storageBytes = storageBytes,
        storageError = storageError,
        dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        storageLocation = context.filesDir.absolutePath,
        versionName = versionName,
        onUpdate = viewModel::update,
        onSetAutomaticScanEnabled = { enabled ->
            if (!enabled) {
                viewModel.setAutomaticScanEnabled(false)
            } else if (
                context.checkSelfPermission(audioPermission) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                viewModel.setAutomaticScanEnabled(true)
            } else {
                enableAutoScanAfterPermission = true
                permissionLauncher.launch(audioPermission)
            }
        },
        onScan = {
            if (
                context.checkSelfPermission(audioPermission) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                viewModel.scanMusicFolder()
            } else {
                enableAutoScanAfterPermission = false
                permissionLauncher.launch(audioPermission)
            }
        },
        onOpenLicense = uriHandler::openUri,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SettingsScreenContent(
    settings: AppSettings,
    scanStatus: MusicScanStatus,
    storageBytes: Long?,
    storageError: Boolean,
    dynamicColorSupported: Boolean,
    storageLocation: String,
    versionName: String,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onSetAutomaticScanEnabled: (Boolean) -> Unit,
    onScan: () -> Unit,
    onOpenLicense: (String) -> Unit,
) {
    var openChoice by remember { mutableStateOf<SettingsChoice?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.settings)) }) }) { insets ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets),
        ) {
            item {
                SettingsSectionTitle(stringResource(R.string.settings_appearance))
                SettingsChoiceRow(
                    title = stringResource(R.string.settings_theme),
                    value = themeLabel(settings.themeMode),
                    onClick = { openChoice = SettingsChoice.THEME },
                )
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_dynamic_color),
                    description = stringResource(
                        if (dynamicColorSupported) R.string.settings_dynamic_color_description
                        else R.string.settings_dynamic_color_unavailable,
                    ),
                    checked = settings.dynamicColor && dynamicColorSupported,
                    enabled = dynamicColorSupported,
                    onCheckedChange = { enabled ->
                        onUpdate { it.copy(dynamicColor = enabled) }
                    },
                )
            }
            item { HorizontalDivider() }
            item {
                SettingsSectionTitle(stringResource(R.string.settings_library))
                SettingsChoiceRow(
                    title = stringResource(R.string.settings_default_sort),
                    value = sortLabel(settings.defaultSongSort),
                    onClick = { openChoice = SettingsChoice.SORT },
                )
                SettingsChoiceRow(
                    title = stringResource(R.string.settings_library_view),
                    value = displayLabel(settings.libraryDisplayMode),
                    onClick = { openChoice = SettingsChoice.DISPLAY },
                )
                SettingsInfoRow(
                    title = stringResource(R.string.settings_delete_behavior),
                    description = stringResource(R.string.settings_delete_behavior_description),
                )
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_scan_automatic),
                    description = stringResource(R.string.settings_scan_automatic_description),
                    checked = settings.scanMusicFolder,
                    onCheckedChange = onSetAutomaticScanEnabled,
                )
                Button(
                    onClick = onScan,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    enabled = !scanStatus.isScanning,
                ) {
                    if (scanStatus.isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp).padding(end = 4.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    Text(stringResource(R.string.settings_scan_now))
                }
                when {
                    scanStatus.error != null -> SettingsInfoRow(
                        title = stringResource(R.string.settings_scan_result),
                        description = scanStatus.error.orEmpty(),
                    )
                    scanStatus.hasScanned -> SettingsInfoRow(
                        title = stringResource(R.string.settings_scan_result),
                        description = stringResource(
                            R.string.settings_scan_summary,
                            scanStatus.indexedFiles,
                            scanStatus.addedSongs,
                            scanStatus.duplicates,
                            scanStatus.failedFiles,
                        ),
                    )
                }
            }
            item { HorizontalDivider() }
            item {
                SettingsSectionTitle(stringResource(R.string.settings_playback))
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_restore_session),
                    description = stringResource(R.string.settings_restore_session_description),
                    checked = settings.restoreLastSession,
                    onCheckedChange = { enabled -> onUpdate { it.copy(restoreLastSession = enabled) } },
                )
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_auto_resume),
                    description = stringResource(R.string.settings_auto_resume_description),
                    checked = settings.autoResumePlayback,
                    enabled = settings.restoreLastSession,
                    onCheckedChange = { enabled -> onUpdate { it.copy(autoResumePlayback = enabled) } },
                )
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_default_shuffle),
                    description = stringResource(R.string.settings_new_queue_only),
                    checked = settings.defaultShuffle,
                    onCheckedChange = { enabled -> onUpdate { it.copy(defaultShuffle = enabled) } },
                )
                SettingsChoiceRow(
                    title = stringResource(R.string.settings_default_repeat),
                    value = repeatLabel(settings.defaultRepeatMode),
                    onClick = { openChoice = SettingsChoice.REPEAT },
                )
            }
            item { HorizontalDivider() }
            item {
                SettingsSectionTitle(stringResource(R.string.settings_audio))
                SettingsInfoRow(
                    title = stringResource(R.string.settings_audio_playback),
                    description = stringResource(R.string.settings_audio_playback_description),
                )
            }
            item { HorizontalDivider() }
            item {
                SettingsSectionTitle(stringResource(R.string.settings_storage))
                SettingsInfoRow(
                    title = stringResource(R.string.settings_storage_location),
                    description = storageLocation,
                )
                SettingsInfoRow(
                    title = stringResource(R.string.settings_storage_audio),
                    description = stringResource(R.string.settings_storage_audio_description),
                )
                SettingsInfoRow(
                    title = stringResource(R.string.settings_storage_cache),
                    description = stringResource(
                        R.string.settings_storage_cache_description,
                        when {
                            storageError -> stringResource(R.string.settings_storage_usage_unavailable)
                            storageBytes != null -> storageBytes.toString()
                            else -> stringResource(R.string.settings_storage_calculating)
                        },
                    ),
                )
            }
            item { HorizontalDivider() }
            item {
                SettingsSectionTitle(stringResource(R.string.settings_about))
                SettingsInfoRow(
                    title = stringResource(R.string.app_name),
                    description = stringResource(
                        R.string.settings_version,
                        versionName,
                    ),
                )
                SettingsInfoRow(
                    title = stringResource(R.string.settings_about_description_title),
                    description = stringResource(R.string.settings_about_description),
                )
                SettingsInfoRow(
                    title = stringResource(R.string.settings_licenses),
                    description = stringResource(R.string.settings_licenses_description),
                )
                Text(
                    text = stringResource(R.string.settings_apache_license_link),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { onOpenLicense("https://www.apache.org/licenses/LICENSE-2.0") }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
                Text(
                    text = stringResource(R.string.settings_sqlite_license_link),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { onOpenLicense("https://sqlite.org/copyright.html") }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }

    openChoice?.let { choice ->
        when (choice) {
            SettingsChoice.THEME -> SettingsOptionsDialog(
                title = stringResource(R.string.settings_theme),
                selected = settings.themeMode,
                options = AppThemeMode.entries,
                label = ::themeLabel,
                onSelect = { value -> onUpdate { it.copy(themeMode = value) } },
                onDismiss = { openChoice = null },
            )
            SettingsChoice.SORT -> SettingsOptionsDialog(
                title = stringResource(R.string.settings_default_sort),
                selected = settings.defaultSongSort,
                options = SongSort.entries,
                label = ::sortLabel,
                onSelect = { value -> onUpdate { it.copy(defaultSongSort = value) } },
                onDismiss = { openChoice = null },
            )
            SettingsChoice.DISPLAY -> SettingsOptionsDialog(
                title = stringResource(R.string.settings_library_view),
                selected = settings.libraryDisplayMode,
                options = LibraryDisplayMode.entries,
                label = ::displayLabel,
                onSelect = { value -> onUpdate { it.copy(libraryDisplayMode = value) } },
                onDismiss = { openChoice = null },
            )
            SettingsChoice.REPEAT -> SettingsOptionsDialog(
                title = stringResource(R.string.settings_default_repeat),
                selected = settings.defaultRepeatMode,
                options = PlaybackRepeatMode.entries,
                label = ::repeatLabel,
                onSelect = { value -> onUpdate { it.copy(defaultRepeatMode = value) } },
                onDismiss = { openChoice = null },
            )
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsChoiceRow(title: String, value: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsInfoRow(title: String, description: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun <T> SettingsOptionsDialog(
    title: String,
    selected: T,
    options: List<T>,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(option)
                                onDismiss()
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = option == selected,
                            onClick = {
                                onSelect(option)
                                onDismiss()
                            },
                        )
                        Text(label(option), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) } },
    )
}

@Composable
private fun themeLabel(value: AppThemeMode): String = stringResource(
    when (value) {
        AppThemeMode.SYSTEM -> R.string.settings_theme_system
        AppThemeMode.LIGHT -> R.string.settings_theme_light
        AppThemeMode.DARK -> R.string.settings_theme_dark
    },
)

@Composable
private fun sortLabel(value: SongSort): String = stringResource(
    when (value) {
        SongSort.TITLE_ASC -> R.string.library_sort_title_asc
        SongSort.TITLE_DESC -> R.string.library_sort_title_desc
        SongSort.ARTIST_ASC -> R.string.library_sort_artist_asc
        SongSort.ALBUM_ASC -> R.string.library_sort_album_asc
        SongSort.DATE_ADDED_DESC -> R.string.library_sort_recent_first
        SongSort.DATE_ADDED_ASC -> R.string.library_sort_oldest_first
        SongSort.DURATION_DESC -> R.string.library_sort_duration_desc
    },
)

@Composable
private fun displayLabel(value: LibraryDisplayMode): String = stringResource(
    if (value == LibraryDisplayMode.LIST) R.string.settings_view_list else R.string.settings_view_grid,
)

@Composable
private fun repeatLabel(value: PlaybackRepeatMode): String = stringResource(
    when (value) {
        PlaybackRepeatMode.OFF -> R.string.player_repeat_off
        PlaybackRepeatMode.ALL -> R.string.player_repeat_all
        PlaybackRepeatMode.ONE -> R.string.player_repeat_one
    },
)

@Preview(name = "Ajustes - modo claro", showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    MiMusicaTheme(darkTheme = false, dynamicColor = false) {
        SettingsScreenContent(
            settings = AppSettings(
                themeMode = AppThemeMode.LIGHT,
                dynamicColor = false,
                scanMusicFolder = true,
            ),
            scanStatus = MusicScanStatus(
                hasScanned = true,
                indexedFiles = 12,
                addedSongs = 3,
                duplicates = 9,
            ),
            storageBytes = 1_024_000L,
            storageError = false,
            dynamicColorSupported = true,
            storageLocation = "/data/user/0/com.example.mimusica/files",
            versionName = "0.1.0 (ejemplo)",
            onUpdate = {},
            onSetAutomaticScanEnabled = {},
            onScan = {},
            onOpenLicense = {},
        )
    }
}

@Preview(name = "Ajustes - modo oscuro", showBackground = true)
@Composable
private fun SettingsScreenDarkPreview() {
    MiMusicaTheme(darkTheme = true, dynamicColor = false) {
        SettingsScreenContent(
            settings = AppSettings(
                themeMode = AppThemeMode.DARK,
                dynamicColor = false,
                restoreLastSession = false,
                autoResumePlayback = false,
            ),
            scanStatus = MusicScanStatus(isScanning = true),
            storageBytes = null,
            storageError = true,
            dynamicColorSupported = false,
            storageLocation = "/data/user/0/com.example.mimusica/files",
            versionName = "0.1.0 (ejemplo)",
            onUpdate = {},
            onSetAutomaticScanEnabled = {},
            onScan = {},
            onOpenLicense = {},
        )
    }
}
