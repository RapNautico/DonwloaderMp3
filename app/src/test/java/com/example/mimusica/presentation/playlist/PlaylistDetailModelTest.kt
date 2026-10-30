package com.example.mimusica.presentation.playlist

import com.example.mimusica.domain.model.AlbumSummary
import com.example.mimusica.domain.model.ArtistSummary
import com.example.mimusica.domain.model.PlaylistDetails
import com.example.mimusica.domain.model.PlaylistSummary
import com.example.mimusica.domain.model.Song
import com.example.mimusica.domain.model.SongSort
import com.example.mimusica.domain.repository.PlaylistRepository
import com.example.mimusica.domain.repository.SongRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class PlaylistDetailModelTest {
    @Test
    fun `retry reinicia las observaciones de playlist y biblioteca despues de un error`() = runBlocking {
        val song = Song(
            uri = "preview://song",
            title = "Canción de prueba",
            artist = null,
            album = null,
            durationMs = 1_000L,
            dateAddedMs = 1L,
        )
        val details = PlaylistDetails(
            playlist = PlaylistSummary(1L, "Playlist de prueba", 1, null),
            songs = listOf(song),
        )
        val playlistAttempts = AtomicInteger()
        val libraryAttempts = AtomicInteger()
        val playlistRepository = object : PlaylistRepository {
            override fun observePlaylists(): Flow<List<PlaylistSummary>> = flowOf(emptyList())

            override fun observePlaylist(playlistId: Long): Flow<PlaylistDetails?> = flow {
                if (playlistAttempts.getAndIncrement() == 0) {
                    throw IllegalStateException("Error temporal de playlist")
                }
                emit(details)
            }

            override suspend fun createPlaylist(name: String) = 1L
            override suspend fun renamePlaylist(playlistId: Long, name: String) = Unit
            override suspend fun deletePlaylist(playlistId: Long) = Unit
            override suspend fun addSong(playlistId: Long, songUri: String) = true
            override suspend fun removeSong(playlistId: Long, songUri: String) = Unit
            override suspend fun reorderSongs(playlistId: Long, orderedSongUris: List<String>) = Unit
        }
        val songRepository = object : SongRepository {
            override suspend fun addSongs(songs: List<Song>) = 0
            override suspend fun updateSong(song: Song) = Unit
            override suspend fun deleteSong(uri: String) = Unit
            override suspend fun getSong(uri: String): Song? = null
            override suspend fun searchSongs(query: String): List<Song> = emptyList()
            override fun observeLibrary(): Flow<List<Song>> = flow {
                if (libraryAttempts.getAndIncrement() == 0) {
                    throw IllegalStateException("Error temporal de biblioteca")
                }
                emit(listOf(song))
            }
            override fun observeLibrary(sortedBy: SongSort): Flow<List<Song>> = observeLibrary()
            override fun observeRecentSongs(limit: Int): Flow<List<Song>> = flowOf(emptyList())
            override suspend fun getSongsByArtist(artist: String?): List<Song> = emptyList()
            override suspend fun getSongsByAlbum(album: String?): List<Song> = emptyList()
            override fun observeArtistSummaries(): Flow<List<ArtistSummary>> = flowOf(emptyList())
            override fun observeAlbumSummaries(): Flow<List<AlbumSummary>> = flowOf(emptyList())
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val model = PlaylistDetailModel(playlistRepository, songRepository, playlistId = 1L)

        try {
            model.startIn(scope)
            awaitState(predicate = {
                model.state.value.error != null && !model.state.value.isLoading
            })
            assertEquals(1, playlistAttempts.get())
            assertEquals(1, libraryAttempts.get())

            model.retryIn(scope)
            awaitState(predicate = {
                model.state.value.playlist == details &&
                    model.state.value.librarySongs == listOf(song) &&
                    model.state.value.error == null
            }, diagnostic = {
                "state=${model.state.value}; playlistAttempts=${playlistAttempts.get()}; " +
                    "libraryAttempts=${libraryAttempts.get()}"
            })

            assertEquals(2, playlistAttempts.get())
            assertEquals(2, libraryAttempts.get())
            assertFalse(model.state.value.isLoading)
            assertNull(model.state.value.error)
        } finally {
            scope.cancel()
        }
    }

    private suspend fun awaitState(
        predicate: () -> Boolean,
        diagnostic: () -> String = { "" },
    ) {
        try {
            withTimeout(2_000L) {
                while (!predicate()) {
                    delay(10L)
                }
            }
        } catch (timeout: kotlinx.coroutines.TimeoutCancellationException) {
            throw AssertionError("Timed out waiting for state. ${diagnostic()}", timeout)
        }
    }
}
