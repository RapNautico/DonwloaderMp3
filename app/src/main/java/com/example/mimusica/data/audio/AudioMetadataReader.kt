package com.example.mimusica.data.audio

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
/**
 * Lector de metadatos REALES (requisito FASE 5 §3).
 *
 * - Format/mime real de la pista via MediaExtractor (ground truth del stream,
 *   no la extensión ni el MIME anunciado por el proveedor).
 * - Título/artista/álbum/duración/bitrate/portada via MediaMetadataRetriever.
 *
 * Regla del proyecto: NUNCA inventa valores; lo que no existe es null y el
 * fallback del título está definido en el importador (nombre del documento).
 *
 * Todas las operaciones son BLOQUEANTES: el intentador (`SafAudioImporter`)
 * las ejecuta en Dispatchers.IO.
 */
class AudioMetadataReader(private val context: Context) {

    data class AudioStreamFormat(
        val mime: String?,
        val sampleRateHz: Int?,
        val channels: Int?,
    )

    data class EmbeddedPicture(val bytes: ByteArray?, val mime: String?)

    data class MetadataSnapshot(
        val displayName: String?,
        val stream: AudioStreamFormat?,
        val title: String?,
        val artist: String?,
        val album: String?,
        val durationMs: Long?,
        val bitrateBps: Long?,
        val artwork: EmbeddedPicture?,
        /** Género real del archivo; null/blank → null (nunca inferido, D8.1). */
        val genre: String?,
    )

    fun displayNameOrNull(uri: Uri): String? = runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null, null, null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

    fun sizeBytesOrNull(uri: Uri): Long? = runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.SIZE),
            null, null, null,
        )?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
        }
    }.getOrNull()

    /** Mime/sampleRate/canales de la PRIMERA pista de audio; null si no hay. */
    fun streamFormat(uri: Uri): AudioStreamFormat? {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            var mime: String? = null
            var sampleRateHz: Int? = null
            var channels: Int? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val trackMime = runCatching { format.getString(MediaFormat.KEY_MIME) }
                    .getOrNull()
                if (trackMime?.startsWith("audio/") == true) {
                    mime = trackMime
                    if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRateHz = runCatching { format.getInteger(MediaFormat.KEY_SAMPLE_RATE) }.getOrNull()
                    }
                    if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channels = runCatching { format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) }.getOrNull()
                    }
                    break
                }
            }
            if (mime == null) return null
            return AudioStreamFormat(mime, sampleRateHz, channels)
        } finally {
            extractor.release()
        }
    }

    /** Metadatos descriptivos y portada; devuelve null si la fuente es ilegible. */
    fun descriptiveMetadata(uri: Uri): MetadataSnapshot? {
        val retriever = MediaMetadataRetriever()
        try {
            runCatching { retriever.setDataSource(context, uri) }.getOrElse { return null }

            fun md(key: Int): String? = normalizeOptionalText(
                runCatching { retriever.extractMetadata(key) }.getOrNull(),
            )

            val artworkBytes = runCatching { retriever.embeddedPicture }.getOrNull()
            val artwork = artworkBytes?.takeIf { it.isNotEmpty() }
                ?.let { EmbeddedPicture(it, sniffMimeForImage(it)) }

            return MetadataSnapshot(
                displayName = displayNameOrNull(uri),
                stream = streamFormat(uri),
                title = md(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = md(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                album = md(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                durationMs = md(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
                bitrateBps = md(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull(),
                artwork = artwork,
                genre = md(MediaMetadataRetriever.METADATA_KEY_GENRE),
            )
        } finally {
            runCatching { retriever.release() }
        }
    }

    companion object {
        /**
         * Normalización de textos de metadatos (requisito D8.1 y regla de
         * datos reales): trim; vacío/blank → null (= "desconocido" en UI).
         * Función PURA y testeable en JVM sin dispositivo.
         */
        fun normalizeOptionalText(raw: String?): String? =
            raw?.takeIf { it.isNotBlank() }?.trim()
    }

    private fun sniffMimeForImage(bytes: ByteArray): String? = when {        bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "image/jpeg"
        bytes.size >= 4 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() -> "image/png"
        else -> null
    }
}
