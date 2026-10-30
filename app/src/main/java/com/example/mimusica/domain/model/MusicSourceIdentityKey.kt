package com.example.mimusica.domain.model

object MusicSourceIdentityKey {
    private const val MUSIC_DIRECTORY = "Music/"
    private const val AUDIO_DIRECTORY = "Audio/"

    fun fromRelativePath(volumeName: String, relativePath: String): String? {
        val normalizedPath = relativePath.replace('\\', '/')
        val directory = when {
            normalizedPath.startsWith(MUSIC_DIRECTORY, ignoreCase = true) -> "Music"
            normalizedPath.startsWith(AUDIO_DIRECTORY, ignoreCase = true) -> "Audio"
            else -> return null
        }
        val segments = normalizedPath.split('/')
        if (
            normalizedPath.startsWith('/') ||
            segments.any { it.isBlank() || it == "." || it == ".." }
        ) {
            return null
        }
        val volume = when (volumeName.lowercase()) {
            "external", "external_primary", "primary" -> "primary"
            else -> volumeName.lowercase()
        }
        val canonicalPath = listOf(directory).plus(segments.drop(1)).joinToString("/")
        return "music:$volume:$canonicalPath"
    }
}
