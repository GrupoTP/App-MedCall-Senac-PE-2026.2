package grupotp.medcall.server.repository

import grupotp.medcall.common.staff.StaffUser

/** A staff account with what only the server reads. */
data class StoredStaff(
    val user: StaffUser,
    val passwordHash: String,
    val clinicName: String?,
)

