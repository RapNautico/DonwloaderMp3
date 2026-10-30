package com.example.mimusica.domain.model

/**
 * Orden del listado de la biblioteca (FASE 8.3).
 *
 * La Biblioteca COMPLETA se ordena en SQL (SongDao, consultas dedicadas por
 * orden, todas con desempate estable por `uri`); este enum también expone un
 * comparador puro [toComparator] usado SOLO en Domain/Presentation para ordenar
 * el SUBCONJUNTO ya filtrado por una búsqueda (nunca la biblioteca entera).
 */
enum class SongSort {
    TITLE_ASC,
    TITLE_DESC,
    ARTIST_ASC,
    ALBUM_ASC,
    DATE_ADDED_DESC,
    DATE_ADDED_ASC,
    DURATION_DESC,
}

/**
 * Comparador equivalente al ORDER BY de SQL para cada orden (FASE 8.3):
 *
 * - Textos con plegado ASCII (igual que `COLLATE NOCASE` de SQLite: la tilde
 *   NO se pliega; función [asciiNoCase], sin dependencia del locale del
 *   dispositivo).
 * - Los `null` (artista/álbum desconocido) van AL FINAL, como en los
 *   resúmenes de FASE 8.2 (decisión D8.3).
 * - Desempate SIEMPRE por `uri` ASC, idéntico a las consultas SQL.
 */
fun SongSort.toComparator(): Comparator<Song> {
    val uriTieBreak: Comparator<Song> = compareBy { it.uri }
    return when (this) {
        SongSort.TITLE_ASC -> noCase { it.title }.then(uriTieBreak)
        SongSort.TITLE_DESC -> noCase { it.title }.reversed().then(uriTieBreak)
        SongSort.ARTIST_ASC -> noCase { it.artist }
            .then(noCase { it.title })
            .then(uriTieBreak)
        SongSort.ALBUM_ASC -> noCase { it.album }
            .then(noCase { it.title })
            .then(uriTieBreak)
        SongSort.DATE_ADDED_DESC -> compareByDescending<Song> { it.dateAddedMs }.then(uriTieBreak)
        SongSort.DATE_ADDED_ASC -> compareBy<Song> { it.dateAddedMs }.then(uriTieBreak)
        SongSort.DURATION_DESC -> compareByDescending<Song> { it.durationMs }.then(uriTieBreak)
    }
}

/**
 * Comparador por texto con plegado ASCII-FIJO (A-Z → a-z), idéntico a
 * `COLLATE NOCASE` de SQLite: la tilde NO se pliega y los `null` van AL FINAL.
 */
private inline fun noCase(crossinline selector: (Song) -> String?): Comparator<Song> =
    Comparator { a, b ->
        val ka = selector(a)?.foldAsciiCase()
        val kb = selector(b)?.foldAsciiCase()
        when {
            ka == kb -> 0
            ka == null -> 1
            kb == null -> -1
            else -> ka.compareTo(kb)
        }
    }

private fun String.foldAsciiCase(): String = buildString {
    for (c in this@foldAsciiCase) {
        append(if (c in 'A'..'Z') c + 32 else c)
    }
}
