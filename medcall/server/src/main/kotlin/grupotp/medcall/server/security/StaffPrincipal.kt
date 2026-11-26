package grupotp.medcall.server.security

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

