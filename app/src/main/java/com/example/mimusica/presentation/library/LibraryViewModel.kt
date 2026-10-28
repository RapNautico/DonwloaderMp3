package com.example.mimusica.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mimusica.domain.importing.ImportReport
import com.example.mimusica.domain.model.Song
import com.example.mimusica.domain.repository.SongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Estado de una importación en curso (requisito FASE 5 §7/§8). */
sealed interface ImportState {
    data object Idle : ImportState

    data class Running(val current: Int, val total: Int) : ImportState

    data class Done(val report: ImportReport) : ImportState

    /** No debería ocurrir a nivel de lote (los fallos son por archivo); por si acaso. */
    data class Failed(val message: String) : ImportState
}

/**
 * ViewModel de la biblioteca (_PRESENTATION_): presenta canciones y estado de importación.
 *
 * Concurrencia: un Mutex impide que un usuario pulsa "Añadir música" mientras
 * otra importación sigue procesando (requisito FASE 5 §8). El progreso se
 * emite por [importState] (StateFlow reactivo desde Compose).
 */
class LibraryViewModel(
    private val importAudioUseCase: com.example.mimusica.domain.importing.ImportAudioUseCase,
    private val songRepository: SongRepository,
) : ViewModel() {

    private val songs = MutableStateFlow<List<Song>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private val importMutex = Mutex()

    /** Canciones para la UI. */
    val library: StateFlow<List<Song>> = songs.asStateFlow()

    init {
        viewModelScope.launch {
            songRepository.observeLibrary().collect { list ->
                songs.value = list
                _isLoading.value = false
            }
        }
    }

    /**
     * Importa las URIs seleccionadas en el selector SAF. Nunca lanza: los
     * fallos por archivo están en el reporte; fallos de lote (no cubiertos)
     * se proyectan como ImportState.Failed.
     */
    fun importFiles(uriStrings: List<String>) {
        if (uriStrings.isEmpty()) return
        viewModelScope.launch {
            val locked = importMutex.tryLock()
            if (!locked) return@launch // ya hay una importación en curso: ignorar

            try {
                _importState.value = ImportState.Running(current = 0, total = uriStrings.size)
                val report = try {
                    importAudioUseCase(uriStrings) { current, total ->
                        _importState.value = ImportState.Running(current, total)
                    }
                } catch (unexpected: Throwable) {
                    _importState.value = ImportState.Failed(
                        message = unexpected.message ?: "Error inesperado",
                    )
                    return@launch
                }
                _importState.value = ImportState.Done(report)
            } finally {
                importMutex.unlock()
            }
        }
    }

    /** Vuelve a Idle tras mostrar el resultado (caller:.dismiss tras snackbar). */
    fun dismissImportResult() {
        _importState.update { ImportState.Idle }
    }
}
