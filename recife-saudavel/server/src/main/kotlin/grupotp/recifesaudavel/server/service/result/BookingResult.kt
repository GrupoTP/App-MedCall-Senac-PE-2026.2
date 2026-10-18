package grupotp.recifesaudavel.server.service.result

import grupotp.recifesaudavel.common.scheduling.Appointment
import grupotp.recifesaudavel.server.domain.ServiceError

sealed interface BookingResult {
    data class Created(val appointment: Appointment) : BookingResult

    /** The request is invalid: unknown specialty, professional from another specialty, closed slot. */
    data class Rejected(val error: ServiceError) : BookingResult

    /** Valid request, but someone took the slot first. */
    data object Conflict : BookingResult
}
