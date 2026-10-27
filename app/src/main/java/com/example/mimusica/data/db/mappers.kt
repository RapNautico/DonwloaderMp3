package com.example.mimusica.data.db

import com.example.mimusica.domain.model.Song

/** Mapeo explícito Entity <-> Dominio (AGENTS.md §19: sin magia implícita). */
fun Song.toEntity(): SongEntity = SongEntity(
    uri = uri,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    dateAddedMs = dateAddedMs,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    bitrateBps = bitrateBps,
)

fun SongEntity.toSong(): Song = Song(
    uri = uri,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    dateAddedMs = dateAddedMs,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    bitrateBps = bitrateBps,
)
