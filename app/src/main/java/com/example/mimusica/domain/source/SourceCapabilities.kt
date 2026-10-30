package com.example.mimusica.domain.source

/**
 * Capacidades declaradas de una fuente musical (FASE 7.0).
 *
 * Regla §22.2 del proyecto: las capacidades describen qué AUTORIZA la fuente
 * a nivel general, pero NO habilitan por sí solas la descarga de una pista
 * concreta: la autorización del recurso individual se evalúa con
 * [DownloadAuthorization] a partir de los metadatos del fichero concreto.
 */
data class SourceCapabilities(
    val sourceId: String,
    /** La fuente ofrece búsqueda oficial. */
    val canSearch: Boolean,
    /** La fuente declara una vía oficial de descarga (a nivel de servicio). */
    val canDownload: Boolean,
    /**
     * Atribución exigida por la fuente/licencias (ej. Jamendo: API ToU).
     * No null = la app DEBE mostrar esta atribución al usar la fuente.
     */
    val attributionText: String?,
    /** Licencias presentes en el catálogo (informativo; la valide la pista). */
    val licenseSummary: String? = null,
)
