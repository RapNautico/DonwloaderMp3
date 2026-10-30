package com.example.mimusica.domain.importing

import com.example.mimusica.domain.model.Song
import com.example.mimusica.domain.model.SongSourceIdentity
import com.example.mimusica.domain.model.SongSourceRecord
import com.example.mimusica.domain.repository.SongRepository
import com.example.mimusica.domain.repository.SongSourceRepository

/**
 * Caso de uso: registrar audios locales en la biblioteca Room
 * (Presentation → Domain → Data → Room, requisito FASE 5 §4).
 *
 * Puramente orquestación: sin Android, testeable en JVM con fakes.
 * La deduplicación por URI la aplica Room (IGNORE) vía el repositorio.
 */
class ImportAudioUseCase(
    private val importer: LocalAudioImporter,
    private val songRepository: SongRepository,
    private val sourceRepository: SongSourceRepository? = null,
) {
    suspend operator fun invoke(
        uris: List<String>,
        onProgress: suspend (current: Int, total: Int) -> Unit = { _, _ -> },
        sourceIdentities: Map<String, SongSourceIdentity> = emptyMap(),
    ): ImportReport {
        val prepared = importer.prepare(uris, onFileProcessed = onProgress)
        val songs = prepared.mapNotNull { it.song }
        val sourceRecords = prepared.mapNotNull { item ->
            item.song?.let { song ->
                SongSourceRecord(
                    song = song,
                    identity = sourceIdentities[item.requestedUri]
                        ?: item.sourceIdentityKey?.let {
                            SongSourceIdentity(it, item.sourceModifiedAtMs, item.sourceSizeBytes)
                        },
                )
            }
        }
        val added = if (sourceRepository != null) {
            sourceRepository.ingest(sourceRecords)
        } else {
            songRepository.addSongs(songs)
        }
        return ImportReport(
            totalRequested = uris.size,
            songsAdded = added,
            duplicatesSkipped = songs.size - added,
            failures = prepared.filter { it.song == null }
                .map { ImportReport.FailedImport(it.requestedUri, it.failureReason!!) },
        )
    }
}
