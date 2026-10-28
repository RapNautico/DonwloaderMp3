package com.example.mimusica.domain.model

/**
 * Formatos de audio reconocibles por Mi Música (requisito FASE 5 §2).
 *
 * Nota honesta: un dispositivo puede reproducir un mime pero no otro
 * (Android soporta el contenedor MP4, OGG, etc. según fabricante/API);
 * `targetSdk 36` cubre FLAC/WAV/OGG/Opus en AudioTrack/MediaCodec, pero no se
 * garantiza integridad por proveedor. La validación usa el MIME REAL de la
 * pista (MediaExtractor), no solo la extensión.
 */
enum class AudioFormat(
    val mimeTypes: Set<String>,
) {
    MP3(setOf("audio/mpeg", "audio/mp3", "audio/mpeg3", "audio/x-mpeg-3")),
    AAC(setOf("audio/aac", "audio/mp4a-latm")),
    M4A(setOf("audio/mp4", "audio/x-m4a", "audio/m4b")),
    FLAC(setOf("audio/flac", "audio/x-flac", "application/x-flac")),
    WAV(setOf("audio/wav", "audio/x-wav", "audio/vnd.wave", "audio/wave")),
    OGG(setOf("audio/ogg", "audio/vorbis", "application/ogg")),
    OPUS(setOf("audio/opus", "audio/ogg-opus"));

    companion object {
        fun fromMime(rawMime: String?): AudioFormat? {
            // Se descartan parámetros ("audio/ogg; codecs=opus") y vacíos.
            val mime = rawMime?.trim()?.lowercase()?.substringBefore(';')?.trim()
                ?.takeIf { it.isNotEmpty() } ?: return null
            // 1) coincidencia exacta; 2) el patrón es prefijo del mime real.
            return entries.firstOrNull { fmt -> mime in fmt.mimeTypes }
                ?: entries.firstOrNull { fmt -> fmt.mimeTypes.any { mime.startsWith(it) } }
        }
    }
}
