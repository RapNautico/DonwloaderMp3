package com.example.mimusica.data.repository

import com.example.mimusica.data.db.PlaylistDao
import com.example.mimusica.data.db.PlaylistEntity
import com.example.mimusica.data.db.PlaylistSongRow
import com.example.mimusica.data.db.toSong
import com.example.mimusica.domain.model.PlaylistDetails
import com.example.mimusica.domain.model.PlaylistSummary
import com.example.mimusica.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class PlaylistRepositoryImpl(
    private val dao: PlaylistDao,
) : PlaylistRepository {
    override fun observePlaylists(): Flow<List<PlaylistSummary>> = dao.observePlaylists()

    override fun observePlaylist(playlistId: Long): Flow<PlaylistDetails?> =
        combine(
            dao.observePlaylist(playlistId),
            dao.observeSongs(playlistId),
        ) { playlist, songs ->
            playlist?.let {
                PlaylistDetails(
                    playlist = it.toSummary(songs.size, songs.firstOrNull()?.song?.artworkPath),
                    songs = songs.map { it.toSong() },
                )
            }
        }

    override suspend fun createPlaylist(name: String): Long =
        dao.insertPlaylist(PlaylistEntity(name = name.requireValidName()))

    override suspend fun renamePlaylist(playlistId: Long, name: String) {
        dao.renamePlaylist(playlistId, name.requireValidName())
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        dao.deletePlaylist(playlistId)
    }

    override suspend fun addSong(playlistId: Long, songUri: String): Boolean =
        dao.appendSong(playlistId, songUri) >= 0

    override suspend fun removeSong(playlistId: Long, songUri: String) {
        dao.removeSong(playlistId, songUri)
    }

    override suspend fun reorderSongs(playlistId: Long, orderedSongUris: List<String>) {
        dao.reorderSongs(playlistId, orderedSongUris)
    }

    private fun PlaylistEntity.toSummary(songCount: Int, artworkPath: String?) =
        PlaylistSummary(id = id, name = name, songCount = songCount, artworkPath = artworkPath)

    private fun PlaylistSongRow.toSong() = song.toSong()

    private fun String.requireValidName(): String =
        trim().also { require(it.isNotEmpty()) { "El nombre de la playlist no puede estar vacío." } }
}
