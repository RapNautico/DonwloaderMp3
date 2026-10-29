package com.example.mimusica.data.audio

import android.content.Context
import android.net.Uri
import com.example.mimusica.domain.model.AudioFormat
import com.example.mimusica.domain.model.SongFileAvailability
import com.example.mimusica.domain.repository.SongFileValidator
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Verifica URI SAF con el lector de pistas que ya se usa al importar.
 * El parseo de contenedor y las llamadas a proveedores se ejecutan en IO.
 */
class SafSongFileValidator(
    context: Context,
    private val metadataReader: AudioMetadataReader,
) : SongFileValidator {

    private val contentResolver = context.applicationContext.contentResolver

    override suspend fun validate(uri: String): SongFileAvailability = withContext(Dispatchers.IO) {
        val parsedUri = Uri.parse(uri)
        val scheme = parsedUri.scheme?.lowercase()
        if (scheme !in setOf("content", "file") ||
            (scheme == "content" && parsedUri.authority.isNullOrBlank()) ||
            (scheme == "file" && parsedUri.path.isNullOrBlank())
        ) {
            return@withContext SongFileAvailability.INVALID
        }

        val descriptor = try {
            contentResolver.openAssetFileDescriptor(parsedUri, "r")
        } catch (failure: CancellationException) {
            throw failure
        } catch (failure: FileNotFoundException) {
            return@withContext classifyOpenFailure(failure)
        } catch (failure: SecurityException) {
            return@withContext classifyOpenFailure(failure)
        } catch (failure: IOException) {
            return@withContext classifyOpenFailure(failure)
        } catch (failure: UnsupportedOperationException) {
            return@withContext classifyOpenFailure(failure)
        } catch (failure: IllegalArgumentException) {
            return@withContext classifyOpenFailure(failure)
        } catch (failure: IllegalStateException) {
            return@withContext classifyOpenFailure(failure)
        }

        if (descriptor == null) return@withContext SongFileAvailability.INACCESSIBLE

        val stream = try {
            descriptor.use(metadataReader::streamFormat)
        } catch (failure: CancellationException) {
            throw failure
        } catch (failure: FileNotFoundException) {
            return@withContext classifyParseFailure(failure)
        } catch (failure: SecurityException) {
            return@withContext classifyParseFailure(failure)
        } catch (failure: IOException) {
            return@withContext classifyParseFailure(failure)
        } catch (failure: UnsupportedOperationException) {
            return@withContext classifyParseFailure(failure)
        } catch (failure: IllegalArgumentException) {
            return@withContext classifyParseFailure(failure)
        } catch (failure: IllegalStateException) {
            return@withContext classifyParseFailure(failure)
        }

        if (stream == null || AudioFormat.fromMime(stream.mime) == null) {
            SongFileAvailability.INVALID
        } else {
            SongFileAvailability.AVAILABLE
        }
    }
}

internal fun classifyOpenFailure(failure: Exception): SongFileAvailability =
    when (failure) {
        is CancellationException -> throw failure
        is FileNotFoundException -> SongFileAvailability.MISSING
        is SecurityException -> SongFileAvailability.INACCESSIBLE
        is IOException -> SongFileAvailability.INACCESSIBLE
        is UnsupportedOperationException -> SongFileAvailability.INACCESSIBLE
        is IllegalArgumentException, is IllegalStateException -> SongFileAvailability.INVALID
        else -> throw failure
    }

internal fun classifyParseFailure(failure: Exception): SongFileAvailability =
    when (failure) {
        is CancellationException -> throw failure
        is FileNotFoundException -> SongFileAvailability.MISSING
        is SecurityException -> SongFileAvailability.INACCESSIBLE
        is IOException -> SongFileAvailability.INVALID
        is UnsupportedOperationException -> SongFileAvailability.INACCESSIBLE
        is IllegalArgumentException, is IllegalStateException -> SongFileAvailability.INVALID
        else -> throw failure
    }
