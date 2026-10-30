package com.example.mimusica.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import com.example.mimusica.data.db.toEntity
import com.example.mimusica.domain.model.SongSourceRecord

@Dao
abstract class SongSourceIdentityDao {
    @Query("SELECT * FROM song_source_identities WHERE identityKey = :key")
    protected abstract suspend fun identity(key: String): SongSourceIdentityEntity?

    @Query("SELECT songUri FROM song_source_identities WHERE identityKey = :key")
    protected abstract suspend fun songUriFor(key: String): String?

    @Query("SELECT * FROM songs WHERE uri = :uri")
    protected abstract suspend fun song(uri: String): SongEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertSong(song: SongEntity): Long

    @Update
    protected abstract suspend fun updateSong(song: SongEntity): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertIdentity(identity: SongSourceIdentityEntity): Long

    @Query("SELECT identityKey, modifiedAtMs, sizeBytes FROM song_source_identities WHERE identityKey IN (:keys)")
    abstract suspend fun findKnown(keys: List<String>): List<KnownSongSourceIdentity>

    @Transaction
    open suspend fun ingest(records: List<SongSourceRecord>): Int {
        var insertedCount = 0
        records.forEach { record ->
            val source = record.identity
            val identityRow = source?.let { identity(it.key) }
            val existing = identityRow?.let { song(it.songUri) }
                ?: song(record.song.uri)

            if (existing != null) {
                if (
                    source?.modifiedAtMs != null &&
                    (
                        source.modifiedAtMs != identityRow?.modifiedAtMs ||
                            source.sizeBytes != identityRow.sizeBytes
                        )
                ) {
                    val metadata = record.song.toEntity().copy(
                        uri = existing.uri,
                        dateAddedMs = existing.dateAddedMs,
                        artworkPath = record.song.artworkPath ?: existing.artworkPath,
                        mimeType = record.song.mimeType ?: existing.mimeType,
                        sizeBytes = record.song.sizeBytes ?: existing.sizeBytes,
                        bitrateBps = record.song.bitrateBps ?: existing.bitrateBps,
                        sampleRateHz = record.song.sampleRateHz ?: existing.sampleRateHz,
                        channels = record.song.channels ?: existing.channels,
                        genre = record.song.genre ?: existing.genre,
                    )
                    updateSong(metadata)
                }
                if (source != null) {
                    insertIdentity(
                        SongSourceIdentityEntity(
                            identityKey = source.key,
                            songUri = existing.uri,
                            modifiedAtMs = source.modifiedAtMs ?: identityRow?.modifiedAtMs,
                            sizeBytes = source.sizeBytes ?: identityRow?.sizeBytes,
                        ),
                    )
                }
                return@forEach
            }

            val insertId = insertSong(record.song.toEntity())
            val persistedSong = if (insertId >= 0L) {
                insertedCount += 1
                record.song.toEntity()
            } else {
                song(record.song.uri)
            }
            if (source != null && persistedSong != null) {
                insertIdentity(
                    SongSourceIdentityEntity(
                        identityKey = source.key,
                        songUri = persistedSong.uri,
                        modifiedAtMs = source.modifiedAtMs,
                        sizeBytes = source.sizeBytes,
                    ),
                )
            }
        }
        return insertedCount
    }
}

data class KnownSongSourceIdentity(
    val identityKey: String,
    val modifiedAtMs: Long?,
    val sizeBytes: Long?,
)
