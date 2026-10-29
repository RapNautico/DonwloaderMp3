package com.example.mimusica.data.audio

import com.example.mimusica.domain.model.SongFileAvailability
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class SafSongFileValidatorTest {

    @Test
    fun archivo_ausente_se_distingue_de_permiso_revocado() {
        assertEquals(
            SongFileAvailability.MISSING,
            classifyOpenFailure(FileNotFoundException()),
        )
        assertEquals(
            SongFileAvailability.INACCESSIBLE,
            classifyOpenFailure(SecurityException()),
        )
    }

    @Test
    fun errores_de_lectura_y_proveedor_se_marcan_inaccesibles() {
        assertEquals(
            SongFileAvailability.INACCESSIBLE,
            classifyOpenFailure(IOException()),
        )
        assertEquals(
            SongFileAvailability.INACCESSIBLE,
            classifyOpenFailure(UnsupportedOperationException()),
        )
    }

    @Test
    fun uri_o_contenido_que_el_extractor_no_acepta_se_marcan_invalidos() {
        assertEquals(
            SongFileAvailability.INVALID,
            classifyOpenFailure(IllegalArgumentException()),
        )
        assertEquals(
            SongFileAvailability.INVALID,
            classifyOpenFailure(IllegalStateException()),
        )
        assertEquals(
            SongFileAvailability.INVALID,
            classifyParseFailure(IllegalArgumentException()),
        )
        assertEquals(
            SongFileAvailability.INVALID,
            classifyParseFailure(IllegalStateException()),
        )
        assertEquals(
            SongFileAvailability.INVALID,
            classifyParseFailure(IOException()),
        )
    }

    @Test
    fun fallos_de_acceso_durante_el_analisis_conservan_su_clasificacion() {
        assertEquals(
            SongFileAvailability.MISSING,
            classifyParseFailure(FileNotFoundException()),
        )
        assertEquals(
            SongFileAvailability.INACCESSIBLE,
            classifyParseFailure(SecurityException()),
        )
        assertEquals(
            SongFileAvailability.INACCESSIBLE,
            classifyParseFailure(UnsupportedOperationException()),
        )
    }

    @Test
    fun cancelacion_no_se_convierte_en_un_estado_de_disponibilidad() {
        val openCancellation = CancellationException("open cancelled")
        val parseCancellation = CancellationException("parse cancelled")

        assertCancellationPropagated(openCancellation, ::classifyOpenFailure)
        assertCancellationPropagated(parseCancellation, ::classifyParseFailure)
    }

    private fun assertCancellationPropagated(
        expected: CancellationException,
        classify: (Exception) -> SongFileAvailability,
    ) {
        try {
            classify(expected)
            fail("Cancellation must be rethrown")
        } catch (actual: CancellationException) {
            assertSame(expected, actual)
        }
    }
}
