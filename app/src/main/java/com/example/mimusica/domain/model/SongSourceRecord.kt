package com.example.mimusica.domain.model

data class SongSourceIdentity(
    val key: String,
    val modifiedAtMs: Long? = null,
    val sizeBytes: Long? = null,
)

data class SongSourceRecord(
    val song: Song,
    val identity: SongSourceIdentity?,
)

data class SongSourceState(
    val modifiedAtMs: Long?,
    val sizeBytes: Long?,
)

fun SongSourceIdentity.requiresRefresh(known: SongSourceState?): Boolean =
    known == null ||
        modifiedAtMs != known.modifiedAtMs ||
        sizeBytes != known.sizeBytes
