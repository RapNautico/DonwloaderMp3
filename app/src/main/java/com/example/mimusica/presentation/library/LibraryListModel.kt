package com.example.mimusica.presentation.library

import com.example.mimusica.domain.model.Song
import com.example.mimusica.domain.model.SongSort
import com.example.mimusica.domain.model.toComparator
import com.example.mimusica.domain.repository.SongRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Lógica PRESENTACIÓN de la lista visible de la biblioteca (FASE 8.3).
 *
 * Separada del ViewModel para poder testearla en JVM con un scope propio
 * (viewModelScope usa Dispatchers.Main, indisponible en pruebas unitarias sin
 * `kotlinx-coroutines-test`, dependencia NO autorizada).
 *
 * Comportamiento:
 * - Búsqueda reactiva con DEBOUNCE 500 ms (decisión del usuario 2026-10-29,
 *   sustituye los 300 ms del wireframe DESIGN_SYSTEM §6.3): no se consulta por
 *   cada pulsación.
 * - `flatMapLatest` CANCELA la búsqueda anterior: los cambios rápidos de
 *   consulta nunca muestran resultados obsoletos (solo gana la última).
 * - Sin consulta (o solo espacios): biblioteca completa ordenada EN SQL
 *   (SongRepository.observeLibrary(sort)), reactivo a importaciones;
 *   jamás re-ordenada en memoria completa.
 * - Con consulta: SongRepository.searchSongs (SQL, subconjunto filtrado)
 *   re-ordenado en memoria con el MISMO comparador que el ORDER BY SQL
 *   (SongSort.toComparator, paridad asertada en pruebas de 8.2/8.3).
 * - ERRORES: una consulta que falla NO crashea ni borra la lista; fija
 *   [error] (la UI lo muestra); el siguiente resultado exitoso lo limpia.
 */
class LibraryListModel(
    private val songRepository: SongRepository,
    /** Debounce de búsqueda en ms (real; el ViewModel le pasa el de producción). */
    val searchDebounceMs: Long = SEARCH_DEBOUNCE_MS,
) {

    data class VisibleSongs(
        val songs: List<Song>,
        val error: String?,
        val query: String = "",
        val isLoading: Boolean = true,
    )

    /** Consulta tal cual la escribe el usuario (el trim aplica al consultar). */
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Orden del listado: estado de UI sin persistencia (PLAN 8.3: sin DataStore en FASE 8). */
    private val _sort = MutableStateFlow(SongSort.TITLE_ASC)
    val sort: StateFlow<SongSort> = _sort.asStateFlow()

    /** Lista visible = búsqueda filtrada + orden actual. */
    private val _visibleSongs = MutableStateFlow(VisibleSongs(emptyList(), null))
    val visibleSongs: StateFlow<VisibleSongs> = _visibleSongs.asStateFlow()

    /** Error de la ÚLTIMA operación de búsqueda; null = sin error (se limpia con cada éxito). */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val refreshRequests = MutableStateFlow(0)

    fun startIn(scope: CoroutineScope, libraryState: StateFlow<LibraryLoadState>? = null) {
        pipeline(scope, libraryState)
    }

    fun onQueryChange(query: String) {
        _query.value = query
    }

    fun onSortChange(sort: SongSort) {
        _sort.value = sort
    }

    fun clearQuery() {
        _query.value = ""
    }

    /** Reinicia la consulta activa, por ejemplo tras recuperar un fallo de Room. */
    fun refresh() {
        refreshRequests.update { it + 1 }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun pipeline(
        scope: CoroutineScope,
        libraryState: StateFlow<LibraryLoadState>?,
    ) {
        combine(_query, _sort, refreshRequests) { query, sort, _ -> query.trim() to sort }
            .debounce(searchDebounceMs)
            .flatMapLatest { (trimmed, sort) ->
                kotlinx.coroutines.flow.flow {
                    try {
                        if (trimmed.isEmpty()) {
                            if (libraryState != null) {
                                emitAll(
                                    libraryState.map { state ->
                                        VisibleSongs(
                                            songs = state.songs,
                                            error = state.error,
                                            query = trimmed,
                                            isLoading = state.isLoading,
                                        )
                                    },
                                )
                            } else {
                                emitAll(
                                    songRepository.observeLibrary(sort).map { lista ->
                                        VisibleSongs(lista, null, trimmed, isLoading = false)
                                    },
                                )
                            }
                        } else {
                            val lista = songRepository.searchSongs(trimmed)
                                .sortedWith(sort.toComparator())
                            emit(VisibleSongs(lista, null, trimmed, isLoading = false))
                        }
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        emit(
                            VisibleSongs(
                                songs = _visibleSongs.value.songs,
                                error = failure.message ?: failure::class.simpleName ?: "Error",
                                query = trimmed,
                                isLoading = false,
                            ),
                        )
                    }
                }
            }
            .onEach { resultado ->
                _visibleSongs.value = resultado
                _error.value = resultado.error
            }
            .launchIn(scope)
    }

    companion object {
        /** Debounce de búsqueda PRODUCCIÓN (decisión usuario 2026-10-29): 500 ms. */
        const val SEARCH_DEBOUNCE_MS = 500L
    }
}
