package com.example.mimusica.presentation.library

import com.example.mimusica.domain.importing.ImportReport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class LibraryImportModelTest {

    @Test
    fun cancelacion_se_propaga_limpia_el_estado_y_libera_el_bloqueo() = runBlocking {
        val cancelled = CancellationException("import cancelled")
        var nextFailure: Throwable? = cancelled
        val model = LibraryImportModel { _, _ ->
            nextFailure?.let { throw it }
            ImportReport(
                totalRequested = 1,
                songsAdded = 1,
                duplicatesSkipped = 0,
                failures = emptyList(),
            )
        }

        var caught: CancellationException? = null
        try {
            model.importFiles(listOf("content://song"))
        } catch (actual: CancellationException) {
            caught = actual
        }

        assertSame(cancelled, caught)
        assertEquals(ImportState.Idle, model.state.value)

        nextFailure = null
        model.importFiles(listOf("content://song"))

        assertEquals(
            ImportState.Done(
                ImportReport(
                    totalRequested = 1,
                    songsAdded = 1,
                    duplicatesSkipped = 0,
                    failures = emptyList(),
                ),
            ),
            model.state.value,
        )
        assertFalse(model.state.value is ImportState.Failed)
    }

    @Test
    fun excepcion_ordinaria_sigue_proyectandose_como_error_de_importacion() = runBlocking {
        val model = LibraryImportModel { _, _ ->
            throw IllegalStateException("fallo de lote")
        }

        model.importFiles(listOf("content://song"))

        assertEquals(ImportState.Failed("fallo de lote"), model.state.value)
    }
}
