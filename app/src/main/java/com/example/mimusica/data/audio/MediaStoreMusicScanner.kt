package com.example.mimusica.data.audio

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.example.mimusica.domain.importing.ImportAudioUseCase
import com.example.mimusica.domain.model.MusicScanStatus
import com.example.mimusica.domain.model.SongSourceIdentity
import com.example.mimusica.domain.model.SongSourceRecord
import com.example.mimusica.domain.model.requiresRefresh
import com.example.mimusica.domain.repository.AppSettingsRepository
import com.example.mimusica.domain.repository.MusicLibraryScanner
import com.example.mimusica.domain.repository.SongRepository
import com.example.mimusica.domain.repository.SongSourceRepository
import java.io.File
import java.io.IOException
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class MediaStoreMusicScanner(
    context: Context,
    private val importAudio: ImportAudioUseCase,
    private val songRepository: SongRepository,
    private val sourceRepository: SongSourceRepository,
    private val settingsRepository: AppSettingsRepository,
    private val identityResolver: MediaStoreSourceIdentityResolver,
) : MusicLibraryScanner {
    private val appContext = context.applicationContext
    private val contentResolver = appContext.contentResolver
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val scanMutex = Mutex()
    private val mutableStatus = MutableStateFlow(MusicScanStatus())
    private var observer: ContentObserver? = null
    private var pendingScan: Job? = null
    private var aliasesInitialized = false

    override val status: StateFlow<MusicScanStatus> = mutableStatus.asStateFlow()

    override suspend fun scanMusicFolder() {
        scanMutex.withLock {
            if (!hasAudioPermission()) {
                mutableStatus.value = MusicScanStatus(
                    error = appContext.getString(com.example.mimusica.R.string.settings_scan_permission_required),
                )
                return@withLock
            }
            mutableStatus.value = MusicScanStatus(isScanning = true)
            try {
                val indexedFiles = withContext(Dispatchers.IO) { queryMusicFiles() }
                registerExistingAliases(indexedFiles)
                val known = sourceRepository.findKnownIdentities(indexedFiles.map { it.identityKey }.distinct())
                val needsImport = indexedFiles.filter { file ->
                    file.identity.requiresRefresh(known[file.identityKey])
                }
                val report = importAudio(
                    uris = needsImport.map { it.uri.toString() },
                    sourceIdentities = needsImport.associate { it.uri.toString() to it.identity },
                )
                mutableStatus.value = MusicScanStatus(
                    hasScanned = true,
                    indexedFiles = indexedFiles.size,
                    addedSongs = report.songsAdded,
                    duplicates = report.duplicatesSkipped,
                    failedFiles = report.failures.size,
                )
            } catch (cancelled: CancellationException) {
                mutableStatus.value = MusicScanStatus()
                throw cancelled
            } catch (failure: SecurityException) {
                mutableStatus.value = MusicScanStatus(
                    error = appContext.getString(com.example.mimusica.R.string.settings_scan_permission_required),
                )
            } catch (failure: Exception) {
                mutableStatus.value = MusicScanStatus(
                    error = appContext.getString(
                        com.example.mimusica.R.string.settings_scan_error,
                    ),
                )
            }
        }
    }

    override fun startAutomaticUpdates() {
        if (!settingsRepository.settings.value.scanMusicFolder) return
        if (!hasAudioPermission()) {
            mutableStatus.value = MusicScanStatus(
                error = appContext.getString(com.example.mimusica.R.string.settings_scan_permission_required),
            )
            return
        }
        if (observer == null) {
            val newObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    pendingScan?.cancel()
                    pendingScan = scope.launch {
                        delay(CONTENT_CHANGE_DEBOUNCE_MS)
                        if (settingsRepository.settings.value.scanMusicFolder) scanMusicFolder()
                    }
                }
            }
            try {
                contentResolver.registerContentObserver(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    true,
                    newObserver,
                )
                observer = newObserver
            } catch (_: SecurityException) {
                mutableStatus.value = MusicScanStatus(
                    error = appContext.getString(com.example.mimusica.R.string.settings_scan_permission_required),
                )
                return
            }
        }
        pendingScan = scope.launch { scanMusicFolder() }
    }

    override fun stopAutomaticUpdates() {
        observer?.let(contentResolver::unregisterContentObserver)
        observer = null
        pendingScan?.cancel()
        pendingScan = null
    }

    private suspend fun registerExistingAliases(files: List<IndexedMusicFile>) {
        if (aliasesInitialized || files.isEmpty()) return
        val keysByUri = files.associate { it.uri.toString() to it.identityKey }
        val idKeys = files.groupBy { it.mediaId.toString() }
            .mapValues { (_, matches) -> matches.map { it.identityKey }.distinct().singleOrNull() }
        val existingSongs = songRepository.searchSongs("")
        val aliases = existingSongs.mapNotNull { song ->
            val key = keysByUri[song.uri] ?: if (
                Uri.parse(song.uri).authority == "com.android.providers.media"
            ) {
                null
            } else {
                identityResolver.resolve(song.uri)
            }
                ?: mediaDocumentIdentity(song.uri, idKeys)
            key?.let { SongSourceRecord(song, SongSourceIdentity(it)) }
        }
        sourceRepository.ingest(aliases)
        aliasesInitialized = true
    }

    private fun mediaDocumentIdentity(
        songUri: String,
        byId: Map<String, String?>,
    ): String? {
        val uri = try {
            Uri.parse(songUri)
        } catch (_: IllegalArgumentException) {
            return null
        }
        if (uri.authority != "com.android.providers.media.documents") return null
        val documentId = try {
            android.provider.DocumentsContract.getDocumentId(uri).substringAfter(':')
        } catch (_: IllegalArgumentException) {
            return null
        } catch (_: SecurityException) {
            return null
        }
        if (!documentId.startsWith("audio:")) return null
        val id = documentId.substringAfter(':')
        return byId[id]
    }

    private fun queryMusicFiles(): List<IndexedMusicFile> {
        val results = mutableListOf<IndexedMusicFile>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.getExternalVolumeNames(appContext).forEach { volumeName ->
                val collection = MediaStore.Audio.Media.getContentUri(volumeName)
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    MediaStore.MediaColumns.SIZE,
                    MediaStore.MediaColumns.DATE_MODIFIED,
                )
                (contentResolver.query(
                    collection,
                    projection,
                    "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?",
                    arrayOf("$MUSIC_DIRECTORY%"),
                    null,
                ) ?: throw IOException("MediaStore query returned no cursor")).use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
                    val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(nameColumn) ?: continue
                        val path = cursor.getString(pathColumn) ?: continue
                        if (!path.startsWith(MUSIC_DIRECTORY)) continue
                        val id = cursor.getLong(idColumn)
                        val uri = ContentUris.withAppendedId(collection, id)
                        val key = identityResolver.identityForMediaStoreItem(volumeName, path, name) ?: continue
                        results += IndexedMusicFile(
                            mediaId = id,
                            uri = uri,
                            identityKey = key,
                            identity = SongSourceIdentity(
                                key = key,
                                modifiedAtMs = cursor.getLong(modifiedColumn) * 1_000L,
                                sizeBytes = cursor.getLong(sizeColumn).takeIf { it >= 0L },
                            ),
                        )
                    }
                }
            }
        } else {
            @Suppress("DEPRECATION")
            val dataColumnName = MediaStore.MediaColumns.DATA
            @Suppress("DEPRECATION")
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                dataColumnName,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_MODIFIED,
            )
            (contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                "$dataColumnName LIKE ?",
                arrayOf("%/$MUSIC_DIRECTORY%"),
                null,
            ) ?: throw IOException("MediaStore query returned no cursor")).use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val pathColumn = cursor.getColumnIndexOrThrow(dataColumnName)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                while (cursor.moveToNext()) {
                    val path = cursor.getString(pathColumn) ?: continue
                    val name = cursor.getString(nameColumn) ?: File(path).name
                    if (identityResolver.identityForMediaStoreItem("primary", null, name, path) == null) continue
                    val id = cursor.getLong(idColumn)
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val key = identityResolver.identityForMediaStoreItem("primary", null, name, path) ?: continue
                    results += IndexedMusicFile(
                        mediaId = id,
                        uri = uri,
                        identityKey = key,
                        identity = SongSourceIdentity(
                            key = key,
                            modifiedAtMs = cursor.getLong(modifiedColumn) * 1_000L,
                            sizeBytes = cursor.getLong(sizeColumn).takeIf { it >= 0L },
                        ),
                    )
                }
            }
        }
        return results
    }

    private fun hasAudioPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return appContext.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private data class IndexedMusicFile(
        val mediaId: Long,
        val uri: Uri,
        val identityKey: String,
        val identity: SongSourceIdentity,
    )

    private companion object {
        const val MUSIC_DIRECTORY = "Music/"
        const val CONTENT_CHANGE_DEBOUNCE_MS = 700L
    }
}
