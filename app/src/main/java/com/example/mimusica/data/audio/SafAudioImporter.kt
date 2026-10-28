package com.example.mimusica.data.audio

import android.content.Context
import android.net.Uri
import com.example.mimusica.domain.importing.ImportFailureReason
import com.example.mimusica.domain.importing.LocalAudioImporter
import com.example.mimusica.domain.importing.PreparedAudio
import com.example.mimusica.domain.model.AudioFormat
import com.example.mimusica.domain.model.Song
import java.io.FileNotFoundException
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Importador de audio local sobre SAF (requisito FASE 5).
 *
 * Por cada URI:
 *  1. Guardas y límites: tamaño consultado ANTES de leer (TOO_LARGE).
 *  2. Metadatos reales: MediaExtractor (formato del stream) + MediaMetadataRetriever.
 *  3. Validación por MIME real de la pista (NO solo extensión) contra
 *     `AudioFormat`; sin pista de audio reconocible → UNSUPPORTED_FORMAT.
 *  4. Fallback del título: RETRIEVER título → displayName del documento →
 *     fallback estático (documentado, nunca inventado).
 *  5. Portada: copia única en filesDir (ArtworkStore), si existe.
 *  6. Errores por archivo: NUNCA aborta el lote; cada URI devuelve un resultado.
 *
 * Todas las lecturas discriminatorias en [Dispatchers.IO].
 */
class SafAudioImporter(
    appContext: Context,
    private val metadataReader: AudioMetadataReader,
    private val artworkStore: ArtworkStore,
) : LocalAudioImporter {

    private val appContext = appContext.applicationContext

    /** Guarda: archivos mayores se rechazan con TOO_LARGE (antes de leerlos). */
    private val maxFileBytes: Long = MAX_FILE_BYTES_DEFAULT

    override suspend fun prepare(
        uris: List<String>,
        onFileProcessed: suspend (index: Int, total: Int) -> Unit,
    ): List<PreparedAudio> = withContext(Dispatchers.IO) {
        uris.mapIndexed { index, rawUri ->
            val prepared = prepareSingle(rawUri)
            onFileProcessed(index + 1, uris.size)
            prepared
        }
    }

    private suspend fun prepareSingle(rawUri: String): PreparedAudio = withContext(Dispatchers.IO) {
        val uri = try {
            Uri.parse(rawUri)
        } catch (_: Throwable) {
            return@withContext PreparedAudio(
                requestedUri = rawUri,
                displayName = null,
                failure = ImportFailureReason.INACCESSIBLE,
            )
        }

        if (uri.scheme.isNullOrEmpty() || uri.authority.isNullOrEmpty()) {
            return@withContext PreparedAudio(
                requestedUri = rawUri,
                displayName = null,
                failure = ImportFailureReason.INACCESSIBLE,
            )
        }

        // Límite de tamaño (cuando el proveedor puede informarlo).
        metadataReader.sizeBytesOrNull(uri)?.let { size ->
            if (size > maxFileBytes) {
                return@withContext PreparedAudio(
                    requestedUri = rawUri,
                    displayName = null,
                    failure = ImportFailureReason.TOO_LARGE,
                )
            }
        }

        val displayName = metadataReader.displayNameOrNull(uri)

        val metadata = try {
            metadataReader.descriptiveMetadata(uri)
        } catch (security: SecurityException) {
            return@withContext PreparedAudio(rawUri, displayName, failure = ImportFailureReason.INACCESSIBLE)
        } catch (notFound: FileNotFoundException) {
            return@withContext PreparedAudio(rawUri, displayName, failure = ImportFailureReason.INACCESSIBLE)
        } catch (io: IOException) {
            return@withContext PreparedAudio(rawUri, displayName, failure = ImportFailureReason.UNREADABLE)
        } catch (state: IllegalStateException) {
            return@withContext PreparedAudio(rawUri, displayName, failure = ImportFailureReason.UNREADABLE)
        } catch (other: Throwable) {
            return@withContext PreparedAudio(rawUri, displayName, failure = ImportFailureReason.UNREADABLE)
        }

        if (metadata == null) {
            return@withContext PreparedAudio(rawUri, displayName, failure = ImportFailureReason.UNREADABLE)
        }

        val stream = metadata.stream
            ?: return@withContext PreparedAudio(rawUri, displayName, failure = ImportFailureReason.UNSUPPORTED_FORMAT)
        AudioFormat.fromMime(stream.mime)
            ?: return@withContext PreparedAudio(rawUri, displayName, failure = ImportFailureReason.UNSUPPORTED_FORMAT)

        val artworkPath = artworkStore.store(rawUri, metadata.artwork)

        val song = Song(
            uri = rawUri,
            title = metadata.title ?: displayName ?: TITLE_FALLBACK,
            artist = metadata.artist,
            album = metadata.album,
            // Duración real; 0 = la fuente no la informó (fallback documentado).
            durationMs = metadata.durationMs ?: 0L,
            dateAddedMs = System.currentTimeMillis(),
            mimeType = stream.mime,
            sizeBytes = metadataReader.sizeBytesOrNull(uri),
            bitrateBps = metadata.bitrateBps,
            sampleRateHz = stream.sampleRateHz,
            channels = stream.channels,
            artworkPath = artworkPath,
            genre = metadata.genre,
        )

        PreparedAudio(requestedUri = rawUri, displayName = displayName, song = song, failure = null)
    }

    private companion object {
        /** 512 MB: guardia de sanidad contra archivos absurdos (documentado). */
        const val MAX_FILE_BYTES_DEFAULT = 512L * 1024L * 1024L

        /** Fallback del título cuando ni el retriever ni el displayName lo informan. */
        const val TITLE_FALLBACK = "Audio sin título"
    }
}
