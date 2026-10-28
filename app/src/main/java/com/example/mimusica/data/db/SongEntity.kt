package com.example.mimusica.data.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Fila de la tabla de canciones.
 *
 * Identidad estable: la URI del contenido es la PRIMARY KEY, así el mismo
 * archivo importado varias veces no crea duplicados (requisito de FASE 4).
 * Índices de búsqueda: título y artista (consultas frecuentes de la biblioteca).
 */
@Entity(
    tableName = "songs",
    indices = [Index("title"), Index("artist")],
)
data class SongEntity(
    @PrimaryKey val uri: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long,
    val dateAddedMs: Long,
    // Metadatos opcionales justificados en domain/model/Song.kt:
    // null = desconocido, nunca inventado.
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val bitrateBps: Long? = null,
    val sampleRateHz: Int? = null,
    val channels: Int? = null,
    val artworkPath: String? = null,
)
