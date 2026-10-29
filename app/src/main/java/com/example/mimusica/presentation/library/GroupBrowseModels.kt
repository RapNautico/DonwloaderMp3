package com.example.mimusica.presentation.library

import com.example.mimusica.domain.model.Song
import com.example.mimusica.domain.repository.SongRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Tipo de grupo explorable (FASE 8.4): artista o álbum. El detalle reutiliza
 * la MISMA lógica para ambos: solo cambia la consulta del repositorio.
 */
enum class GroupKind { ARTIST, ALBUM }

/**
 * Estado del detalle de un grupo (artista o álbum). El bucket desconocido
 * (`groupName == null`) es un destino de navegación NORMAL: nunca bloquea ni
 * crashea; la UI lo rotula "Artista desconocido"/"Álbum desconocido".
 */
sealed interface GroupDetailState {
    data object Loading : GroupDetailState
    data class Content(val songs: List<Song>) : GroupDetailState
    data class Error(val message: String) : GroupDetailState
}

/**
 * Modelo PRESENTACIÓN de las listas de resúmenes (FASE 8.4): artistas o
 * álbumes REACTIVOS, con el conteo ya calculado por Room (GROUP BY en SQL,
 * una fila por grupo, sin N+1 ni cargar la biblioteca completa en memoria).
 *
 * Separado del ViewModel igual que `LibraryListModel` (8.3): testeable en JVM
 * con scope propio (`viewModelScope` usa Dispatchers.Main, indisponible en
 * pruebas unitarias sin `kotlinx-coroutines-test`, dependencia NO autorizada).
 *
 * El Flow del repositorio es REACTIVO (Room re-emite al cambiar la tabla):
 * no hay que recrear la recolección cuando la biblioteca cambia.
 *
 * ERRORES: no crashean ni borran lo ya cargado; fijan [error] (la UI lo
 * muestra) y el siguiente éxito lo limpia. El nombre `null` del bucket
 * desconocido viaja INTACTO: la sustitución por etiqueta "Desconocido"
 * ocurre SOLO en la capa de presentación (regla de datos reales).
 */
class GroupSummariesModel<T>(
    private val summaries: Flow<List<T>>,
) {

    private val _loading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _items = MutableStateFlow<List<T>>(emptyList())
    val items: StateFlow<List<T>> = _items.asStateFlow()

    private var observationJob: Job? = null

    fun startIn(scope: CoroutineScope) {
        observationJob?.cancel()
        observationJob = summaries
            .onEach { lista ->
                _loading.value = false
                _error.value = null
                _items.value = lista
            }
            .catch { fallo ->
                if (fallo is kotlinx.coroutines.CancellationException) throw fallo
                _loading.value = false
                _error.value = fallo.message ?: fallo::class.simpleName ?: "Error"
            }
            .launchIn(scope)
    }

    fun retryIn(scope: CoroutineScope) {
        _loading.value = true
        _error.value = null
        startIn(scope)
    }
}

/**
 * Modelo PRESENTACIÓN del DETALLE de un grupo (FASE 8.4): canciones de un
 * artista o de un álbum con el orden ya definido en SQL (`getByArtist`/
 * `getByAlbum`: álbum/artista, título, uri — paridad con 8.2/8.3).
 *
 * Carga ÚNICA al abrir el grupo (la consulta es suspend, contrato 8.2): el
 * snapshot es válido para navegar; la reactividad completa del detalle no
 * añade valor sin reproducción ni edición (ninguna en FASE 8) y evita
 * consultas re-repetidas al recomponer. Nombres `null` = bucket desconocido,
 * soportado de primera clase por el DAO/repo (requisito 8.4).
 *
 * Estado explícito Loading → Content | Error; un fallo NUNCA crashea.
 */
class GroupDetailModel(
    private val repository: SongRepository,
    private val kind: GroupKind,
    /** Nombre real del grupo; `null` = bucket desconocido (navegable). */
    val groupName: String?,
) {

    private val _state: MutableStateFlow<GroupDetailState> =
        MutableStateFlow(GroupDetailState.Loading)
    val state: StateFlow<GroupDetailState> = _state.asStateFlow()

    fun startIn(scope: CoroutineScope) {
        scope.launch {
            _state.value = GroupDetailState.Loading
            val resultado = runCatching {
                when (kind) {
                    GroupKind.ARTIST -> repository.getSongsByArtist(groupName)
                    GroupKind.ALBUM -> repository.getSongsByAlbum(groupName)
                }
            }
            _state.value = resultado.fold(
                onSuccess = { GroupDetailState.Content(it) },
                onFailure = { fallo ->
                    GroupDetailState.Error(fallo.message ?: "error desconocido")
                },
            )
        }
    }
}
