package com.example.mimusica.domain.model

data class PlaylistSummary(
    val id: Long,
    val name: String,
    val songCount: Int,
    val artworkPath: String?,
)

data class PlaylistDetails(
    val playlist: PlaylistSummary,
    val songs: List<Song>,
)
