package com.example.mimusica.data.db

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songUri"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["uri"],
            childColumns = ["songUri"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["playlistId", "position"]),
        Index(value = ["songUri"]),
    ],
)
data class PlaylistSongEntity(
    val playlistId: Long,
    val songUri: String,
    val position: Int,
)
