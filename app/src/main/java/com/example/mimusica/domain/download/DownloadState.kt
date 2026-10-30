package com.example.mimusica.domain.download

/**
 * Estados de una descarga (terminología de DESIGN.md §3.4 y ciclo §3.2).
 *
 * Diagrama de transiciones VÁLIDAS:
 *
 * ```
 * Pending ──> Analyzing ──> Downloading ──> Processing ──> Completed
 *    │            │              │              │
 *    │            ├──> Cancelled └──> Cancelled └──> Failed
 *    │            └──> Failed
 *    └──> Cancelled
 * ```
 *
 * - `Completed` es terminal absoluto (sin salida).
 * - `Failed` y `Cancelled` son TERMINALES: no transicionan a `Completed`
 *   (nadie se completa desde un fallo/cancelación sin transición legítima).
 *   "Reintentar" se modela como UNA NUEVA operación en `DownloadQueue`
 *   (nuevo intento `Pending`), nunca como un salto de estado del ítem viejo.
 */
enum class DownloadState {
    Pending,
    Analyzing,
    Downloading,
    Processing,
    Completed,
    Failed,
    Cancelled,
    ;

    /** Transiciones legítimas SALIENDO de este estado (vacío = terminal). */
    fun allowedTransitions(): Set<DownloadState> = TRANSITIONS.getValue(this)

    fun canTransitionTo(target: DownloadState): Boolean = target in allowedTransitions()

    val isTerminal: Boolean get() = allowedTransitions().isEmpty()

    companion object {
        /**
         * Transiciones VÁLIDAS (requisito §3 del enunciado):
         * - Pending → Analyzing | Cancelled (el usuario se arrepiente en espera).
         * - Analyzing → Downloading | Failed | Cancelled.
         * - Downloading → Processing | Failed | Cancelled.
         * - Processing → Completed | Failed.
         * - Completed/Failed/Cancelled: sin salida (terminales).
         */
        private val TRANSITIONS: Map<DownloadState, Set<DownloadState>> = mapOf(
            Pending to setOf(Analyzing, Cancelled),
            Analyzing to setOf(Downloading, Failed, Cancelled),
            Downloading to setOf(Processing, Failed, Cancelled),
            Processing to setOf(Completed, Failed),
            Completed to emptySet(),
            Failed to emptySet(),
            Cancelled to emptySet(),
        )

        /**
         * Transición validada. Devuelve [Result.failure] con
         * [IllegalStateException] para transiciones no documentadas: la
         * máquina de estados JAMÁS salta a un estado no autorizado.
         */
        fun transition(from: DownloadState, to: DownloadState): Result<DownloadState> =
            if (from.canTransitionTo(to)) Result.success(to)
            else Result.failure(
                IllegalStateException(
                    "Transición inválida: $from → $to (permitidas: ${from.allowedTransitions()})",
                ),
            )
    }
}
