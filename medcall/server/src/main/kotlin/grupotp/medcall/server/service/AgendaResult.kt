package grupotp.medcall.server.service

import grupotp.medcall.common.clinic.ClinicAppointment
import grupotp.medcall.server.domain.ServiceError

/** Answer of an agenda action. */
sealed interface AgendaResult {
    data class Done(val appointment: ClinicAppointment) : AgendaResult
    data object NotFound : AgendaResult
    data class Rejected(val error: ServiceError) : AgendaResult
    /** Someone changed the appointment since it was read: the screen should reload it. */
    data object Changed : AgendaResult
    data object SlotTaken : AgendaResult
}

