package com.example.mimusica.data.db

import androidx.room3.Dao
import androidx.room3.Embedded
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.example.mimusica.domain.model.PlaylistSummary
import kotlinx.coroutines.flow.Flow

data class PlaylistSongRow(
    @Embedded val song: SongEntity,
    val position: Int,
)

@Dao
abstract class PlaylistDao {
    @Query(
        """
        SELECT playlists.id AS id,
               playlists.name AS name,
               COUNT(playlist_songs.songUri) AS songCount,
               (
                   SELECT songs.artworkPath
                   FROM playlist_songs AS ordered_songs
                   JOIN songs ON songs.uri = ordered_songs.songUri
                   WHERE ordered_songs.playlistId = playlists.id
                   ORDER BY ordered_songs.position, ordered_songs.songUri
                   LIMIT 1
               ) AS artworkPath
        FROM playlists
        LEFT JOIN playlist_songs ON playlist_songs.playlistId = playlists.id
        GROUP BY playlists.id
        ORDER BY playlists.id
        """,
    )
    abstract fun observePlaylists(): Flow<List<PlaylistSummary>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    abstract fun observePlaylist(playlistId: Long): Flow<PlaylistEntity?>

    @Query(
        """
        SELECT songs.*, playlist_songs.position AS position
        FROM playlist_songs
        JOIN songs ON songs.uri = playlist_songs.songUri
        WHERE playlist_songs.playlistId = :playlistId
        ORDER BY playlist_songs.position, playlist_songs.songUri
        """,
    )
    abstract fun observeSongs(playlistId: Long): Flow<List<PlaylistSongRow>>

    @Query(
        """
        SELECT songs.*, playlist_songs.position AS position
        FROM playlist_songs
        JOIN songs ON songs.uri = playlist_songs.songUri
        WHERE playlist_songs.playlistId = :playlistId
        ORDER BY playlist_songs.position, playlist_songs.songUri
        """,
    )
    abstract suspend fun getSongs(playlistId: Long): List<PlaylistSongRow>

    @Query("SELECT songUri FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position, songUri")
    abstract suspend fun getSongUris(playlistId: Long): List<String>

    @Insert
    abstract suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :playlistId")
    abstract suspend fun renamePlaylist(playlistId: Long, name: String): Int

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    abstract suspend fun deletePlaylist(playlistId: Long): Int

    @Query(
        """
        INSERT OR IGNORE INTO playlist_songs (playlistId, songUri, position)
        SELECT :playlistId, :songUri,
               COALESCE((SELECT MAX(position) + 1 FROM playlist_songs WHERE playlistId = :playlistId), 0)
        WHERE EXISTS (SELECT 1 FROM playlists WHERE id = :playlistId)
          AND EXISTS (SELECT 1 FROM songs WHERE uri = :songUri)
        """,
    )
    abstract suspend fun appendSong(playlistId: Long, songUri: String): Long

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songUri = :songUri")
    abstract suspend fun removeSong(playlistId: Long, songUri: String): Int

    @Query(
        "UPDATE playlist_songs SET position = :position WHERE playlistId = :playlistId AND songUri = :songUri",
    )
    protected abstract suspend fun setPosition(playlistId: Long, songUri: String, position: Int): Int

    @Transaction
    open suspend fun reorderSongs(playlistId: Long, orderedSongUris: List<String>) {
        val currentUris = getSongUris(playlistId)
        require(
            orderedSongUris.size == currentUris.size &&
                orderedSongUris.toSet() == currentUris.toSet(),
        ) { "El orden debe contener exactamente las canciones actuales de la playlist." }
        orderedSongUris.forEachIndexed { index, uri ->
            setPosition(playlistId, uri, index)
        }
    }
}
