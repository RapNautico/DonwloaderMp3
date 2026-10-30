package com.example.mimusica.data.player

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class PlaybackCommandRunnerTest {
    @Test
    fun cancelacion_se_propaga_y_no_se_convierte_en_error() = runBlocking {
        val cancelled = CancellationException("playback cancelled")
        var handledFailure: Exception? = null
        var caught: CancellationException? = null

        try {
            executePlaybackCommand(
                command = { throw cancelled },
                onFailure = { handledFailure = it },
            )
        } catch (actual: CancellationException) {
            caught = actual
        }

        assertSame(cancelled, caught)
        assertNull(handledFailure)
    }

    @Test
    fun excepcion_ordinaria_se_entrega_al_manejador_de_estado_de_error() = runBlocking {
        val failure = IllegalStateException("archivo inaccesible")
        var handledFailure: Exception? = null

        executePlaybackCommand(
            command = { throw failure },
            onFailure = { handledFailure = it },
        )

        assertSame(failure, handledFailure)
    }
}
