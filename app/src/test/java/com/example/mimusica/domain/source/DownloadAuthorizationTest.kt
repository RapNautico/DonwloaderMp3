package com.example.mimusica.domain.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Requisito FASE 7.0 §2/§4: una capacidad GENERAL de la fuente NO habilita
 * la descarga de un recurso concreto; la autorización se evalúa por pista.
 */
class DownloadAuthorizationTest {

    private val fuente = SourceCapabilities(
        sourceId = "jamendo",
        canSearch = true,
        canDownload = true,
        attributionText = "Atribución exigida por API ToU",
    )
    private val fuenteSinDescarga = fuente.copy(canDownload = false)

    @Test
    fun fuente_con_descarga_y_pista_que_lo_permite() {
        val track = trackRef(downloadAllowed = true)
        assertEquals(DownloadVerdict.GRANTED, evaluateDownloadAuthorization(fuente, track))
    }

    @Test
    fun capacidad_general_no_descarga_pistas_denegadas() {
        val track = trackRef(downloadAllowed = false)
        assertEquals(DownloadVerdict.DENIED_BY_TRACK, evaluateDownloadAuthorization(fuente, track))
    }

    @Test
    fun pista_que_no_informa_permiso_es_desconocida_sin_suponer() {
        val track = trackRef(downloadAllowed = null)
        assertEquals(DownloadVerdict.UNKNOWN, evaluateDownloadAuthorization(fuente, track))
    }

    @Test
    fun fuente_sin_via_de_descarga_deniega_incluso_con_pista_disponible() {
        val track = trackRef(downloadAllowed = true)
        assertEquals(DownloadVerdict.DENIED_BY_SOURCE, evaluateDownloadAuthorization(fuenteSinDescarga, track))
    }

    @Test
    fun atribucion_obligatoria_queda_en_el_contrato() =
        assertTrue(fuente.attributionText!!.isNotBlank())

    private fun trackRef(downloadAllowed: Boolean?) = TrackReference(
        sourceId = "jamendo",
        remoteId = "10",
        displayTitle = "T",
        artist = null,
        album = null,
        durationMs = 10_000L,
        downloadAllowed = downloadAllowed,
        webUrl = null,
    )
}
