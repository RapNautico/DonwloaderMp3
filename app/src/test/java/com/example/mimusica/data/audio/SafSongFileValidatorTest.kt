package com.example.mimusica.data.audio

import com.example.mimusica.domain.model.SongFileAvailability
import java.io.FileNotFoundException
import java.io.IOException
import org.junit.Assert.assertEquals
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
}
