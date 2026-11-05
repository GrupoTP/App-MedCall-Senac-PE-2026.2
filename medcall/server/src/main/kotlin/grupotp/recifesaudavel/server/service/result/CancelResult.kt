package grupotp.recifesaudavel.server.service.result

import grupotp.recifesaudavel.common.scheduling.Appointment

sealed interface CancelResult {
    data class Cancelled(val appointment: Appointment) : CancelResult

    /** No appointment with this id belongs to the patient. */
    data object NotFound : CancelResult

    /** Already cancelled, or confirmed and already started. */
    data object NotCancellable : CancelResult
}
