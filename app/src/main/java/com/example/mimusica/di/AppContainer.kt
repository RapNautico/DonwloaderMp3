package com.example.mimusica.di

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mimusica.data.audio.ArtworkStore
import com.example.mimusica.data.audio.AudioMetadataReader
import com.example.mimusica.data.audio.SafAudioImporter
import com.example.mimusica.data.db.AppDatabase
import com.example.mimusica.data.repository.SongRepositoryImpl
import com.example.mimusica.domain.importing.ImportAudioUseCase
import com.example.mimusica.domain.repository.SongRepository
import com.example.mimusica.presentation.library.AlbumsViewModel
import com.example.mimusica.presentation.library.ArtistsViewModel
import com.example.mimusica.presentation.library.GroupDetailViewModel
import com.example.mimusica.presentation.library.GroupKind
import com.example.mimusica.presentation.library.LibraryViewModel

/**
 * Inyección de dependencias MANUAL (decisión de ARCHITECTURE §18: sin Hilt
 * mientras el proyecto sea monomódulo y las dependencias sean pocas).
 * Lazy: cada dependencia se crea solo cuando se necesita por primera vez.
 */
class AppContainer(private val app: Application) {

    // Data
    private val database: AppDatabase by lazy { AppDatabase.build(app) }
    val songRepository: SongRepository by lazy { SongRepositoryImpl(database.songDao()) }

    // Importación (FASE 5)
    private val metadataReader by lazy { AudioMetadataReader(app) }
    private val artworkStore by lazy { ArtworkStore(app) }
    val audioImporter: SafAudioImporter by lazy {
        SafAudioImporter(app, metadataReader, artworkStore)
    }
    val importAudioUseCase: ImportAudioUseCase by lazy {
        ImportAudioUseCase(audioImporter, songRepository)
    }

    val viewModelFactory by lazy { createViewModelFactory() }

    private fun createViewModelFactory(): ViewModelProvider.Factory =
        viewModelFactory {
            initializer {
                LibraryViewModel(importAudioUseCase, songRepository)
            }
            initializer {
                ArtistsViewModel(songRepository)
            }
            initializer {
                AlbumsViewModel(songRepository)
            }
        }

    /**
     * Factories del DETALLE de grupo (FASE 8.4): el nombre del artista/álbum
     * llega de la ruta de navegación (nullable; null = bucket desconocido),
     * así que cada destino necesita SU propia factory con ese parámetro.
     */
    fun artistDetailViewModelFactory(name: String?): ViewModelProvider.Factory =
        viewModelFactory {
            initializer { GroupDetailViewModel(songRepository, GroupKind.ARTIST, name) }
        }

    fun albumDetailViewModelFactory(name: String?): ViewModelProvider.Factory =
        viewModelFactory {
            initializer { GroupDetailViewModel(songRepository, GroupKind.ALBUM, name) }
        }

    /**
     * Persistencia del acceso de SAF (FASE 5 §5): tras `OpenMultipleDocuments`
     * el documento UI concede FLAG_GRANT_PERSISTABLE_URI_PERMISSION y aquí se
     * materializa con takePersistableUriPermission.
     *
     * Limitaciones documentadas:
     * - No todos los proveedores conceden permisos persistibles:
     *   SecurityException se ignora (la app seguirá sin ese documento; Room
     *   conserva los metadatos igualmente).
     * - Límite del sistema: ~128 permisos persistibles por app (API < 31) y
     *   512 (API 31+); si se supera el sistema revoca los más antiguos.
     */
    fun takePersistableUriPermissions(uris: List<Uri>) {
        uris.forEach { uri ->
            runCatching {
                app.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }
    }
}
