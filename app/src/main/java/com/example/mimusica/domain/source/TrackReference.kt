package com.example.mimusica.domain.source

/**
 * Referencia estable a una pista en una fuente remota (FASE 7.0).
 *
 * [downloadAllowed] es el campo explícito de autorización POR RECURSO que
 * devuelven APIs oficiales con esa semántica (para Jamendo: `audiodownload_allowed`).
 * `null` = la fuente no informa este recurso (desconocido, nunca se asume permitido).
 */
data class TrackReference(
    val sourceId: String,
    val remoteId: String,
    val displayTitle: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long?,
    val downloadAllowed: Boolean?,
    /** URL pública de la pista (metadata/reproducción); no presupone descargable. */
    val webUrl: String?,
)
