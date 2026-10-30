package com.example.mimusica.domain.model

object MusicSourceIdentityKey {
    private const val MUSIC_DIRECTORY = "Music/"

    fun fromRelativePath(volumeName: String, relativePath: String): String? {
        if (!relativePath.startsWith(MUSIC_DIRECTORY) || relativePath.split('/').any { it == ".." }) {
            return null
        }
        val volume = when (volumeName.lowercase()) {
            "external", "external_primary", "primary" -> "primary"
            else -> volumeName.lowercase()
        }
        return "music:$volume:${relativePath.trimStart('/')}"
    }
}
