package com.example.mimusica.domain.model

/**
 * Resumen de un artículo de la biblioteca (FASE 8.2, D8.3):
 * calculado por Room con GROUP BY (una fila por grupo, sin N+1),
 * no cargando la biblioteca entera en memoria.
 *
 * Regla de datos reales: `name` es String? para modelar el valor NULL del
 * archivo tal cual es — la sustitución por "Desconocido" ocurre SOLO en la
 * capa de presentación. Nunca se guarda una cadena mágica en la base de datos.
 */
data class ArtistSummary(
    /** Artista real; `null` = bucket "Artista desconocido" (agrupa todas las canciones sin artista). */
    val name: String?,
    val songCount: Int,
)

data class AlbumSummary(
    /** Álbum real; `null` = bucket "Álbum desconocido". */
    val name: String?,
    val songCount: Int,
)
