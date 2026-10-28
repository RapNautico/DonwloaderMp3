package com.example.mimusica.domain.download

import com.example.mimusica.domain.download.DownloadState.Analyzing
import com.example.mimusica.domain.download.DownloadState.Cancelled
import com.example.mimusica.domain.download.DownloadState.Completed
import com.example.mimusica.domain.download.DownloadState.Downloading
import com.example.mimusica.domain.download.DownloadState.Failed
import com.example.mimusica.domain.download.DownloadState.Pending
import com.example.mimusica.domain.download.DownloadState.Processing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Requisitos FASE 7.0 §4: transiciones válidas, inválidas y terminales. */
class DownloadStateTest {

    @Test
    fun camino_feliz_es_valido() {
        assertEquals(Pending, DownloadState.Pending) // api de enum estable
        assertEquals(Analyzing, DownloadState.transition(Pending, Analyzing).getOrThrow())
        assertEquals(Downloading, DownloadState.transition(Analyzing, Downloading).getOrThrow())
        assertEquals(Processing, DownloadState.transition(Downloading, Processing).getOrThrow())
        assertEquals(Completed, DownloadState.transition(Processing, Completed).getOrThrow())
    }

    @Test
    fun fallos_y_cancelaciones_durante_el_proceso() {
        assertEquals(Failed, DownloadState.transition(Analyzing, Failed).getOrThrow())
        assertEquals(Cancelled, DownloadState.transition(Downloading, Cancelled).getOrThrow())
        assertEquals(Failed, DownloadState.transition(Processing, Failed).getOrThrow())
        assertEquals(Cancelled, DownloadState.transition(Pending, Cancelled).getOrThrow())
    }

    @Test
    fun transiciones_invalidas_se_rechazan() {
        // No se salta estados: Pending no puede descargar directamente.
        assertTrue(DownloadState.transition(Pending, Downloading).isFailure)
        // Downloading no puede ir a Completed directamente (debe pasar por Processing).
        assertTrue(DownloadState.transition(Downloading, Completed).isFailure)
        // Processing no puede regresar a Downloading.
        assertTrue(DownloadState.transition(Processing, Downloading).isFailure)
        // Analyzing no convierte la espera en éxito.
        assertTrue(DownloadState.transition(Analyzing, Completed).isFailure)
    }

    @Test
    fun estados_terminales_sin_salida() {
        // Un ítem terminado NO puede reanimarse a sí mismo: el "reintentar"
        // se modela como UNA NUEVA operación en cola, no como transición.
        for (terminal in listOf(Completed, Failed, Cancelled)) {
            assertTrue("$terminal debe ser terminal", terminal.isTerminal)
            assertTrue(DownloadState.transition(terminal, Pending).isFailure)
            assertTrue(DownloadState.transition(terminal, Downloading).isFailure)
        }
        assertFalse(DownloadState.transition(Failed, Completed).isSuccess)
        assertFalse(DownloadState.transition(Cancelled, Completed).isSuccess)
    }

    @Test
    fun transicion_invalida_explica_estado_actual_y_permitidos() {
        val failure = DownloadState.transition(Pending, Processing).exceptionOrNull()
        assertTrue(failure is IllegalStateException)
        assertTrue((failure as IllegalStateException).message!!.contains("Pending"))
        assertTrue(failure.message!!.contains("Analyzing"))
    }
}
