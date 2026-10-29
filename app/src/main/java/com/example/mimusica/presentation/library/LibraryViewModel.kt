package com.example.mimusica.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mimusica.domain.importing.ImportReport
import com.example.mimusica.domain.model.SongFileAvailability
import com.example.mimusica.domain.model.SongSort
import com.example.mimusica.domain.repository.SongRepository
import com.example.mimusica.domain.repository.SongFileValidator
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
    songFileValidator: SongFileValidator,
) : ViewModel() {

    private val importModel = LibraryImportModel { uris, onProgress ->
        importAudioUseCase(uris, onProgress)
    }
    val importState: StateFlow<ImportState> = importModel.state

    private val loadModel = LibraryLoadModel(songRepository)

    /** Carga inicial, contenido y error de biblioteca son estados excluyentes explícitos. */
    val libraryState: StateFlow<LibraryLoadState> = loadModel.state

    // ---------- Gestión: eliminar de biblioteca (FASE 8.6) ----------

    /**
     * Eliminación SOLO del registro Room (D8.3, ConfirmDialog en la UI).
     * Reproducir (FASE 9) y Añadir a playlist (FASE 10) son callbacks
     * FUTUROS del plan 8.6: todavía NO tienen lógica aquí ni en la UI.
     */
    private val managerModel = LibraryManagerModel(songRepository)
    private val fileCheckModel = LibraryFileCheckModel(songFileValidator)

    /** Estado por URI de la última comprobación explícita de archivo. */
    val fileAvailability: StateFlow<Map<String, SongFileAvailability>> = fileCheckModel.availability

    /** true mientras hay una eliminación en curso. */
    val manageBusy: StateFlow<Boolean> = managerModel.busy

    /** Error de la última eliminación; null = sin error. */
    val manageError: StateFlow<String?> = managerModel.error

    /** Decide ya CONFIRMADA por el usuario (ConfirmDialog); lanza en VM scope. */
    fun deleteLibrarySong(uri: String) {
        viewModelScope.launch { managerModel.deleteSong(uri) }
    }

    /** Comprueba una URI fuera del hilo principal; no modifica Room ni el archivo. */
    fun checkLibrarySongFile(uri: String) {
        viewModelScope.launch { fileCheckModel.check(uri) }
    }

    /** Reintenta Room; el query visible también se vuelve a suscribir. */
    fun retryLibraryLoad() {
        loadModel.retryIn(viewModelScope)
        listModel.refresh()
    }

    // ---------- Búsqueda y orden (FASE 8.3) ----------

    /**
     * Lógica de búsqueda/orden delegada a [LibraryListModel] (testeable en JVM
     * con scope propio: viewModelScope usa Dispatchers.Main, indisponible en
     * pruebas unitarias sin `kotlinx-coroutines-test`, NO autorizada).
     */
    private val listModel = LibraryListModel(songRepository)

    /** Consulta de búsqueda tal cual el usuario la escribe (el trim aplica al consultar). */
    val searchQuery: StateFlow<String> = listModel.query

    /** Orden del listado: ESTADO de UI sin persistencia (PLAN 8.3: sin DataStore en 8.x). */
    val sort: StateFlow<SongSort> = listModel.sort

    /** Lista visible (filtrada + ordenada) con su error asociado, si lo hubo. */
    val visibleSongs: StateFlow<LibraryListModel.VisibleSongs> = listModel.visibleSongs

    /** Error de la última búsqueda; null = sin error. */
    val searchError: StateFlow<String?> = listModel.error

    init {
        loadModel.observeIn(viewModelScope)
        viewModelScope.launch {
            loadModel.state.collect { state ->
                fileCheckModel.retainSongs(state.songs.mapTo(mutableSetOf()) { it.uri })
            }
        }
        listModel.startIn(viewModelScope)
    }

    /** Cambia la consulta; el debounce (500 ms) evita consultar por cada pulsación. */
    fun onSearchQueryChange(query: String) {
        listModel.onQueryChange(query)
    }

    /** Cambia el orden del listado (estado de UI; sin DataStore en FASE 8). */
    fun onSortChange(sort: SongSort) {
        listModel.onSortChange(sort)
    }

    /** Borra la búsqueda (vuelve al listado completo en el orden actual). */
    fun clearSearch() {
        listModel.clearQuery()
    }

    /**
     * Importa las URIs seleccionadas en el selector SAF. Nunca lanza: los
     * fallos por archivo están en el reporte; fallos de lote (no cubiertos)
     * se proyectan como ImportState.Failed.
     */
    fun importFiles(uriStrings: List<String>) {
        if (uriStrings.isEmpty()) return
        viewModelScope.launch { importModel.importFiles(uriStrings) }
    }

    /** Vuelve a Idle tras mostrar el resultado (caller: dismiss tras snackbar). */
    fun dismissImportResult() {
        importModel.dismissResult()
    }
}
