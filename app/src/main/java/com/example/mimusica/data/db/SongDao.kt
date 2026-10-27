package com.example.mimusica.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
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

    @Query(
        """
        SELECT * FROM songs
        WHERE title LIKE '%' || :query || '%' COLLATE NOCASE
           OR artist LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY title COLLATE NOCASE ASC
        """
    )
    suspend fun search(query: String): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<SongEntity>>
}
