package com.example.mimusica.presentation.library

import com.example.mimusica.domain.model.AlbumSummary
import com.example.mimusica.domain.model.ArtistSummary
import com.example.mimusica.domain.model.Song
import com.example.mimusica.domain.model.SongSort
import com.example.mimusica.domain.repository.SongRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LibraryLoadModelTest {

    private class FakeRepository : SongRepository {
        val library = MutableStateFlow<List<Song>>(emptyList())
        var failLibrary = false

        override fun observeLibrary(): Flow<List<Song>> = flow {
            if (failLibrary) throw IllegalStateException("library unavailable")
            library.collect { emit(it) }
        }

        override suspend fun addSongs(songs: List<Song>): Int = 0
        override suspend fun updateSong(song: Song) = Unit
        override suspend fun deleteSong(uri: String) = Unit
        override suspend fun getSong(uri: String): Song? = null
        override suspend fun searchSongs(query: String): List<Song> = emptyList()
        override fun observeLibrary(sortedBy: SongSort): Flow<List<Song>> = observeLibrary()
        override fun observeRecentSongs(limit: Int): Flow<List<Song>> = flowOf(emptyList())
        override suspend fun getSongsByArtist(artist: String?): List<Song> = emptyList()
        override suspend fun getSongsByAlbum(album: String?): List<Song> = emptyList()
        override fun observeArtistSummaries(): Flow<List<ArtistSummary>> = flowOf(emptyList())
        override fun observeAlbumSummaries(): Flow<List<AlbumSummary>> = flowOf(emptyList())
    }

    private lateinit var repository: FakeRepository
    private lateinit var model: LibraryLoadModel
    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        repository = FakeRepository()
        model = LibraryLoadModel(repository)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun load_error_is_not_reported_as_empty_success_and_can_retry() = runBlocking {
        repository.failLibrary = true
        model.observeIn(scope)
        waitUntil { model.state.value.error != null }

        assertTrue(model.state.value.songs.isEmpty())
        assertFalse(model.state.value.isLoading)
        assertEquals("library unavailable", model.state.value.error)

        repository.failLibrary = false
        repository.library.value = listOf(song())
        model.retryIn(scope)
        waitUntil { !model.state.value.isLoading && model.state.value.songs.isNotEmpty() }

        assertEquals(listOf(song()), model.state.value.songs)
        assertEquals(null, model.state.value.error)
    }

    @Test
    fun starts_loading_then_reports_an_actual_empty_library() = runBlocking {
        assertTrue(model.state.value.isLoading)
        assertTrue(model.state.value.songs.isEmpty())
        model.observeIn(scope)
        waitUntil { !model.state.value.isLoading }

        assertTrue(model.state.value.songs.isEmpty())
        assertEquals(null, model.state.value.error)
    }

    @Test
    fun retains_last_content_if_room_flow_fails_after_emitting() = runBlocking {
        repository.library.value = listOf(song())
        model.observeIn(scope)
        waitUntil { model.state.value.songs.isNotEmpty() }
        repository.failLibrary = true
        model.retryIn(scope)
        waitUntil { model.state.value.error != null }

        assertEquals(listOf(song()), model.state.value.songs)
        assertNotNull(model.state.value.error)
    }

    private suspend fun waitUntil(predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 2_000
        while (!predicate() && System.currentTimeMillis() < deadline) delay(5)
        assertTrue("condition not reached in time", predicate())
    }

    private fun song() = Song(
        uri = "content://music/one",
        title = "Song",
        artist = null,
        album = null,
        durationMs = 1_000,
        dateAddedMs = 1,
    )
}
