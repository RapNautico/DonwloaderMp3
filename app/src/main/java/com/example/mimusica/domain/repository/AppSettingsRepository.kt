package com.example.mimusica.domain.repository

import com.example.mimusica.domain.model.AppSettings
import kotlinx.coroutines.flow.StateFlow

interface AppSettingsRepository {
    val settings: StateFlow<AppSettings>

    suspend fun update(settings: AppSettings)
}
