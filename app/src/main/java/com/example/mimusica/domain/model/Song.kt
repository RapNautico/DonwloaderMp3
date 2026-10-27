package com.example.mimusica.domain.model

/**
 * Canción del modelo de dominio (libre de cualquier tecnología).
 *
 * La identidad de una canción es su origen: la misma URI de contenido
 * (content:// o file://) siempre representa el mismo archivo en el dispositivo.
 *
 * Regla del proyecto (AGENTS.md §8): solo metadatos extraídos realmente.
 * Un valor null significa "desconocido"; nunca se inventa calidad de audio.
 */
data class Song(
    val uri: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long,
    /** Fecha de incorporación a la biblioteca (epoch ms). */
    val dateAddedMs: Long,
    /** Metadatos opcionales justificados: null = aún no extraído/no disponible. */
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val bitrateBps: Long? = null,
)
