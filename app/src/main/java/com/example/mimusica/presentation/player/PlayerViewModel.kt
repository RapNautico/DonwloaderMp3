package com.example.mimusica.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mimusica.domain.model.PlaybackTrack
import com.example.mimusica.domain.model.PlaybackRepeatMode
import com.example.mimusica.domain.model.Song
import com.example.mimusica.domain.player.PlayerController
import com.example.mimusica.domain.player.PlaybackQueuePolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerViewModel(
    private val playerController: PlayerController,
) : ViewModel() {
    val state = playerController.state

    fun playSongs(songs: List<Song>, selectedUri: String) {
        viewModelScope.launch {
            val queue = withContext(Dispatchers.Default) {
                songs.map { song ->
                    PlaybackTrack(
                        uri = song.uri,
                        title = song.title,
                        artist = song.artist,
                        artworkPath = song.artworkPath,
                    )
                }
            }
            val startIndex = queue.indexOfFirst { it.uri == selectedUri }
            playerController.playQueue(queue, startIndex)
        }
    }

    fun playTrackAt(index: Int) {
        val currentQueue = state.value.queue
        viewModelScope.launch {
            playerController.playQueue(currentQueue, index)
        }
    }

    fun togglePlayPause() = dispatch(playerController::togglePlayPause)

    fun skipToNext() = dispatch(playerController::skipToNext)

    fun skipToPrevious() = dispatch(playerController::skipToPrevious)

    fun seekTo(positionMs: Long) = dispatch { playerController.seekTo(positionMs) }

    fun seekBy(offsetMs: Long) = dispatch { playerController.seekBy(offsetMs) }

    fun setVolume(volume: Float) = dispatch { playerController.setVolume(volume) }

    fun cycleRepeatMode() {
        val nextMode = PlaybackQueuePolicy.nextRepeatMode(state.value.repeatMode)
        dispatch { playerController.setRepeatMode(nextMode) }
    }

    fun toggleShuffle() {
        val enabled = !state.value.shuffleEnabled
        dispatch { playerController.setShuffleEnabled(enabled) }
    }

    fun stop() = dispatch(playerController::stop)

    private fun dispatch(command: suspend () -> Unit) {
        viewModelScope.launch { command() }
    }
}
