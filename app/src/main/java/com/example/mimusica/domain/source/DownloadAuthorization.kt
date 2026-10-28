package com.example.mimusica.domain.source

/**
 * Evaluación pura de la autorización de descarga de UN RECURSO concreto
 * (FASE 7.0, requisito "no considerar una capacidad general como prueba
 * suficiente para descargar cualquier pista").
 *
 * Las reglas son explícitas y testeables:
 * - La fuente no puede descargar → denegado por fuente.
 * - La fuente puede, pero el recurso dice que no → denegado por pista.
 * - La fuente puede pero el recurso no informa → desconocido (NO se asume
 *   permitido: conservador por diseño).
 * - Fuente puede && recurso informa true → concedido.
 */
enum class DownloadVerdict { GRANTED, DENIED_BY_SOURCE, DENIED_BY_TRACK, UNKNOWN }

fun evaluateDownloadAuthorization(
    capabilities: SourceCapabilities,
    track: TrackReference,
): DownloadVerdict = when {
    !capabilities.canDownload -> DownloadVerdict.DENIED_BY_SOURCE
    track.downloadAllowed != null -> if (track.downloadAllowed) DownloadVerdict.GRANTED
    else DownloadVerdict.DENIED_BY_TRACK
    else -> DownloadVerdict.UNKNOWN
}
