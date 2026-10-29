package com.example.mimusica.data.repository

import com.example.mimusica.data.db.SongDao
import com.example.mimusica.data.db.SongEntity
import com.example.mimusica.data.db.toEntity
import com.example.mimusica.data.db.toSong
import com.example.mimusica.domain.model.AlbumSummary
import com.example.mimusica.domain.model.ArtistSummary
import com.example.mimusica.domain.model.Song
import com.example.mimusica.domain.model.SongSort
import com.example.mimusica.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementación Room del repositorio. Traduce modelos de Domain <-> Entities
 * y aísla a Domain del DAO (AGENTS.md §4).
 *
 * FASE 8.2: el patrón LIKE se construye aquí (capa de datos) para mantener la
 * política de comodines en UN solo punto y testeable en JVM.
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

    override suspend fun searchSongs(query: String): List<Song> {
        val trimmed = query.trim()
        // Búsqueda vacía = biblioteca completa (estado explícito; LIKE '%%'
        // NO es un sustituto correcto: el NULL de artist/album no igualaría
        // `LIKE '%%'` en SQL). Resuelta en Room con una consulta dedicada,
        // nunca replicando la biblioteca en memoria.
        if (trimmed.isEmpty()) return dao.getAll().map { it.toSong() }
        val pattern = "%" + escapeLikePattern(trimmed) + "%"
        return dao.search(
            titlePattern = pattern,
            artistPattern = pattern,
            albumPattern = pattern,
        ).map { it.toSong() }
    }

    override fun observeLibrary(): Flow<List<Song>> =
        observeLibrary(SongSort.TITLE_ASC)

    /**
     * FASE 8.3: orden dedicado en SQL por variante (consultas explícitas del
     * DAO, sin magia dinámica); cada flujo ya viene estable con desempate uri.
     */
    override fun observeLibrary(sortedBy: SongSort): Flow<List<Song>> {
        val flow = when (sortedBy) {
            SongSort.TITLE_ASC -> dao.observeAll()
            SongSort.TITLE_DESC -> dao.observeAllTitleDesc()
            SongSort.ARTIST_ASC -> dao.observeAllArtistAsc()
            SongSort.ALBUM_ASC -> dao.observeAllAlbumAsc()
            SongSort.DATE_ADDED_DESC -> dao.observeAllDateAddedDesc()
            SongSort.DATE_ADDED_ASC -> dao.observeAllDateAddedAsc()
            SongSort.DURATION_DESC -> dao.observeAllDurationDesc()
        }
        return flow.map { rows -> rows.map { it.toSong() } }
    }

    override fun observeRecentSongs(limit: Int): Flow<List<Song>> =
        dao.observeRecent(limit).map { rows -> rows.map { it.toSong() } }

    override suspend fun getSongsByArtist(artist: String?): List<Song> =
        dao.getByArtist(artist).map { it.toSong() }

    override suspend fun getSongsByAlbum(album: String?): List<Song> =
        dao.getByAlbum(album).map { it.toSong() }

    override fun observeArtistSummaries(): Flow<List<ArtistSummary>> =
        dao.observeArtistSummaries()

    override fun observeAlbumSummaries(): Flow<List<AlbumSummary>> =
        dao.observeAlbumSummaries()

    companion object {
        /**
         * Escapado para LIKE con `ESCAPE '\'` (FASE 8.2, política documentada):
         * los comodines de SQL (`%`, `_`) y el propio carácter de escape (`\`)
         * se tratan como TEXTO LITERAL cuando provienen de la consulta del
         * usuario. Resultado: buscar "50%" encuentra "50%" y NO cualquier
         * texto de dos caracteres que acabe en "0" — nunca accidental.
         */
        internal fun escapeLikePattern(raw: String): String =
            raw
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
    }
}
