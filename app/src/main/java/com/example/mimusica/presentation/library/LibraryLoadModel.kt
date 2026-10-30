package com.example.mimusica.presentation.library

import com.example.mimusica.domain.model.Song
import com.example.mimusica.domain.model.SongSort
import com.example.mimusica.domain.repository.SongRepository
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Estado de carga inicial y recuperación de la biblioteca observada desde Room. */
data class LibraryLoadState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

class LibraryLoadModel(
    private val repository: SongRepository,
) {
    private val _state = MutableStateFlow(LibraryLoadState())
    val state: StateFlow<LibraryLoadState> = _state.asStateFlow()

    private val refreshRequests = MutableStateFlow(0)
    private var loadJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeIn(scope: CoroutineScope, sort: StateFlow<SongSort>) {
        loadJob?.cancel()
        loadJob = scope.launch {
            combine(sort, refreshRequests) { currentSort, _ -> currentSort }
                .flatMapLatest { currentSort ->
                    _state.value = _state.value.copy(isLoading = true, error = null)
                    repository.observeLibrary(currentSort)
                        .map { songs -> LibraryLoadState(songs = songs, isLoading = false) }
                        .catch { failure ->
                            if (failure is CancellationException) throw failure
                            emit(
                                _state.value.copy(
                                    isLoading = false,
                                    error = failure.message ?: failure::class.simpleName ?: "Error",
                                ),
                            )
                        }
                }
                .collect { state -> _state.value = state }
        }
    }

    fun retry() {
        refreshRequests.value += 1
    }
}
