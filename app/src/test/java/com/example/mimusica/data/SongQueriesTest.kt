package com.example.mimusica.data

import com.example.mimusica.data.db.AppDatabase
import com.example.mimusica.data.repository.SongRepositoryImpl
import com.example.mimusica.domain.model.Song
import com.example.mimusica.domain.model.SongSort
import com.example.mimusica.domain.model.toComparator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * FASE 8.2: pruebas de las consultas de la biblioteca (búsqueda, recientes,
 * por artista/álbum, resúmenes agrupados) sobre Room+SQLite real en JVM
 * (BundledSQLiteDriver, sin emulador, sin Internet).
 */
class SongQueriesTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var db: AppDatabase
    private lateinit var repo: SongRepositoryImpl

    @Before
    fun setup() = runBlocking {
        db = AppDatabase.build(tmp.newFile("consultas.db").absolutePath)
        repo = SongRepositoryImpl(db.songDao())
        seed(
            song("uri-1", "Radio Ga Ga", artist = "Queen", album = "The Works"),
            song("uri-2", "Hot Space", artist = "Queen", album = "Hot Space"),
            song("uri-3", "Another_100 of %rock%", artist = "queen", album = null), // case de artista distinto
            song("uri-4", "Completamente distinto", artist = null, album = "Hot Space"),
            song("uri-5", "Ángel.avi", artist = "Barricada", album = "Rock_50%Verano"),
            song("uri-6", "zeta antigua", artist = "Barricada", album = "Rock_50%Verano",
                dateAddedMs = FECHA_VIEJA),
            song("uri-7", "Ana Antes", artist = null, album = null),
        )
        Unit
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun song(
        uri: String,
        title: String,
        artist: String? = null,
        album: String? = null,
        dateAddedMs: Long = FECHA_BASE,
    ) = Song(
        uri = uri,
        title = title,
        artist = artist,
        album = album,
        durationMs = 100_000L,
        dateAddedMs = dateAddedMs,
    )

    private suspend fun seed(vararg canciones: Song) {
        val insertadas = repo.addSongs(canciones.toList())
        assertEquals(canciones.size, insertadas)
    }

    // ---------- Búsqueda ----------

    @Test
    fun busca_por_titulo_parcial_case_insensitive() = runBlocking {
        val resultados = repo.searchSongs("RADIO")
        assertEquals(listOf("uri-1"), resultados.map { it.uri })
    }

    @Test
    fun busca_por_artista() = runBlocking {
        // "QUEEN" (mayúsculas) encuentra también "queen" (uri-3): ASCII case-insensitive.
        val queen = repo.searchSongs("QUEEN")
        assertEquals(setOf("uri-1", "uri-2", "uri-3"), queen.map { it.uri }.toSet())
    }

    @Test
    fun busca_por_album() = runBlocking {
        val resultados = repo.searchSongs("hot space")
        assertEquals(setOf("uri-2", "uri-4"), resultados.map { it.uri }.toSet())
    }

    @Test
    fun una_fila_no_se_duplica_aunque_coincida_en_varios_campos() = runBlocking {
        // uri-2 coincide por título (Hot Space) Y álbum (Hot Space); uri-4 solo por álbum:
        val resultados = repo.searchSongs("Hot Space")
        assertEquals(listOf("uri-4", "uri-2"), resultados.map { it.uri }) // cada URI UNA vez
    }

    @Test
    fun sin_coincidencias_devuelve_lista_vacia() = runBlocking {
        assertTrue(repo.searchSongs("xxxxxxxxxxxx").isEmpty())
    }

    @Test
    fun consulta_vacia_devuelve_biblioteca_completa() = runBlocking {
        // Con y sin espacios: comprobado el recorte del Repositorio.
        assertEquals(7, repo.searchSongs("").size)
        assertEquals(7, repo.searchSongs("     ").size)
        // Y en el mismo orden canónico (título NOCASE, desempate uri):
        assertEquals(repo.observeLibrary().first().map { it.uri }, repo.searchSongs("").map { it.uri })
    }

    @Test
    fun consulta_solo_espacios_se_recorta_antes_de_buscar() = runBlocking {
        // "  Ana  " -> "Ana": recorte exterior antes del patrón LIKE.
        assertEquals(listOf("uri-7"), repo.searchSongs("  Ana  ").map { it.uri })
    }

    @Test
    fun las_tildes_no_se_foldean_comportamiento_real_de_sqlite() = runBlocking {
        // Documentado: LIKE en SQLite solo ignora mayúsculas/minúsculas ASCII;
        // "Angel" (sin tilde) NO iguala "Ángel.avi". La UI (8.3) normalizará su
        // propia consulta si decide hacer folding de acentos, nunca la BD aquí.
        assertTrue(repo.searchSongs("Angel").isEmpty())
        assertEquals(listOf("uri-5"), repo.searchSongs("Ángel").map { it.uri })
    }

    @Test
    fun comodines_sql_se_tratan_como_texto_literal() = runBlocking {
        // POLÍTICA DOCUMENTADA (D8.3): % y _ son literal en la consulta del usuario.
        assertEquals(listOf("uri-3"), repo.searchSongs("100 of").map { it.uri }) // "_" literal
        assertEquals(listOf("uri-3"), repo.searchSongs("%rock%").map { it.uri }) // "%" literal
        // Si "%" fuera comodín, "A%a" igualadría casi todo; por eso solo iguala:
        assertEquals(listOf("uri-5"), repo.searchSongs("Ángel.avi").map { it.uri })
        // Y buscar "a_a" NO iguala "Ana Antes" (un solo carácter cualquiera):
        assertTrue(repo.searchSongs("a_a").isEmpty())
    }

    @Test
    fun nulos_de_artista_o_album_no_coinciden_con_patrones() = runBlocking {
        // uri-7 (sin artista ni álbum) solo puede aparecer por su TÍTULO:
        assertEquals(listOf("uri-7"), repo.searchSongs("Ana").map { it.uri })
        // Las filas con artist NULL no igualan búsquedas por artista:
        assertTrue(repo.searchSongs("barricada").none { it.uri == "uri-7" })
    }

    // ---------- Recientes ----------

    @Test
    fun recientes_ordenadas_por_fecha_desc_con_desempate() = runBlocking {
        // Solo uri-6 tiene fecha vieja; el resto comparte FECHA_BASE → desempate por uri ASC:
        assertEquals(listOf("uri-1", "uri-2"), repo.observeRecentSongs(limit = 2).first().map { it.uri })
    }

    @Test
    fun recientes_con_limit_superior_devuelve_todo() = runBlocking {
        assertEquals(7, repo.observeRecentSongs(limit = 100).first().size)
    }

    // ---------- Por artista / álbum ----------

    @Test
    fun canciones_de_un_artista_exacto() = runBlocking {
        val barricada = repo.getSongsByArtist("Barricada")
        assertEquals(setOf("uri-5", "uri-6"), barricada.map { it.uri }.toSet())
        // Igualdad exacta, NO parcial:
        assertTrue(repo.getSongsByArtist("Barri").isEmpty())
    }

    @Test
    fun bucket_artista_desconocido_via_null() = runBlocking {
        val desconocidas = repo.getSongsByArtist(artist = null)
        assertEquals(setOf("uri-4", "uri-7"), desconocidas.map { it.uri }.toSet())
    }

    @Test
    fun bucket_album_desconocido_via_null() = runBlocking {
        val desconocidas = repo.getSongsByAlbum(album = null)
        // Orden REAL de SQLite: el artist NULL (uri-7) va antes que "queen" (uri-3):
        assertEquals(listOf("uri-7", "uri-3"), desconocidas.map { it.uri })
    }

    @Test
    fun canciones_de_un_album_ordenadas_estables() = runBlocking {
        val hotSpace = repo.getSongsByAlbum("Hot Space")
        // Orden estable por artist NOCASE/title: "Completamente distinto" (uri-4) < "Hot Space" (uri-2):
        assertEquals(listOf("uri-4", "uri-2"), hotSpace.map { it.uri })
    }

    // ---------- Resúmenes (GROUP BY en SQL) ----------

    @Test
    fun resumen_de_artistas_con_conteo_y_desconocidos_al_final() = runBlocking {
        val artistas = repo.observeArtistSummaries().first()
        assertEquals(
            listOf(
                ArtistaEsperado("Barricada", 2),
                ArtistaEsperado("Queen", 2),
                ArtistaEsperado("queen", 1),
                ArtistaEsperado(null, 2), // uri-4 + uri-7: bucket UNICO
            ),
            artistas.map { ArtistaEsperado(it.name, it.songCount) },
        )
    }

    @Test
    fun resumen_de_albumes_con_conteo_y_desconocidos_al_final() = runBlocking {
        val albumes = repo.observeAlbumSummaries().first()
        assertEquals(
            listOf(
                AlbumEsperado("Hot Space", 2),
                AlbumEsperado("Rock_50%Verano", 2),
                AlbumEsperado("The Works", 1),
                AlbumEsperado(null, 2), // uri-3 + uri-7: bucket UNICO
            ),
            albumes.map { AlbumEsperado(it.name, it.songCount) },
        )
    }

    // ---------- Persistencia / duplicados ----------

    @Test
    fun reimportar_la_misma_uri_no_crea_duplicados_ni_duplicos_en_resumenes() = runBlocking {
        val antes = repo.observeArtistSummaries().first().first { it.name == "Queen" }.songCount
        val nuevas = repo.addSongs(
            listOf(
                song("uri-1", "Radio Ga Ga", "Queen", "The Works"),
            ),
        )
        assertEquals(0, nuevas) // IGNORE: no es nueva
        val despues = repo.observeArtistSummaries().first().first { it.name == "Queen" }.songCount
        assertEquals(antes, despues)
        assertEquals(7, repo.searchSongs("").size) // nada se duplicó
    }

    // ---------- Orden (FASE 8.3) ----------

    @Test
    fun ordenes_sql_coinciden_con_el_comparador_puro() = runBlocking {
        // PARIDAD: la orden SQL (observeLibrary(sort)) y el orden en memoria
        // del comparador dominio (toComparator, usado en los resultados de
        // búsqueda) deben producir LA MISMA secuencia con los mismos datos.
        for (sort in SongSort.entries) {
            val viaSql = repo.observeLibrary(sort).first().map { it.uri }
            val viaComparador = repo.searchSongs("").sortedWith(sort.toComparator()).map { it.uri }
            assertEquals("orden $sort", viaComparador, viaSql)
        }
    }

    @Test
    fun orden_por_artista_coloca_desconocidos_al_final() = runBlocking {
        val viaSql = repo.observeLibrary(SongSort.ARTIST_ASC).first().map { it.uri }
        // Barricada (u6 "zeta..." antes que u5 "Ángel..." — tilde sin plegar);
        // grupo Queen/queen bajo NOCASE es UNO: por título Another(u3), Hot(u2), Radio(u1);
        // los NULL (artist null) van al final:
        assertEquals(
            listOf("uri-6", "uri-5", "uri-3", "uri-2", "uri-1", "uri-7", "uri-4"),
            viaSql,
        )
    }

    private data class ArtistaEsperado(val name: String?, val songCount: Int)
    private data class AlbumEsperado(val name: String?, val songCount: Int)

    companion object {
        const val FECHA_BASE = 1_000_000_000L
        const val FECHA_VIEJA = 100_000L
    }
}
