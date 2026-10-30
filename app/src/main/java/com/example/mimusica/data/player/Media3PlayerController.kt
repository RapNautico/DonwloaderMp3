package com.example.mimusica.data.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.mimusica.domain.model.PlaybackState
import com.example.mimusica.domain.model.PlaybackRepeatMode
import com.example.mimusica.domain.model.PlaybackStatus
import com.example.mimusica.domain.model.PlaybackTrack
import com.example.mimusica.domain.player.PlayerController
import com.example.mimusica.domain.player.PlaybackQueuePolicy
import com.example.mimusica.domain.repository.AppSettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ExecutionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class Media3PlayerController(
    context: Context,
    private val appSettingsRepository: AppSettingsRepository? = null,
) : PlayerController {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val connectionMutex = Mutex()
    private val mutableState = MutableStateFlow(PlaybackState())
    private var mediaController: MediaController? = null
    private var positionJob: Job? = null
    private var currentQueue: List<PlaybackTrack> = emptyList()

    override val state: StateFlow<PlaybackState> = mutableState.asStateFlow()

    init {
        scope.launch {
            try {
                getController()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                mutableState.value = mutableState.value.copy(
                    status = PlaybackStatus.ERROR,
                    error = failure.message ?: "No se pudo conectar con el reproductor.",
                )
            }
        }
    }

    override suspend fun playQueue(queue: List<PlaybackTrack>, startIndex: Int) {
        if (queue.isEmpty() || startIndex !in queue.indices) {
            mutableState.value = mutableState.value.copy(
                status = PlaybackStatus.ERROR,
                error = "La cola de reproducción no es válida.",
            )
            return
        }
        withController { controller ->
            val items = withContext(Dispatchers.Default) { queue.map(::toMediaItem) }
            if (controller.mediaItemCount == 0) {
                appSettingsRepository?.settings?.value?.let { settings ->
                    controller.repeatMode = settings.defaultRepeatMode.toMedia3RepeatMode()
                    controller.shuffleModeEnabled = settings.defaultShuffle
                }
            }
            controller.setMediaItems(items, startIndex, 0L)
            controller.prepare()
            controller.play()
            publishState(controller, refreshQueue = true)
        }
    }

    override suspend fun togglePlayPause() = withController { controller ->
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.playbackState == Player.STATE_ENDED) {
                controller.seekToDefaultPosition()
            }
            controller.play()
        }
        publishState(controller)
    }

    override suspend fun skipToNext() = withController { controller ->
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        }
        publishState(controller, refreshQueue = true)
    }

    override suspend fun skipToPrevious() = withController { controller ->
        if (PlaybackQueuePolicy.shouldRestartCurrent(controller.currentPosition)) {
            controller.seekTo(0L)
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
        }
        publishState(controller, refreshQueue = true)
    }

    override suspend fun seekTo(positionMs: Long) = withController { controller ->
        controller.seekTo(positionMs.coerceAtLeast(0L))
        publishState(controller)
    }

    override suspend fun seekBy(offsetMs: Long) = withController { controller ->
        val duration = controller.duration.takeIf { it != C.TIME_UNSET } ?: 0L
        controller.seekTo(
            PlaybackQueuePolicy.seekTarget(controller.currentPosition, offsetMs, duration),
        )
        publishState(controller)
    }

    override suspend fun setVolume(volume: Float) = withController { controller ->
        controller.volume = volume.coerceIn(0f, 1f)
        publishState(controller)
    }

    override suspend fun setRepeatMode(mode: PlaybackRepeatMode) = withController { controller ->
        controller.repeatMode = mode.toMedia3RepeatMode()
        publishState(controller)
    }

    override suspend fun setShuffleEnabled(enabled: Boolean) = withController { controller ->
        controller.shuffleModeEnabled = enabled
        publishState(controller)
    }

    override suspend fun stop() = withController { controller ->
        controller.stop()
        controller.clearMediaItems()
        currentQueue = emptyList()
        mutableState.value = PlaybackState(
            volume = controller.volume,
            repeatMode = controller.repeatMode.toPlaybackRepeatMode(),
            shuffleEnabled = controller.shuffleModeEnabled,
        )
    }

    fun release() {
        positionJob?.cancel()
        mediaController?.removeListener(playerListener)
        mediaController?.release()
        mediaController = null
        scope.coroutineContext[Job]?.cancel()
    }

    private suspend fun withController(action: suspend (MediaController) -> Unit) {
        executePlaybackCommand(
            command = {
                withContext(Dispatchers.Main.immediate) {
                    action(getController())
                }
            },
            onFailure = { failure ->
                mutableState.value = mutableState.value.copy(
                    status = PlaybackStatus.ERROR,
                    isPlaying = false,
                    error = failure.message ?: "No se pudo controlar la reproducción.",
                )
            },
        )
    }

    private suspend fun getController(): MediaController {
        mediaController?.let { return it }
        return connectionMutex.withLock {
            mediaController?.let { return@withLock it }
            val token = SessionToken(
                appContext,
                ComponentName(appContext, MediaPlaybackService::class.java),
            )
            val connected = suspendCancellableCoroutine { continuation ->
                val future = MediaController.Builder(appContext, token).buildAsync()
                continuation.invokeOnCancellation { future.cancel(false) }
                future.addListener(
                    {
                        try {
                            val created = future.get()
                            if (continuation.isActive) {
                                continuation.resume(created) { _, cancelledController, _ ->
                                    cancelledController.release()
                                }
                            } else {
                                created.release()
                            }
                        } catch (failure: Exception) {
                            val cause = (failure as? ExecutionException)?.cause ?: failure
                            if (continuation.isActive) {
                                continuation.resumeWithException(cause)
                            }
                        }
                    },
                    appContext.mainExecutor,
                )
            }
            mediaController = connected
            connected.addListener(playerListener)
            positionJob = scope.launch {
                while (true) {
                    publishState(connected, refreshQueue = true)
                    delay(POSITION_UPDATE_INTERVAL_MS)
                }
            }
            publishState(connected)
            connected
        }
    }

    private fun publishState(controller: Player, refreshQueue: Boolean = false) {
        if (refreshQueue) {
            currentQueue = (0 until controller.mediaItemCount).map { index ->
                controller.getMediaItemAt(index).toPlaybackTrack()
            }
        }
        val queue = currentQueue
        val currentIndex = controller.currentMediaItemIndex.takeIf { it in queue.indices } ?: -1
        val duration = controller.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L
        val failure = controller.playerError
        val status = when {
            failure != null -> PlaybackStatus.ERROR
            controller.playbackState == Player.STATE_BUFFERING -> PlaybackStatus.BUFFERING
            controller.playbackState == Player.STATE_ENDED -> PlaybackStatus.ENDED
            controller.playbackState == Player.STATE_READY -> PlaybackStatus.READY
            else -> PlaybackStatus.IDLE
        }
        mutableState.value = PlaybackState(
            queue = queue,
            currentIndex = currentIndex,
            positionMs = controller.currentPosition.coerceAtLeast(0L),
            durationMs = duration,
            status = status,
            isPlaying = controller.isPlaying,
            volume = controller.volume,
            error = failure?.message,
            repeatMode = controller.repeatMode.toPlaybackRepeatMode(),
            shuffleEnabled = controller.shuffleModeEnabled,
            canSkipNext = controller.hasNextMediaItem(),
            canSkipPrevious = controller.hasPreviousMediaItem() ||
                PlaybackQueuePolicy.shouldRestartCurrent(controller.currentPosition),
        )
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publishState(player, refreshQueue = true)
        }
    }

    private fun toMediaItem(track: PlaybackTrack): MediaItem =
        MediaItem.Builder()
            .setMediaId(track.uri)
            .setUri(Uri.parse(track.uri))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setArtworkUri(track.artworkPath?.let { Uri.fromFile(File(it)) })
                    .build(),
            )
            .build()

    private fun MediaItem.toPlaybackTrack(): PlaybackTrack {
        val artworkUri = mediaMetadata.artworkUri
        return PlaybackTrack(
            uri = localConfiguration?.uri?.toString() ?: mediaId,
            title = mediaMetadata.title?.toString().orEmpty(),
            artist = mediaMetadata.artist?.toString(),
            artworkPath = artworkUri?.takeIf { it.scheme == "file" }?.path,
        )
    }

    private companion object {
        const val POSITION_UPDATE_INTERVAL_MS = 500L
    }
}
