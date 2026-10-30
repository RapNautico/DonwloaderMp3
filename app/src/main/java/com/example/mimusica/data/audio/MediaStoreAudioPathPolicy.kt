package com.example.mimusica.data.audio

internal object MediaStoreAudioPathPolicy {
    private val whatsappLocations = listOf(
        listOf("WhatsApp", "Media", "WhatsApp Audio"),
        listOf("WhatsApp", "Media", "WhatsApp Voice Notes"),
        listOf("Android", "media", "com.whatsapp", "WhatsApp", "Media", "WhatsApp Audio"),
        listOf("Android", "media", "com.whatsapp", "WhatsApp", "Media", "WhatsApp Voice Notes"),
    )

    fun includes(relativePath: String?): Boolean {
        val segments = pathSegments(relativePath) ?: return false
        val isMusicRoot = segments.firstOrNull()?.let { root ->
            root.equals("Music", ignoreCase = true) || root.equals("Audio", ignoreCase = true)
        } == true
        return isMusicRoot &&
            !isExcludedWhatsAppLocation(segments)
    }

    fun isExcludedWhatsAppLocation(relativePath: String?): Boolean =
        pathSegments(relativePath)?.let(::isExcludedWhatsAppLocation) == true

    private fun isExcludedWhatsAppLocation(segments: List<String>): Boolean =
        whatsappLocations.any { excluded ->
            segments.size >= excluded.size &&
                excluded.indices.all { index ->
                    segments[index].equals(excluded[index], ignoreCase = true)
                }
        }

    private fun pathSegments(relativePath: String?): List<String>? {
        val path = relativePath?.replace('\\', '/') ?: return null
        if (path.startsWith('/') || path.contains("//")) return null
        val segments = path.trimEnd('/').split('/')
        if (segments.any { it.isBlank() || it == "." || it == ".." }) return null
        return segments
    }
}
