package com.example.mimusica.presentation.library

import com.example.mimusica.domain.importing.ImportReport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

class LibraryImportModel(
    private val importAudio: suspend (
        uris: List<String>,
        onProgress: suspend (current: Int, total: Int) -> Unit,
    ) -> ImportReport,
) {

    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state.asStateFlow()

    private val importMutex = Mutex()

    suspend fun importFiles(uriStrings: List<String>) {
        if (uriStrings.isEmpty() || !importMutex.tryLock()) return

        try {
            _state.value = ImportState.Running(current = 0, total = uriStrings.size)
            val report = try {
                importAudio(uriStrings) { current, total ->
                    _state.value = ImportState.Running(current, total)
                }
            } catch (cancelled: CancellationException) {
                _state.value = ImportState.Idle
                throw cancelled
            } catch (unexpected: Throwable) {
                _state.value = ImportState.Failed(
                    message = unexpected.message ?: "Error inesperado",
                )
                return
            }
            _state.value = ImportState.Done(report)
        } finally {
            importMutex.unlock()
        }
    }

    fun dismissResult() {
        _state.value = ImportState.Idle
    }
}
