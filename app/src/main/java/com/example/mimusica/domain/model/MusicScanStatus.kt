package com.example.mimusica.domain.model

data class MusicScanStatus(
    val isScanning: Boolean = false,
    val hasScanned: Boolean = false,
    val indexedFiles: Int = 0,
    val addedSongs: Int = 0,
    val duplicates: Int = 0,
    val failedFiles: Int = 0,
    val error: String? = null,
) {
    companion object {
        fun completed(
            indexedSources: List<SongSourceIdentity>,
            knownSources: Map<String, SongSourceState>,
            addedSongs: Int,
            duplicateImports: Int,
            failedFiles: Int,
        ) = MusicScanStatus(
            hasScanned = true,
            indexedFiles = indexedSources.size,
            addedSongs = addedSongs,
            duplicates = indexedSources.count { source ->
                !source.requiresRefresh(knownSources[source.key])
            } + duplicateImports,
            failedFiles = failedFiles,
        )
    }
}
