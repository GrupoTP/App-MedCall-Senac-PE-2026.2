package grupotp.medcall.server.repository

/** Everything a dashboard needs, read once per request. */
data class KpiFacts(
    val clinics: List<ClinicFact>,
    val professionals: List<ProfessionalFact>,
    val appointments: List<AppointmentFact>,
    /** Events of [appointments], oldest first. */
    val events: Map<Int, List<EventFact>>,
)

