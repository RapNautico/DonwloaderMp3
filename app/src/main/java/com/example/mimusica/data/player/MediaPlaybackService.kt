package com.example.mimusica.data.player

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.mimusica.domain.model.PlaybackTrack
import java.io.File

class MediaPlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private lateinit var snapshotStore: PlaybackSnapshotStore
    private val persistenceHandler = Handler(Looper.getMainLooper())
    private val persistPosition = object : Runnable {
        override fun run() {
            if (player.isPlaying) {
                persistSnapshot()
                persistenceHandler.postDelayed(this, POSITION_SAVE_INTERVAL_MS)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        snapshotStore = PlaybackSnapshotStore(this)
        player = ExoPlayer.Builder(applicationContext).build().apply {
            setAudioAttributes(AudioAttributes.DEFAULT, true)
            setHandleAudioBecomingNoisy(true)
            addListener(playerListener)
        }
        mediaSession = MediaSession.Builder(this, player).build()
        restoreSnapshot()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (!player.isPlaying) stopSelf()
    }

    override fun onDestroy() {
        persistenceHandler.removeCallbacks(persistPosition)
        persistSnapshot()
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            persistSnapshot()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            persistenceHandler.removeCallbacks(persistPosition)
            if (isPlaying) {
                persistenceHandler.postDelayed(persistPosition, POSITION_SAVE_INTERVAL_MS)
            }
            persistSnapshot()
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            persistSnapshot()
        }
    }

    private fun restoreSnapshot() {
        val snapshot = snapshotStore.read() ?: return
        val items = snapshot.queue.map(::toMediaItem)
        player.setMediaItems(items, snapshot.currentIndex, snapshot.positionMs)
        player.prepare()
        player.playWhenReady = snapshot.playWhenReady
    }

    private fun persistSnapshot() {
        if (!::player.isInitialized || !::snapshotStore.isInitialized) return
        val queue = (0 until player.mediaItemCount)
            .map(player::getMediaItemAt)
            .map(::toPlaybackTrack)
        if (queue.isEmpty()) {
            snapshotStore.clear()
            return
        }
        val index = player.currentMediaItemIndex.takeIf { it in queue.indices } ?: 0
        snapshotStore.write(
            PlaybackSnapshot(
                queue = queue,
                currentIndex = index,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                playWhenReady = player.playWhenReady,
            ),
        )
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

    private fun toPlaybackTrack(item: MediaItem): PlaybackTrack {
        val metadata = item.mediaMetadata
        val artworkUri = metadata.artworkUri
        return PlaybackTrack(
            uri = item.localConfiguration?.uri?.toString() ?: item.mediaId,
            title = metadata.title?.toString().orEmpty(),
            artist = metadata.artist?.toString(),
            artworkPath = artworkUri?.takeIf { it.scheme == "file" }?.path,
        )
    }

    private companion object {
        const val POSITION_SAVE_INTERVAL_MS = 1_000L
    }
}
