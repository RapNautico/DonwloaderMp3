package com.example.mimusica.domain.repository

import com.example.mimusica.domain.model.AlbumSummary
import com.example.mimusica.domain.model.ArtistSummary
import com.example.mimusica.domain.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * Abstracción de la biblioteca en Domain. Los consumidores (ViewModels)
 * no conocen Room ni el DAO (STORY "Presentation → Domain → Data").
 *
 * FASE 8.2: consultas de la biblioteca completa. El filtrado/orden/agrupado
 * se resuelve EN LA BASE DE DATOS (nunca se re-filtra una lista completa en
 * memoria desde la UI).
 */
interface SongRepository {
    /**
     * Inserción idempotente por URI del archivo (mismo archivo ≠ duplicado).
     * Devuelve el número de canciones NUEVAS insertadas (las ya existentes
     * no se modifican ni cuentan).
     */
    suspend fun addSongs(songs: List<Song>): Int

    suspend fun updateSong(song: Song)

    suspend fun deleteSong(uri: String)

    suspend fun getSong(uri: String): Song?

    /**
     * Búsqueda parcial e insensibilidad de mayúsculas por TÍTULO, ARTISTA y
     * ÁLBUM (case-insensitive en el comportamiento real de SQLite: ASCII).
     * La consulta se recorta por los lados; búsqueda vacía = biblioteca
     * completa; sin resultados devuelve una lista vacía (estado explícito).
     * Política de comodines documentada: `%` y `_` se tratan como TEXTO LITERAL.
     */
    suspend fun searchSongs(query: String): List<Song>

    /** Observación reactiva de la biblioteca ordenada por título (A→Z, desempate por uri). */
    fun observeLibrary(): Flow<List<Song>>

    /** Observación reactiva de las canciones RECIENTES por dateAddedMs (más nuevas primero). */
    fun observeRecentSongs(limit: Int): Flow<List<Song>>

    /** Canciones de un artista. El `null` agrupa las canciones SIN artista (desconocido). */
    suspend fun getSongsByArtist(artist: String?): List<Song>

    /** Canciones de un álbum. El `null` agrupa las canciones SIN álbum (desconocido). */
    suspend fun getSongsByAlbum(album: String?): List<Song>

    /** Resumen de artistas con nº de canciones, calculado por GROUP BY en SQL. */
    fun observeArtistSummaries(): Flow<List<ArtistSummary>>

    /** Resumen de álbumes con nº de canciones, calculado por GROUP BY en SQL. */
    fun observeAlbumSummaries(): Flow<List<AlbumSummary>>
}
