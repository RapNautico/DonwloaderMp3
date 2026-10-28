package com.example.mimusica.domain.download

/**
 * Política de cola FIFO con límite de concurrencia (FASE 7.0).
 *
 * Clase PURA y determinista (sin coroutines, sin red, sin Android): decide
 * QUÉ descargas deben arrancarse y libera/ocupa los 2 slots. El arranque
 * real lo ejecuta el motor en FASE 7.2; aquí solo la política.
 *
 * Terminología:
 * - activo: slot ocupado por una descarga en curso (Downloading/Processing).
 * - pendiente: en espera FIFO hasta que se libere un slot.
 *
 * Ciclo de vida: enqueue → dispatch (frees-starts) → confirmStarted…
 * Un intento fallido/cancelado NO reintenta dentro de la cola: una
 * "reintento" significa enqueue de una NUEVA operación (ver DownloadState).
 */
class DownloadQueue(private val maxConcurrent: Int = DEFAULT_MAX_CONCURRENT) {

    init {
        require(maxConcurrent >= 1) { "maxConcurrent debe ser >= 1" }
    }

    private val pending = ArrayDeque<Long>()
    private val active = LinkedHashSet<Long>()

    val activeIds: Set<Long> get() = active.toSet()
    val pendingIds: List<Long> get() = pending.toList()
    val freeSlots: Int get() = (maxConcurrent - active.size).coerceAtLeast(0)

    /**
     * Añade una operación al final de la cola FIFO.
     * Devuelve false si (a) ya está pendiente o activa, (b) terminó
     * anteriormente en esta cola (el reintento es una operación nueva).
     */
    fun enqueue(id: Long): Boolean {
        if (active.contains(id) || pending.contains(id) || finishedIds.contains(id)) return false
        pending.addLast(id)
        return true
    }

    /**
     * Arranca las siguientes operaciones FIFO mientras haya slots libres.
     * Devuelve los ids que DEBEN arrancarse (mover a activos), en orden FIFO.
     */
    fun dispatch(): List<Long> {
        val toStart = mutableListOf<Long>()
        while (freeSlots > 0 && pending.isNotEmpty()) {
            val id = pending.removeFirst()
            active.add(id)
            toStart.add(id)
        }
        return toStart
    }

    /** Operación terminada (éxito o fallo definitivo): libera su slot. */
    fun finish(id: Long): Boolean = active.remove(id).also { if (it) finishedIds.add(id) }

    /**
     * Cancelación pendiente: si estaba pendiente, se elimina de la cola;
     * si estaba activa, el caller debe cancelar el trabajo en el motor y
     * llamar a `finish(id)` para liberar el slot (la cola no lo saca solos).
     * Si setFinished=true (por ítem terminal) libera al instante.
     */
    fun cancel(id: Long): CancelTarget = when {
        pending.contains(id) -> {
            pending.remove(id); finishedIds.add(id); CancelTarget.PENDING_REMOVED
        }
        active.contains(id) -> CancelTarget.ACTIVE_NEEDS_ENGINE_CANCEL
        else -> CancelTarget.NONE
    }

    fun clearFinished() = finishedIds.clear()

    /** Rastreo local de ids terminados para prohibir reenqueue fantasma. */
    private val finishedIds = mutableSetOf<Long>()

    enum class CancelTarget { PENDING_REMOVED, ACTIVE_NEEDS_ENGINE_CANCEL, NONE }

    companion object {
        /** Política documentada (PLAN FASE 7): FIFO con máximo 2 concurrentes. */
        const val DEFAULT_MAX_CONCURRENT = 2
    }
}
