package com.example.mimusica.domain.repository

import com.example.mimusica.domain.model.SongFileAvailability

/** Comprueba una URI local sin modificar la biblioteca ni el archivo. */
fun interface SongFileValidator {
    suspend fun validate(uri: String): SongFileAvailability
}
