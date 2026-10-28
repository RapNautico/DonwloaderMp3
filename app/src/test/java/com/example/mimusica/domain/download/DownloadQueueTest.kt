package com.example.mimusica.domain.download

import com.example.mimusica.domain.download.DownloadQueue.CancelTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Requisitos FASE 7.0 §4: FIFO, límite de 2 concurrentes y cancelación. */
class DownloadQueueTest {

    private val queue = DownloadQueue(maxConcurrent = 2)

    @Test
    fun orden_fifo_en_el_despacho() {
        assertTrue(queue.enqueue(1))
        assertTrue(queue.enqueue(2))
        assertTrue(queue.enqueue(3))

        // Con 2 slots: arrancan 1 y 2 (en orden), 3 espera.
        assertEquals(listOf(1L, 2L), queue.dispatch())
        assertEquals(setOf(1L, 2L), queue.activeIds)
        assertEquals(listOf(3L), queue.pendingIds)
    }

    @Test
    fun limite_de_concurrencia_dos() {
        (1..5).forEach { assertTrue(queue.enqueue(it.toLong())) }
        val started = queue.dispatch()
        assertEquals(2, started.size)
        assertEquals(0, queue.freeSlots)
        assertEquals(3, queue.pendingIds.size)
    }

    @Test
    fun al_liberar_slot_arranca_el_siguiente_en_orden_fifo() {
        (1..3).forEach { assertTrue(queue.enqueue(it.toLong())) }
        queue.dispatch()
        assertTrue(queue.finish(1L))
        assertEquals(listOf(3L), queue.dispatch())
        assertEquals(setOf(2L, 3L), queue.activeIds)
    }

    @Test
    fun cancelar_pendiente_lo_elimina_sin_otros_cambios() {
        (1..3).forEach { assertTrue(queue.enqueue(it.toLong())) }
        queue.dispatch()

        assertEquals(CancelTarget.PENDING_REMOVED, queue.cancel(3L))
        assertFalse(queue.pendingIds.contains(3L))
        // finish(3) ya no aplica: la operación nunca llegó a arrancar.
        assertFalse(queue.finish(3L))
    }

    @Test
    fun cancelar_activo_pide_cancel_de_motor_y_luego_libera() {
        (1..3).forEach { assertTrue(queue.enqueue(it.toLong())) }
        queue.dispatch()

        assertEquals(CancelTarget.ACTIVE_NEEDS_ENGINE_CANCEL, queue.cancel(1L))
        // El slot sigue ocupado hasta confirmar la cancelación real:
        assertEquals(setOf(1L, 2L), queue.activeIds)
        assertTrue(queue.finish(1L))
        assertEquals(setOf(2L), queue.activeIds)
        assertEquals(1, queue.freeSlots)
    }

    @Test
    fun no_se_reencolan_duplicados_ni_terminadas() {
        assertTrue(queue.enqueue(7L))
        assertFalse("duplicado pendiente", queue.enqueue(7L))
        queue.dispatch()
        assertFalse("duplicado activo", queue.enqueue(7L))
        queue.finish(7L)
        assertFalse("no reenqueue de una terminada: reintento = operación nueva", queue.enqueue(7L))
    }

    @Test
    fun cola_vacia_dispatch_devuelve_vacio() = assertEquals(emptyList<Long>(), queue.dispatch())

    @Test
    fun configuracion_no_valida_rechazada() {
        try {
            DownloadQueue(maxConcurrent = 0)
            throw AssertionError("debía lanzar IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("maxConcurrent"))
        }
    }
}
