package com.example.mimusica.data.db

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)
