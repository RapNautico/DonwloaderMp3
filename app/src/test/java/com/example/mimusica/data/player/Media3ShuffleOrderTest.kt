package com.example.mimusica.data.player

import androidx.media3.common.C
import androidx.media3.exoplayer.source.ShuffleOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class Media3ShuffleOrderTest {
    @Test
    fun una_vuelta_shuffle_visita_cada_indice_una_vez_y_la_anterior_es_reversible() {
        for (queueSize in 2..10) {
            val order = ShuffleOrder.DefaultShuffleOrder(queueSize, 1234L)
            val forwardOrder = mutableListOf<Int>()
            var index = order.firstIndex
            while (index != C.INDEX_UNSET) {
                forwardOrder += index
                index = order.getNextIndex(index)
            }

            assertEquals(queueSize, forwardOrder.size)
            assertEquals(queueSize, forwardOrder.toSet().size)
            assertEquals(C.INDEX_UNSET, order.getNextIndex(forwardOrder.last()))

            val backwardOrder = mutableListOf<Int>()
            index = forwardOrder.last()
            while (index != C.INDEX_UNSET) {
                backwardOrder += index
                index = order.getPreviousIndex(index)
            }
            assertEquals(forwardOrder.reversed(), backwardOrder)
        }
    }

    @Test
    fun cola_unitaria_no_repite_indices_dentro_del_ciclo_shuffle() {
        val order = ShuffleOrder.DefaultShuffleOrder(1, 1234L)

        assertEquals(0, order.firstIndex)
        assertEquals(C.INDEX_UNSET, order.getNextIndex(0))
    }
}
