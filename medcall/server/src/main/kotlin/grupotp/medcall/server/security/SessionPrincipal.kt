package grupotp.medcall.server.security

/** The signed-in person of a request, set by the JWT provider once its session is found open. */
data class SessionPrincipal(
    val userId: Int,
    val sessionId: Int,
)

/**
 * A signed-in staff member. [role] and [clinicId] are read from the database on every request, so
 * a change of role or a switched-off account takes effect at once.
 */
data class StaffPrincipal(
    val staffId: Int,
    val sessionId: Int,
    val role: grupotp.medcall.common.staff.StaffRole,
    val clinicId: Int?,
    val fullName: String,
    val mustChangePassword: Boolean,
)
