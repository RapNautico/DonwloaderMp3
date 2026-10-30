package com.example.mimusica.data.db

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

@Entity(
    tableName = "song_source_identities",
    primaryKeys = ["identityKey"],
    indices = [Index("songUri")],
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["uri"],
            childColumns = ["songUri"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SongSourceIdentityEntity(
    val identityKey: String,
    val songUri: String,
    val modifiedAtMs: Long?,
    val sizeBytes: Long?,
)
