package com.example.mimusica.presentation.library

import com.example.mimusica.domain.repository.SongRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Modelo PRESENTACIÓN de la GESTIÓN de canciones (FASE 8.6, PLAN 8.6):
 * eliminar de biblioteca SOLO el registro Room (decisión D8.3: el archivo
 * físico nunca se elimina automáticamente). Acciones de reproducción y
 * playlists son CALLBACKS FUTUROS (FASE 9/10): no viven aquí.
 *
 * Testeable en JVM: [deleteSong] es suspend y se llama DIRECTO con
 * runBlocking (sin Main ni `kotlinx-coroutines-test`, NO autorizada).
 *
 * SINCRONIZACIÓN UI↔Room: tras un borrado, los Flows reactivos ya existentes
 * (`observeLibrary`, búsqueda, resúmenes de artista/álbum) re-emiten solos;
 * el modelo NO recarga nada por su cuenta (contratos 8.2 intactos).
 *
 * ERROR: no crashea; fija [error] (la UI lo muestra) y el SIGUIENTE ÉXITO lo
 * limpia, igual que en 8.3/8.4. [busy] evita eliminaciones concurrentes: si
 * ya hay una en curso, la segunda llamada se ignora silenciosamente.
 */
class LibraryManagerModel(
    private val repository: SongRepository,
) {

    private val _busy = MutableStateFlow(false)
    /** true mientras hay una eliminación EN CURSO. */
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    /** Error de la ÚLTIMA eliminación; null = sin error. */
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Elimina el REGISTRO de la canción (uri real del archivo como PK).
     * El mensaje de confirmación (ConfirmDialog) vive en la UI: aquí llega
     * la decisión ya confirmada por el usuario.
     */
    suspend fun deleteSong(uri: String) {
        if (_busy.value) return // ya hay una eliminación en curso
        _busy.value = true
        try {
            try {
                repository.deleteSong(uri)
                _error.value = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (fallo: Throwable) {
                _error.value = fallo.message ?: "error desconocido"
            }
        } finally {
            _busy.value = false
        }
    }
}
