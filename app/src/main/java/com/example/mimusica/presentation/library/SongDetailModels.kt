package com.example.mimusica.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mimusica.domain.model.Song
import com.example.mimusica.domain.repository.SongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Estado del DETALLE de una canción (FASE 8.5, PLAN 8.5): bottom sheet M3
 * con datos REALES; `null` del Domain → "Desconocido" SOLO en la UI; nada de
 * bitrate/calidad/codec inventados.
 *
 * [NotFound] cubre la canción quitada de la biblioteca / uri desconocida:
 * estado visible explícito, sin crash (AGENTS §12: nunca cerrar por datos).
 */
sealed interface SongDetailState {
    data object Loading : SongDetailState
    data class Content(val song: Song) : SongDetailState
    data object NotFound : SongDetailState
    data class Error(val message: String) : SongDetailState
}

/**
 * Modelo PRESENTACIÓN del detalle de canción (FASE 8.5). Testeable en JVM:
 * `load()` es suspend y se llama DIRECTO desde pruebas con runBlocking
 * (sin `viewModelScope`/Main ni `kotlinx-coroutines-test`, NO autorizada).
 *
 * Contrato usado: `SongRepository.getSong(uri)` (existente desde 8.2). El
 * `Song` viaja INTACTO incluyendo TODOS los null (regla datos reales): el
 * mapeo a "Desconocido" ocurre único en la capa de UI.
 */
class SongDetailModel(
    private val repository: SongRepository,
    /** URI real de la canción; `null`/vacía (defensa de navegación) = NotFound. */
    val uri: String?,
) {

    private val _state = MutableStateFlow<SongDetailState>(SongDetailState.Loading)
    val state: StateFlow<SongDetailState> = _state.asStateFlow()

    suspend fun load() {
        _state.value = SongDetailState.Loading
        val destino = uri?.takeIf { it.isNotBlank() }
        if (destino == null) {
            _state.value = SongDetailState.NotFound
            return
        }
        val resultado = runCatching { repository.getSong(destino) }
        _state.value = resultado.fold(
            onSuccess = { canción ->
                canción?.let { SongDetailState.Content(it) } ?: SongDetailState.NotFound
            },
            onFailure = { fallo ->
                SongDetailState.Error(fallo.message ?: "error desconocido")
            },
        )
    }
}

/**
 * ViewModel FINAL del detalle de canción: la lógica vive en
 * [SongDetailModel] (testeable); aquí solo se lanza en el scope de ciclo de
 * vida (patrón de 8.3/8.4).
 */
class SongDetailViewModel(
    songRepository: SongRepository,
    uri: String?,
) : ViewModel() {

    private val model = SongDetailModel(songRepository, uri)

    val state: StateFlow<SongDetailState> get() = model.state

    init {
        viewModelScope.launch { model.load() }
    }
}

// ---------- Helpers de formato (PUROS, testables, sin datos inventados) ----------

/**
 * Tamaño legible a partir del valor REAL en bytes; null/≤0 → null
 * (la UI muestra "Desconocido", jamás una cifra estimada).
 * Separador decimal Locale.ROOT ("."), convención técnica determinista.
 */
internal fun formatSongSize(sizeBytes: Long?): String? =
    sizeBytes?.takeIf { it > 0 }
        ?.let { String.format(Locale.ROOT, "%.1f MB", it / 1024.0 / 1024.0) }

/**
 * Bitrate legible a partir del valor REAL medido en importación
 * (MediaExtractor, bps); null/≤0 → null. La conversión a kbps es matemática
 * exacta sobre datos reales: no se inventa ni se redondea a engaño.
 */
internal fun formatSongBitrate(bitrateBps: Long?): String? =
    bitrateBps?.takeIf { it > 0 }
        ?.let { String.format(Locale.ROOT, "%.1f kbps", it / 1000.0) }

/** Frecuencia de muestreo REAL en Hz; null/≤0 → null (desconocida). */
internal fun formatSongSampleRate(sampleRateHz: Int?): String? =
    sampleRateHz?.takeIf { it > 0 }?.let { "$it Hz" }
