package com.example.mimusica.domain.source

/**
 * Metadatos detallados de una pista obtenidos por vía oficial (FASE 7.0).
 *
 * Nota honesta (AGENTS §8/§19): los formatos listados son los que la fuente
 * declara disponibles para ese recurso; el formato/bitrate definitivos se
 * verifican después de la descarga con el lector real de FASE 5
 * (MediaExtractor), nunca con la información de la fuente sola.
 */
data class SourceMetadata(
    val reference: TrackReference,
    /** Formatos descargables declarados por la fuente para esta pista. */
    val downloadableFormats: List<DownloadFormatSpec>,
    /** Portada si la fuente la sirve (no obligatoria). */
    val artworkUrl: String? = null,
)

/** Especificación de un formato descargable concreto declarado por la fuente. */
data class DownloadFormatSpec(
    /** Ej. "mp32", "flac", "mp3", "ogg": etiqueta oficial de la fuente. */
    val formatId: String,
    /** Mime real declarado por la fuente (null = no informado). */
    val mimeType: String? = null,
    /** Bytes declarados por la fuente (null = desconocido; nunca se inventa). */
    val sizeBytes: Long? = null,
    /** Bitrate declarado (null = desconocido; no se infiere del formato). */
    val bitrateBps: Long? = null,
)
