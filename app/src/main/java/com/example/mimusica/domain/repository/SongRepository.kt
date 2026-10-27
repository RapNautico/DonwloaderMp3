package com.example.mimusica.domain.repository

import com.example.mimusica.domain.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * Abstracción de la biblioteca en Domain. Los consumidores (ViewModels)
 * no conocen Room ni el DAO (STORY "Presentation → Domain → Data").
 */
interface SongRepository {
    /** Inserción idempotente por URI del archivo (mismo archivo ≠ duplicado). */
    suspend fun addSongs(songs: List<Song>)

    suspend fun updateSong(song: Song)

    suspend fun deleteSong(uri: String)

    suspend fun getSong(uri: String): Song?

    /** Búsqueda (case-insensitive) por título o artista. */
    suspend fun searchSongs(query: String): List<Song>

    /** Observación reactiva de la biblioteca ordenada por título. */
    fun observeLibrary(): Flow<List<Song>>
}
