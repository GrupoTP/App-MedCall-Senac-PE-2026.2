package grupotp.medcall.server.repository

/** What the events add to an appointment row. */
internal data class AppointmentExtras(
    val rescheduled: Boolean = false,
    val declineReason: String = "",
)

