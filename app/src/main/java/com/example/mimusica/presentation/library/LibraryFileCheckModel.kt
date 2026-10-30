package com.example.mimusica.presentation.library

import com.example.mimusica.domain.model.SongFileAvailability
import com.example.mimusica.domain.repository.SongFileValidator
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Estado de comprobaciones explícitas de archivos, separado de los metadatos de Room. */
class LibraryFileCheckModel(
    private val validator: SongFileValidator,
) {
    private val _availability = MutableStateFlow<Map<String, SongFileAvailability>>(emptyMap())
    val availability: StateFlow<Map<String, SongFileAvailability>> = _availability.asStateFlow()

    suspend fun check(uri: String) {
        if (_availability.value[uri] == SongFileAvailability.CHECKING) return

        val previous = _availability.value[uri]
        _availability.update { it + (uri to SongFileAvailability.CHECKING) }
        try {
            val result = validator.validate(uri)
            _availability.update { it + (uri to result) }
        } catch (cancelled: CancellationException) {
            _availability.update { current ->
                if (previous == null) current - uri else current + (uri to previous)
            }
            throw cancelled
        }
    }

    /** Elimina estados obsoletos cuando las canciones salen de Room. */
    fun retainSongs(uris: Set<String>) {
        _availability.update { current -> current.filterKeys(uris::contains) }
    }
}
