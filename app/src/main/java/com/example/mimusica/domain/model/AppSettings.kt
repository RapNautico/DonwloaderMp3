package com.example.mimusica.domain.model

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class LibraryDisplayMode {
    LIST,
    GRID,
}

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val defaultSongSort: SongSort = SongSort.TITLE_ASC,
    val libraryDisplayMode: LibraryDisplayMode = LibraryDisplayMode.LIST,
    val restoreLastSession: Boolean = true,
    val autoResumePlayback: Boolean = true,
    val defaultShuffle: Boolean = false,
    val defaultRepeatMode: PlaybackRepeatMode = PlaybackRepeatMode.OFF,
    val scanMusicFolder: Boolean = false,
)

fun AppSettings.shouldRestorePlaybackSession(hasSnapshot: Boolean): Boolean =
    restoreLastSession && hasSnapshot

fun AppSettings.shouldResumeSavedPlayback(savedPlayWhenReady: Boolean): Boolean =
    restoreLastSession && autoResumePlayback && savedPlayWhenReady
