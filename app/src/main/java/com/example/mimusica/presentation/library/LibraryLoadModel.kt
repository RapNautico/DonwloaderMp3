package com.example.mimusica.presentation.library

import com.example.mimusica.domain.model.Song
import com.example.mimusica.domain.repository.SongRepository
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
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

    private var loadJob: Job? = null

    fun observeIn(scope: CoroutineScope) {
        loadIn(scope)
    }

    fun retryIn(scope: CoroutineScope) {
        loadIn(scope)
    }

    private fun loadIn(scope: CoroutineScope) {
        loadJob?.cancel()
        _state.update { it.copy(isLoading = true, error = null) }
        loadJob = scope.launch {
            repository.observeLibrary()
                .catch { failure ->
                    if (failure is CancellationException) throw failure
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = failure.message ?: failure::class.simpleName ?: "Error",
                        )
                    }
                }
                .collect { songs ->
                    _state.value = LibraryLoadState(songs = songs, isLoading = false)
                }
        }
    }
}
