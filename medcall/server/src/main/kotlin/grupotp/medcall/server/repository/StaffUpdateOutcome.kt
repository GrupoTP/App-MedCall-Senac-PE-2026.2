package grupotp.medcall.server.repository

import grupotp.medcall.common.staff.StaffUser
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/** Changes to a clinic user, checked together in one transaction. */
sealed interface StaffUpdateOutcome {
    data class Updated(val user: StaffUser) : StaffUpdateOutcome
    data object NotFound : StaffUpdateOutcome
    data object LastAdmin : StaffUpdateOutcome
}

