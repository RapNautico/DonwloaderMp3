package com.example.mimusica.data.repository

import com.example.mimusica.data.db.SongDao
import com.example.mimusica.data.db.SongEntity
import com.example.mimusica.data.db.toEntity
import com.example.mimusica.data.db.toSong
import com.example.mimusica.domain.model.Song
import com.example.mimusica.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementación Room del repositorio. Traduce modelos de Domain <-> Entities
 * y aísla a Domain del DAO (AGENTS.md §4).
 */
class SongRepositoryImpl(
    private val dao: SongDao,
) : SongRepository {

    override suspend fun addSongs(songs: List<Song>): Int =
        dao.insertAll(songs.map { it.toEntity() }).count { rowId -> rowId >= 0 }

    override suspend fun updateSong(song: Song) {
        dao.update(song.toEntity())
    }

    override suspend fun deleteSong(uri: String) {
        dao.deleteByUri(uri)
    }

    override suspend fun getSong(uri: String): Song? =
        dao.getByUri(uri)?.toSong()

    override suspend fun searchSongs(query: String): List<Song> =
        dao.search(query.trim()).map { it.toSong() }

    override fun observeLibrary(): Flow<List<Song>> =
        dao.observeAll().map { rows -> rows.map { it.toSong() } }
}
