package com.example.mimusica.data.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * FASE 8.1: normalización de textos de metadatos (D8.1).
 * Regla de datos reales: blank/vacío → null (= desconocido); el resto, trim.
 * Función pura, testeable en JVM sin dispositivo.
 */
class TextNormalizationTest {

    @Test
    fun texto_normal_se_recorta() {
        assertEquals("Rock", AudioMetadataReader.normalizeOptionalText("  Rock  "))
        assertEquals("Rock", AudioMetadataReader.normalizeOptionalText("Rock"))
    }

    @Test
    fun nulo_permanece_nulo() {
        assertNull(AudioMetadataReader.normalizeOptionalText(null))
    }

    @Test
    fun vacio_o_blank_es_null() {
        assertNull(AudioMetadataReader.normalizeOptionalText(""))
        assertNull(AudioMetadataReader.normalizeOptionalText("   "))
        assertNull(AudioMetadataReader.normalizeOptionalText("\t\n"))
    }
}
