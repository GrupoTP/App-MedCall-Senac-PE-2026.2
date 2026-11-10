package grupotp.medcall.common.scheduling

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Where an appointment stands. A reschedule keeps [Confirmed] and moves the date and time
 * ([Appointment.rescheduled] tells the patient it moved).
 *
 * Transitions (checked on the server):
 * - [Pending] → [Confirmed], [Declined] by the clinic; → [Cancelled] by the patient.
 * - [Confirmed] → [Declined] by the clinic or [Cancelled] by the patient, before it starts;
 *   → [Attended] or [NoShow] by the clinic, after it starts.
 * - [Attended] and [NoShow] → [Confirmed] when the clinic undoes the mark within 24 hours.
 * - [Declined] and [Cancelled] are final and free the slot.
 */
@Serializable
enum class AppointmentStatus {
    @SerialName("pending")
    Pending,

    @SerialName("confirmed")
    Confirmed,

    @SerialName("declined")
    Declined,

    @SerialName("cancelled")
    Cancelled,

    @SerialName("attended")
    Attended,

    @SerialName("no_show")
    NoShow,
    ;

    /** True while the appointment holds its slot, so nobody else can book it. */
    val holdsSlot: Boolean get() = this != Declined && this != Cancelled
}
