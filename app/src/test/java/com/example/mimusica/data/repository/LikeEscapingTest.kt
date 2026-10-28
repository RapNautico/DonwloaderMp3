package com.example.mimusica.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * FASE 8.2: política de comodines de la búsqueda (D8.3, documentada en
 * ARCHITECTURE §22.11): los comodines SQL (`%`, `_`) y el propio `\\` provienen
 * de la consulta del usuario y se tratan como TEXTO LITERAL vía `ESCAPE '\\'`.
 * Función pura, sin dispositivo.
 */
class LikeEscapingTest {

    @Test
    fun barra_invertida_se_duplica_primero() {
        assertEquals("a\\\\b", SongRepositoryImpl.escapeLikePattern("a\\b"))
    }

    @Test
    fun porcentaje_se_escapa() {
        assertEquals("100\\%", SongRepositoryImpl.escapeLikePattern("100%"))
    }

    @Test
    fun guion_bajo_se_escapa() {
        assertEquals("a\\_b", SongRepositoryImpl.escapeLikePattern("a_b"))
    }

    @Test
    fun texto_normal_queda_intacto_y_el_repositorio_aplica_recorte_antes() {
        assertEquals("rock", SongRepositoryImpl.escapeLikePattern("rock"))
    }

    @Test
    fun combinacion_de_comodines_y_barras() {
        // `\%` en la BD ya es un `\` escapado + literal %:
        assertEquals("a\\\\\\%\\_b", SongRepositoryImpl.escapeLikePattern("a\\%_b"))
    }
}
