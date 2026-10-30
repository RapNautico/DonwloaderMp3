package com.example.mimusica.presentation.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mimusica.domain.model.PlaylistSummary
import com.example.mimusica.domain.repository.PlaylistRepository
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

data class PlaylistsUiState(
    val playlists: List<PlaylistSummary> = emptyList(),
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val operationError: String? = null,
)

class PlaylistsViewModel(
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(PlaylistsUiState())
    val state: StateFlow<PlaylistsUiState> = mutableState.asStateFlow()
    private var observeJob: Job? = null

    init {
        observePlaylists()
    }

    fun retry() {
        observeJob?.cancel()
        mutableState.value = mutableState.value.copy(isLoading = true, loadError = null)
        observePlaylists()
    }

    private fun observePlaylists() {
        observeJob = viewModelScope.launch {
            playlistRepository.observePlaylists()
                .catch { failure ->
                    if (failure is CancellationException) throw failure
                    mutableState.value = mutableState.value.copy(
                        isLoading = false,
                        loadError = failure.message ?: "No se pudieron cargar las playlists.",
                    )
                }
                .collect { playlists ->
                    mutableState.value = mutableState.value.copy(
                        playlists = playlists,
                        isLoading = false,
                        loadError = null,
                    )
                }
        }
    }

    fun createPlaylist(name: String) = runOperation {
        playlistRepository.createPlaylist(name)
    }

    fun renamePlaylist(playlistId: Long, name: String) = runOperation {
        playlistRepository.renamePlaylist(playlistId, name)
    }

    fun deletePlaylist(playlistId: Long) = runOperation {
        playlistRepository.deletePlaylist(playlistId)
    }

    fun addSong(playlistId: Long, songUri: String) = runOperation {
        playlistRepository.addSong(playlistId, songUri)
    }

    private fun <T> runOperation(operation: suspend () -> T) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(operationError = null)
            try {
                operation()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                mutableState.value = mutableState.value.copy(
                    operationError = failure.message ?: "No se pudo completar la operación.",
                )
            }
        }
    }
}

data class PlaylistDetailUiState(
    val playlist: com.example.mimusica.domain.model.PlaylistDetails? = null,
    val librarySongs: List<com.example.mimusica.domain.model.Song> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val operationError: String? = null,
)

class PlaylistDetailViewModel(
    private val playlistRepository: PlaylistRepository,
    songRepository: com.example.mimusica.domain.repository.SongRepository,
    private val playlistId: Long,
) : ViewModel() {
    private val mutableState = MutableStateFlow(PlaylistDetailUiState())
    val state: StateFlow<PlaylistDetailUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                playlistRepository.observePlaylist(playlistId).collect { playlist ->
                    mutableState.value = mutableState.value.copy(
                        playlist = playlist,
                        isLoading = false,
                        error = null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                mutableState.value = mutableState.value.copy(
                    isLoading = false,
                    error = failure.message ?: "No se pudo cargar la playlist.",
                )
            }
        }
        viewModelScope.launch {
            try {
                songRepository.observeLibrary().collect { songs ->
                    mutableState.value = mutableState.value.copy(librarySongs = songs)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                mutableState.value = mutableState.value.copy(
                    error = failure.message ?: "No se pudo cargar la biblioteca.",
                )
            }
        }
    }

    fun addSong(uri: String) = runOperation {
        playlistRepository.addSong(playlistId, uri)
    }

    fun removeSong(uri: String) = runOperation {
        playlistRepository.removeSong(playlistId, uri)
    }

    fun moveSong(fromIndex: Int, toIndex: Int) = runOperation {
        val songs = mutableState.value.playlist?.songs.orEmpty()
        if (fromIndex !in songs.indices || toIndex !in songs.indices) return@runOperation
        val reordered = songs.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        playlistRepository.reorderSongs(playlistId, reordered.map { it.uri })
    }

    private fun <T> runOperation(operation: suspend () -> T) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(operationError = null)
            try {
                operation()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                mutableState.value = mutableState.value.copy(
                    operationError = failure.message ?: "No se pudo completar la operación.",
                )
            }
        }
    }
}
