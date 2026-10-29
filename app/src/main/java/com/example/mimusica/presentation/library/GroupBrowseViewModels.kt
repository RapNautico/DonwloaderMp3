package com.example.mimusica.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mimusica.domain.repository.SongRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModels de las vistas de artistas/álbumes (FASE 8.4). Son FINALES:
 * toda la lógica vive en [GroupSummariesModel]/[GroupDetailModel]
 * (testeable en JVM con scope propio, patrón de 8.3).
 */
class ArtistsViewModel(songRepository: SongRepository) : ViewModel() {

    /** Resúmenes REACTIVOS de artistas (SQL GROUP BY, bucket desconocido = null). */
    private val model = GroupSummariesModel(songRepository.observeArtistSummaries())

    val artists get() = model.items // StateFlow<List<ArtistSummary>> (genérico tipado)
    val isLoading get() = model.isLoading
    val error get() = model.error

    fun retry() {
        model.retryIn(viewModelScope)
    }

    init {
        model.startIn(viewModelScope)
    }
}

class AlbumsViewModel(songRepository: SongRepository) : ViewModel() {

    /** Resúmenes REACTIVOS de álbumes (SQL GROUP BY, bucket desconocido = null). */
    private val model = GroupSummariesModel(songRepository.observeAlbumSummaries())

    val albums get() = model.items // StateFlow<List<AlbumSummary>> (genérico tipado)
    val isLoading get() = model.isLoading
    val error get() = model.error

    fun retry() {
        model.retryIn(viewModelScope)
    }

    init {
        model.startIn(viewModelScope)
    }
}

/**
 * Detalle de UN grupo (artista/álbum). El nombre llega de la RUTA navegada
 * (con su codec de URI en la capa de navegación) y `null` = bucket
 * desconocido, tratado como cualquier otro grupo.
 */
class GroupDetailViewModel(
    songRepository: SongRepository,
    kind: GroupKind,
    groupName: String?,
) : ViewModel() {

    private val model = GroupDetailModel(songRepository, kind, groupName)

    /** Nombre REAL del grupo (puro, sin etiqueta de presentación). */
    val groupName: String? get() = model.groupName

    val state: StateFlow<GroupDetailState> get() = model.state

    fun retry() {
        model.startIn(viewModelScope)
    }

    init {
        model.startIn(viewModelScope)
    }
}
