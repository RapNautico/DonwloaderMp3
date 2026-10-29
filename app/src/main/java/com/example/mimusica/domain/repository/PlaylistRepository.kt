package com.example.mimusica.domain.repository

import com.example.mimusica.domain.model.PlaylistDetails
import com.example.mimusica.domain.model.PlaylistSummary
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun observePlaylists(): Flow<List<PlaylistSummary>>

    fun observePlaylist(playlistId: Long): Flow<PlaylistDetails?>

    suspend fun createPlaylist(name: String): Long

    suspend fun renamePlaylist(playlistId: Long, name: String)

    suspend fun deletePlaylist(playlistId: Long)

    suspend fun addSong(playlistId: Long, songUri: String): Boolean

    suspend fun removeSong(playlistId: Long, songUri: String)

    suspend fun reorderSongs(playlistId: Long, orderedSongUris: List<String>)
}
