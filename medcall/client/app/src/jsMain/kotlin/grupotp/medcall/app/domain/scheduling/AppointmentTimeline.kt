package grupotp.medcall.app.domain.scheduling

import grupotp.medcall.common.scheduling.Appointment
import grupotp.medcall.common.scheduling.AppointmentStatus
import kotlin.time.Instant

/**
 * Places appointments in time by the real moment they start, each through its own clinic's zone,
 * so appointments in different zones sort and expire correctly.
 */
object AppointmentTimeline {
    /** Soonest first. */
    fun soonestFirst(appointments: List<Appointment>): List<Appointment> =
        appointments.sortedBy { it.startInstant() }

    /** Appointments not cancelled and not yet past, soonest first. */
    fun upcoming(appointments: List<Appointment>, now: Instant): List<Appointment> =
        soonestFirst(appointments.filter { it.status != AppointmentStatus.Cancelled && it.startInstant() >= now })
}
