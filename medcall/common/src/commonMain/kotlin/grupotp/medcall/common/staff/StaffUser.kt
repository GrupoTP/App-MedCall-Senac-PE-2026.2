package grupotp.medcall.common.staff

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** A staff account as listed to the people who manage it. */
@Serializable
data class StaffUser(
    val id: Int,
    val fullName: String,
    val email: String,
    val role: StaffRole,
    val clinicId: Int? = null,
    val active: Boolean,
    val mustChangePassword: Boolean,
    val createdAt: Instant,
    val lastLoginAt: Instant? = null,
)

