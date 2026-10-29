package com.example.mimusica.domain.model

/** Resultado de comprobar bajo demanda si el archivo asociado sigue utilizable. */
enum class SongFileAvailability {
    CHECKING,
    AVAILABLE,
    MISSING,
    INACCESSIBLE,
    INVALID,
}
