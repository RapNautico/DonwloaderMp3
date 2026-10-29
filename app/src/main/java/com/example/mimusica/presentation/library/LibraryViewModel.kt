package com.example.mimusica.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mimusica.domain.importing.ImportReport
import com.example.mimusica.domain.model.Song
import com.example.mimusica.domain.model.SongSort
import com.example.mimusica.domain.model.toComparator
import com.example.mimusica.domain.repository.SongRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
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

    // ---------- Búsqueda y orden (FASE 8.3) ----------

    /** Consulta de búsqueda tal cual la escribe el usuario (el trim aplica al consultar). */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** Orden del listado: ESTADO de UI sin persistencia (PLAN 8.3: sin DataStore en 8.x). */
    private val _sort = MutableStateFlow(SongSort.TITLE_ASC)
    val sort: StateFlow<SongSort> = _sort.asStateFlow()

    /** Lista visible = biblioteca filtrada por búsqueda y con el orden actual. */
    private val _visibleSongs = MutableStateFlow<List<Song>>(emptyList())
    val visibleSongs: StateFlow<List<Song>> = _visibleSongs.asStateFlow()

    init {
        viewModelScope.launch {
            songRepository.observeLibrary().collect { list ->
                songs.value = list
                _isLoading.value = false
            }
        }
        observeVisibleSongs()
    }

    /**
     * Pipeline reactivo de la lista visible (FASE 8.3):
     * - query + sort con DEBOUNCE 300 ms (DESIGN_SYSTEM §6.3): no se consulta
     *   por cada pulsación; flatMapLatest cancela el cálculo anterior.
     * - Sin consulta (o solo espacios): biblioteca completa ordenada EN SQL
     *   (observeLibrary(sort), flujo reactivo a importaciones);
     *   jamás re-ordenada en memoria completa.
     * - Con consulta: searchSongs (SQL, subconjunto filtrado) ordenado en
     *   memoria con el MISMO comparador que el ORDER BY de SQL
     *   (SongSort.toComparator, testeado): ordenar un subconjunto pequeño es
     *   correcto; la biblioteca entera nunca se re-ordena aquí.
     */
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun observeVisibleSongs() {
        viewModelScope.launch {
            combine(_searchQuery, _sort) { query, sort -> query.trim() to sort }
                .debounce(SEARCH_DEBOUNCE_MS)
                .flatMapLatest { (trimmed, sort) ->
                    if (trimmed.isEmpty()) {
                        songRepository.observeLibrary(sort)
                    } else {
                        flow {
                            emit(
                                songRepository
                                    .searchSongs(trimmed)
                                    .sortedWith(sort.toComparator()),
                            )
                        }
                    }
                }
                .collect { lista -> _visibleSongs.value = lista }
        }
    }

    /** Cambia la consulta; el debounce (300 ms) evita consultar por cada pulsación. */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    /** Cambia el orden del listado (estado de UI; sin DataStore en FASE 8). */
    fun onSortChange(sort: SongSort) {
        _sort.value = sort
    }

    /** Borra la búsqueda (vuelve al listado completo en el orden actual). */
    fun clearSearch() {
        _searchQuery.value = ""
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

    /** Vuelve a Idle tras mostrar el resultado (caller: dismiss tras snackbar). */
    fun dismissImportResult() {
        _importState.update { ImportState.Idle }
    }

    companion object {
        /** Debounce de búsqueda (DESIGN_SYSTEM §6.3: 300 ms). */
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
