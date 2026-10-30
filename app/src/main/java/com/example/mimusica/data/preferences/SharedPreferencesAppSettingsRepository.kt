package com.example.mimusica.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.mimusica.domain.model.AppSettings
import com.example.mimusica.domain.model.AppThemeMode
import com.example.mimusica.domain.model.LibraryDisplayMode
import com.example.mimusica.domain.model.PlaybackRepeatMode
import com.example.mimusica.domain.model.SongSort
import com.example.mimusica.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharedPreferencesAppSettingsRepository(context: Context) : AppSettingsRepository {
    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val mutableSettings = MutableStateFlow(readSettings())

    override val settings: StateFlow<AppSettings> = mutableSettings.asStateFlow()

    override suspend fun update(settings: AppSettings) {
        preferences.edit()
            .putString(KEY_THEME_MODE, settings.themeMode.name)
            .putBoolean(KEY_DYNAMIC_COLOR, settings.dynamicColor)
            .putString(KEY_DEFAULT_SORT, settings.defaultSongSort.name)
            .putString(KEY_LIBRARY_DISPLAY, settings.libraryDisplayMode.name)
            .putBoolean(KEY_RESTORE_SESSION, settings.restoreLastSession)
            .putBoolean(KEY_AUTO_RESUME, settings.autoResumePlayback)
            .putBoolean(KEY_DEFAULT_SHUFFLE, settings.defaultShuffle)
            .putString(KEY_DEFAULT_REPEAT, settings.defaultRepeatMode.name)
            .putBoolean(KEY_SCAN_MUSIC, settings.scanMusicFolder)
            .apply()
        mutableSettings.value = settings
    }

    private fun readSettings() = AppSettings(
        themeMode = preferences.enumValue(KEY_THEME_MODE, AppThemeMode.SYSTEM),
        dynamicColor = preferences.getBoolean(KEY_DYNAMIC_COLOR, true),
        defaultSongSort = preferences.enumValue(KEY_DEFAULT_SORT, SongSort.TITLE_ASC),
        libraryDisplayMode = preferences.enumValue(KEY_LIBRARY_DISPLAY, LibraryDisplayMode.LIST),
        restoreLastSession = preferences.getBoolean(KEY_RESTORE_SESSION, true),
        autoResumePlayback = preferences.getBoolean(KEY_AUTO_RESUME, true),
        defaultShuffle = preferences.getBoolean(KEY_DEFAULT_SHUFFLE, false),
        defaultRepeatMode = preferences.enumValue(KEY_DEFAULT_REPEAT, PlaybackRepeatMode.OFF),
        scanMusicFolder = preferences.getBoolean(KEY_SCAN_MUSIC, false),
    )

    private inline fun <reified T : Enum<T>> SharedPreferences.enumValue(
        key: String,
        default: T,
    ): T = getString(key, null)
        ?.let { stored -> enumValues<T>().firstOrNull { it.name == stored } }
        ?: default

    private companion object {
        const val PREFERENCES_NAME = "app_settings"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_DEFAULT_SORT = "default_song_sort"
        const val KEY_LIBRARY_DISPLAY = "library_display_mode"
        const val KEY_RESTORE_SESSION = "restore_last_session"
        const val KEY_AUTO_RESUME = "auto_resume_playback"
        const val KEY_DEFAULT_SHUFFLE = "default_shuffle"
        const val KEY_DEFAULT_REPEAT = "default_repeat_mode"
        const val KEY_SCAN_MUSIC = "scan_music_folder"
    }
}
