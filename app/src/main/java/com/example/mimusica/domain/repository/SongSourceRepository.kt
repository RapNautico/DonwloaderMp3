package com.example.mimusica.domain.repository

import com.example.mimusica.domain.model.SongSourceRecord
import com.example.mimusica.domain.model.SongSourceState

interface SongSourceRepository {
    suspend fun ingest(records: List<SongSourceRecord>): Int

    suspend fun findKnownIdentities(keys: List<String>): Map<String, SongSourceState>
}
