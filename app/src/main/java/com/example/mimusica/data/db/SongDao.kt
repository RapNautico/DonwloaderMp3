package com.example.mimusica.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import com.example.mimusica.domain.model.AlbumSummary
import com.example.mimusica.domain.model.ArtistSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    /** IGNORE: si la URI ya existe, la fila existente se conserva intacta. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(songs: List<SongEntity>): List<Long>

    @Update
    suspend fun update(song: SongEntity): Int

    @Query("DELETE FROM songs WHERE uri = :uri")
    suspend fun deleteByUri(uri: String): Int

    @Query("SELECT * FROM songs WHERE uri = :uri")
    suspend fun getByUri(uri: String): SongEntity?

    /**
     * Búsqueda parcial e insensibilidad de mayúsculas: LIKE en SQLite es
     * case-insensitive para ASCII (comportamiento REAL de la BD; NO se usa
     * lower() con locale porque sería un comportamiento dependiente del
     * dispositivo). Las tildes NO se pliegan (asertado en pruebas).
     *
     * Los patrones llegan YA escapados desde el repositorio (ver
     * `escapeLikePattern`): `%` y `_` son LITERALes en la consulta del usuario
     * (política documentada en ARCHITECTURE §22.11), con `ESCAPE '\'`.
     *
     * Un WHERE OR no duplica filas (cada fila se evalúa una vez y la URI es PK):
     * nunca hay duplicados en el resultado. Los artist/album NULL no igualan
     * ningún patrón (NULL LIKE … es NULL en SQL): correcto, hay consultas
     * específicas para los buckets desconocidos.
     * Orden secundario por uri: resultados equivalentes estables entre llamadas.
     */
    @Query(
        """
        SELECT * FROM songs
        WHERE title LIKE :titlePattern ESCAPE '\'
           OR artist LIKE :artistPattern ESCAPE '\'
           OR album LIKE :albumPattern ESCAPE '\'
        ORDER BY title COLLATE NOCASE ASC, uri ASC
        """
    )
    suspend fun search(
        titlePattern: String,
        artistPattern: String,
        albumPattern: String,
    ): List<SongEntity>

    /** Búsqueda vacía = biblioteca completa en el mismo orden estable. */
    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC, uri ASC")
    suspend fun getAll(): List<SongEntity>

    /** Listado reactivo completo (misma ordenación estable). */
    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC, uri ASC")
    fun observeAll(): Flow<List<SongEntity>>

    /** Recientes: dateAddedMs DESC (índice dateAddedMs), con desempate por uri. */
    @Query("SELECT * FROM songs ORDER BY dateAddedMs DESC, uri ASC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<SongEntity>>

    /** Canciones de UN artista (el `null` = bucket desconocido). */
    @Query(
        """
        SELECT * FROM songs
        WHERE (:artist IS NULL AND artist IS NULL) OR artist = :artist
        ORDER BY album COLLATE NOCASE ASC, title COLLATE NOCASE ASC, uri ASC
        """
    )
    suspend fun getByArtist(artist: String?): List<SongEntity>

    /** Canciones de UN álbum (el `null` = bucket desconocido). Orden REAL de SQLite: NULL < texto. */
    @Query(
        """
        SELECT * FROM songs
        WHERE (:album IS NULL AND album IS NULL) OR album = :album
        ORDER BY artist COLLATE NOCASE ASC, title COLLATE NOCASE ASC, uri ASC
        """
    )
    suspend fun getByAlbum(album: String?): List<SongEntity>

    /**
     * Agrupados en SQL (D8.3): una fila por artista, con conteo.
     * NULL se agrupa como UN solo bucket; en la ordenación los buckets
     * desconocidos van AL FINAL (UI los mostrará como "Desconocido").
     */
    @Query(
        """
        SELECT artist AS name, COUNT(*) AS songCount
        FROM songs
        GROUP BY artist
        ORDER BY CASE WHEN artist IS NULL THEN 1 ELSE 0 END,
                 artist COLLATE NOCASE ASC
        """
    )
    fun observeArtistSummaries(): Flow<List<ArtistSummary>>

    @Query(
        """
        SELECT album AS name, COUNT(*) AS songCount
        FROM songs
        GROUP BY album
        ORDER BY CASE WHEN album IS NULL THEN 1 ELSE 0 END,
                 album COLLATE NOCASE ASC
        """
    )
    fun observeAlbumSummaries(): Flow<List<AlbumSummary>>
}
