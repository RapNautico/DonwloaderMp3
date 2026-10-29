package com.example.mimusica.presentation.library

import com.example.mimusica.domain.model.SongFileAvailability
import com.example.mimusica.domain.repository.SongFileValidator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryFileCheckModelTest {

    @Test
    fun publica_estado_comprobando_y_luego_resultado() = runBlocking {
        val model = LibraryFileCheckModel(
            SongFileValidator {
                delay(50)
                SongFileAvailability.AVAILABLE
            },
        )

        val job = launch { model.check("content://audio/1") }
        while (model.availability.value["content://audio/1"] != SongFileAvailability.CHECKING) {
            delay(1)
        }
        assertEquals(SongFileAvailability.CHECKING, model.availability.value["content://audio/1"])
        job.join()
        assertEquals(SongFileAvailability.AVAILABLE, model.availability.value["content://audio/1"])
    }

    @Test
    fun conserva_estado_por_uri_para_ausente_inaccesible_e_invalido() = runBlocking {
        val results = listOf(
            SongFileAvailability.MISSING,
            SongFileAvailability.INACCESSIBLE,
            SongFileAvailability.INVALID,
        ).iterator()
        val model = LibraryFileCheckModel(SongFileValidator { results.next() })

        model.check("content://audio/missing")
        model.check("content://audio/locked")
        model.check("content://audio/corrupt")

        assertEquals(
            mapOf(
                "content://audio/missing" to SongFileAvailability.MISSING,
                "content://audio/locked" to SongFileAvailability.INACCESSIBLE,
                "content://audio/corrupt" to SongFileAvailability.INVALID,
            ),
            model.availability.value,
        )
    }

    @Test
    fun elimina_estado_de_uris_que_ya_no_estan_en_biblioteca() = runBlocking {
        val model = LibraryFileCheckModel(SongFileValidator { SongFileAvailability.MISSING })
        model.check("content://audio/1")

        model.retainSongs(emptySet())

        assertEquals(emptyMap<String, SongFileAvailability>(), model.availability.value)
    }
}
