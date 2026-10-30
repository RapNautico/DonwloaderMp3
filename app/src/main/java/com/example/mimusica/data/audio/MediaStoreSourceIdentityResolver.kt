package com.example.mimusica.data.audio

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.example.mimusica.domain.model.MusicSourceIdentityKey
import java.io.File
import java.io.IOException

class MediaStoreSourceIdentityResolver(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    fun resolve(rawUri: String): String? {
        val uri = try {
            Uri.parse(rawUri)
        } catch (_: IllegalArgumentException) {
            return null
        }
        return try {
            when (uri.authority) {
                EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY -> resolveExternalStorageDocument(uri)
                MEDIA_DOCUMENTS_AUTHORITY -> resolveMediaDocument(uri)
                else -> if (uri.authority == MediaStore.AUTHORITY) resolveMediaStoreUri(uri) else null
            }
        } catch (_: SecurityException) {
            null
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun identityForMediaStoreItem(
        volumeName: String,
        relativePath: String?,
        displayName: String?,
        legacyPath: String? = null,
    ): String? {
        val path = relativePath?.takeIf(MediaStoreAudioPathPolicy::includes)
            ?: legacyPath?.let(::relativeAudioPath)
            ?: return null
        val filename = displayName ?: path.substringAfterLast('/')
        if (filename.isBlank()) return null
        val normalizedPath = if (path.endsWith('/')) "$path$filename" else path
        return MusicSourceIdentityKey.fromRelativePath(volumeName, normalizedPath)
    }

    private fun resolveExternalStorageDocument(uri: Uri): String? {
        val documentId = DocumentsContract.getDocumentId(uri)
        val volume = documentId.substringBefore(':', missingDelimiterValue = "")
        val path = documentId.substringAfter(':', missingDelimiterValue = "")
        if (volume.isBlank() || !MediaStoreAudioPathPolicy.includes(path.substringBeforeLast('/', ""))) {
            return null
        }
        return MusicSourceIdentityKey.fromRelativePath(volume, path)
    }

    private fun resolveMediaDocument(uri: Uri): String? {
        val documentId = DocumentsContract.getDocumentId(uri)
        val (type, id) = documentId.split(':', limit = 2).takeIf { it.size == 2 } ?: return null
        if (type != "audio") return null
        val itemUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toLongOrNull() ?: return null)
        return resolveMediaStoreUri(itemUri)
    }

    private fun resolveMediaStoreUri(uri: Uri): String? = try {
        val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(MediaStore.MediaColumns.RELATIVE_PATH, MediaStore.MediaColumns.DISPLAY_NAME)
        } else {
            @Suppress("DEPRECATION")
            arrayOf(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME)
        }
        resolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val pathIndex = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                val path = if (pathIndex >= 0) cursor.getString(pathIndex) else null
                identityForMediaStoreItem(MediaStore.getVolumeName(uri), path, name)
            } else {
                @Suppress("DEPRECATION")
                val pathIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                val path = if (pathIndex >= 0) cursor.getString(pathIndex) else null
                identityForMediaStoreItem("primary", null, name, path)
            }
        }
    } catch (_: SecurityException) {
        null
    } catch (_: IOException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: IllegalStateException) {
        null
    }

    private fun relativeAudioPath(absolutePath: String): String? {
        val root = Environment.getExternalStorageDirectory().canonicalPath
        val path = File(absolutePath).canonicalPath
        val relative = path.removePrefix("$root${File.separator}")
        val relativeDirectory = relative.substringBeforeLast('/', "")
        return relative.takeIf { relative != path && MediaStoreAudioPathPolicy.includes(relativeDirectory) }
    }

    private companion object {
        const val EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY = "com.android.externalstorage.documents"
        const val MEDIA_DOCUMENTS_AUTHORITY = "com.android.providers.media.documents"
    }
}
