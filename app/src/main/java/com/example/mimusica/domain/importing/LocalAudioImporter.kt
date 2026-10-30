package com.example.mimusica.domain.importing

import com.example.mimusica.domain.model.Song

/**
 * Proveedor de preparación de audios locales (contrato de Domain sin Android).
 *
 * La implementación (Data) lee la fuente real (SAF) y devuelve, por URI, una
 * [PreparedAudio] lista para persistir, o el motivo de rechazo.
 */
interface LocalAudioImporter {
    /**
     * Prepara N archivos. Llama a [onFileProcessed] después de cada archivo
     * para ui de progreso. Nunca lanza: los fallos se devuelven por archivo.
     */
    suspend fun prepare(
        uris: List<String>,
        onFileProcessed: suspend (index: Int, total: Int) -> Unit = { _, _ -> },
    ): List<PreparedAudio>
}

/** Resultado por archivo. Cyclo exclusivo: [song] != null XOR [failure] != null. */
data class PreparedAudio(
    val requestedUri: String,
    val displayName: String?,
    val song: Song? = null,
    val failure: ImportFailureReason? = null,
    val sourceIdentityKey: String? = null,
    val sourceModifiedAtMs: Long? = null,
    val sourceSizeBytes: Long? = null,
) {
    val failureReason: ImportFailureReason?
        get() = if (song != null) null else failure ?: ImportFailureReason.UNREADABLE
}

enum class ImportFailureReason {
    /** Uri ilegal o proveedor inaccesible (permiso revocado, archivo borrado...). */
    INACCESSIBLE,
    /** Contenido no reconocido como uno de los formatos soportados. */
    UNSUPPORTED_FORMAT,
    /** Metadatos/cabecera corruptos o incompletos de forma irrecuperable. */
    UNREADABLE,
    /** Supera el límite de tamaño admitido. */
    TOO_LARGE,
}

/** Informe final de una importación. */
data class ImportReport(
    val totalRequested: Int,
    val songsAdded: Int,
    /** Canciones cuya URI ya estaba en Room (re-import). */
    val duplicatesSkipped: Int,
    val failures: List<FailedImport>,
) {
    val rejectedCount: Int get() = failures.size

    data class FailedImport(val uri: String, val reason: ImportFailureReason)
}
