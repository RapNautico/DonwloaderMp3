package com.example.mimusica.domain.download

/**
 * Solicitud de descarga de UN recurso autorizado (FASE 7.0, contrato).
 * La "instrucción de red" la construye la fuente oficial en FASE 7.1/7.2;
 * Domain solo la describe. Sin URL = la fuente aún no la resolvió.
 */
data class DownloadRequest(
    val sourceId: String,
    val remoteId: String,
    /** Formato elegido entre los que la fuente declara para el recurso. */
    val formatId: String,
    /** URL resuelta por la fuente (redirección oficial) o null si faltará resolver. */
    val url: String?,
    /** Bytes esperados si la fuente los declara; null = desconocido. */
    val expectedSizeBytes: Long? = null,
    /** Título/artista para el nombre final del fichero (se sanitiza en Data). */
    val displayTitle: String,
)
