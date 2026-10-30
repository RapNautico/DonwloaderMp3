package com.example.mimusica.data.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaStoreAudioPathPolicyTest {
    @Test
    fun `incluye Audio raiz y subcarpetas sin incluir Music`() {
        assertTrue(MediaStoreAudioPathPolicy.includes("Audio"))
        assertTrue(MediaStoreAudioPathPolicy.includes("Audio/"))
        assertTrue(MediaStoreAudioPathPolicy.includes("Audio/Albums/"))
        assertFalse(MediaStoreAudioPathPolicy.includes("Music/Albums/"))
    }

    @Test
    fun `excluye las ubicaciones publicas de WhatsApp Audio y Voice Notes`() {
        assertExcluded(
            "WhatsApp/Media/WhatsApp Audio/",
            "WhatsApp/Media/WhatsApp Voice Notes/",
        )
    }

    @Test
    fun `excluye las ubicaciones de WhatsApp bajo Android media`() {
        assertExcluded(
            "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Audio/",
            "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Voice Notes/",
        )
    }

    @Test
    fun `exclusion de WhatsApp no depende de mayusculas`() {
        assertExcluded(
            "whatsapp/media/whatsapp audio/Album/",
            "android/MEDIA/COM.WHATSAPP/whatsapp/media/whatsapp voice notes/",
        )
        assertTrue(MediaStoreAudioPathPolicy.includes("aUdIo/Albums/"))
    }

    @Test
    fun `no excluye otras ubicaciones por coincidencias parciales de nombre`() {
        assertFalse(MediaStoreAudioPathPolicy.isExcludedWhatsAppLocation("WhatsApp/Media/WhatsApp Audio Mix/"))
        assertFalse(MediaStoreAudioPathPolicy.isExcludedWhatsAppLocation("Audio/WhatsApp/Media/WhatsApp Audio/"))
        assertTrue(MediaStoreAudioPathPolicy.includes("Audio/WhatsApp Audio Mix/"))
    }

    @Test
    fun `rechaza rutas absolutas y traversal`() {
        assertFalse(MediaStoreAudioPathPolicy.includes("/Audio/Albums/"))
        assertFalse(MediaStoreAudioPathPolicy.includes("Audio/../WhatsApp/"))
    }

    private fun assertExcluded(vararg paths: String) {
        paths.forEach { path ->
            assertTrue(MediaStoreAudioPathPolicy.isExcludedWhatsAppLocation(path))
            assertFalse(MediaStoreAudioPathPolicy.includes(path))
        }
    }
}
