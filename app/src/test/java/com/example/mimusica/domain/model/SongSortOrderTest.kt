package com.example.mimusica.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * FASE 8.3: comparador puro por orden (SongSort.toComparator).
 * Debe replicar EXACTAMENTE el ORDER BY SQL con que ordena la biblioteca
 * completa: plegado ASCII (NOCASE), nulls al final y desempate por uri.
 */
class SongSortOrderTest {

    private val datos = listOf(
        song("u1", "beta", "B", "Álbum B", 100L, 30_000L), // duración media
        song("u2", "Alfa", null, "Álbum A", 90L, 50_000L), // más larga
        song("u3", "Ángel", "B", "Álbum Z", 95L, 10_000L), // la más corta
        song("u4", "alpha", "A", "Álbum A", 80L, 20_000L), // "alpha" = "Alfa" en NOCASE
        song("u5", "cortísima", null, null, 70L, 15_000L),
    )

    private fun song(
        uri: String,
        title: String,
        artist: String?,
        album: String?,
        dateAddedMs: Long,
        durationMs: Long,
    ) = Song(
        uri = uri,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        dateAddedMs = dateAddedMs,
    )

    private fun song(uri: String, title: String, artist: String?, album: String?) =
        song(uri, title, artist, album, dateAddedMs = 1L, durationMs = 1_000L)

    @Test
    fun titulo_asc_iguala_alfabetico_con_desempate_uri() {
        val orden = datos.sortedWith(SongSort.TITLE_ASC.toComparator()).map { it.uri }
        // "Alfa"="alpha" en NOCASE (empate → uri); "Ángel" SIN plegar va última (ASCII alto):
        assertEquals(listOf("u2", "u4", "u1", "u5", "u3"), orden)
        // TITLE_DESC invierte solo el título; el desempate por uri sigue ASC.
        // "alpha" (u4) > "alfa" (u2) en descenso:
        assertEquals(
            listOf("u3", "u5", "u1", "u4", "u2"),
            datos.sortedWith(SongSort.TITLE_DESC.toComparator()).map { it.uri },
        )
    }

    @Test
    fun nulls_al_final_en_artista() {
        val porArtista = datos.sortedWith(SongSort.ARTIST_ASC.toComparator()).map { it.uri }
        // u5 (artista null) ÚLTIMO:
        assertEquals("u5", porArtista.last())
    }

    @Test
    fun duracion_desc_orden_correcto() {
        // 50k, 30k, 20k, 15k, 10k:
        val porDuracion = datos.sortedWith(SongSort.DURATION_DESC.toComparator()).map { it.uri }
        assertEquals(listOf("u2", "u1", "u4", "u5", "u3"), porDuracion)
    }

    @Test
    fun recientes_antes_que_antiguas() {
        // Fechas: 100, 90, 95, 80, 70 (u1..u5):
        assertEquals(
            listOf("u1", "u3", "u2", "u4", "u5"),
            datos.sortedWith(SongSort.DATE_ADDED_DESC.toComparator()).map { it.uri },
        )
        assertEquals(
            listOf("u5", "u4", "u2", "u3", "u1"),
            datos.sortedWith(SongSort.DATE_ADDED_ASC.toComparator()).map { it.uri },
        )
    }
}
