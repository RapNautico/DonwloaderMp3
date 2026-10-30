package com.example.mimusica.data.audio

import android.content.Context
import java.io.File
import java.security.MessageDigest

private val ARTWORK_MD5: MessageDigest = MessageDigest.getInstance("MD5")

internal fun artworkFileName(uri: String, byteSize: Int): String {
    val hash = ARTWORK_MD5.digest(uri.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    return "$hash-v$byteSize"
}

/**
 * Almacén en disco para portadas incrustadas (FASE 5 §9).
 *
 * Estrategia:
 * - Copia los bytes a `filesDir/artwork/` UNA vez (nombre determinista a partir
 *   del hash de la URI + tamaño): reimportar el mismo archivo no duplica la
 *   imagen ni la re-decodifica.
 * - La copia no depende del permiso SAF (persistente aunque el permiso expire).
 * - No se decodifica a Bitmap en importación; cada pantalla decide cómo cargar
 *   (por ejemplo, downsampleando en Dispatchers.IO).
 */
class ArtworkStore(context: Context) {
    private val dir = File(context.filesDir, "artwork")

    fun store(uri: String, artwork: AudioMetadataReader.EmbeddedPicture?): String? {
        val bytes = artwork?.bytes ?: return null
        if (bytes.isEmpty()) return null
        val extension = when (artwork.mime) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            else -> "img" // firma desconocida: bytes tal cual
        }
        val target = File(dir, artworkFileName(uri, bytes.size) + "." + extension)
        return runCatching {
            dir.mkdirs()
            if (!target.exists()) writeAtomically(target, bytes)
            target.absolutePath
        }.getOrNull()
    }

    private fun writeAtomically(target: File, bytes: ByteArray) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.outputStream().use { it.write(bytes) }
        if (!tmp.renameTo(target)) {
            tmp.delete()
            throw IllegalStateException("No se pudo guardar la portada: ${target.name}")
        }
    }
}
