package com.example.mimusica.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSettingsPolicyTest {
    @Test
    fun `restauracion requiere preferencia habilitada y snapshot disponible`() {
        assertTrue(AppSettings().shouldRestorePlaybackSession(hasSnapshot = true))
        assertFalse(AppSettings().shouldRestorePlaybackSession(hasSnapshot = false))
        assertFalse(
            AppSettings(restoreLastSession = false)
                .shouldRestorePlaybackSession(hasSnapshot = true),
        )
    }

    @Test
    fun `reanudar solo si la sesion guardada estaba reproduciendo y auto resume esta activo`() {
        assertTrue(AppSettings().shouldResumeSavedPlayback(savedPlayWhenReady = true))
        assertFalse(AppSettings().shouldResumeSavedPlayback(savedPlayWhenReady = false))
        assertFalse(
            AppSettings(autoResumePlayback = false)
                .shouldResumeSavedPlayback(savedPlayWhenReady = true),
        )
        assertFalse(
            AppSettings(restoreLastSession = false)
                .shouldResumeSavedPlayback(savedPlayWhenReady = true),
        )
    }
}
