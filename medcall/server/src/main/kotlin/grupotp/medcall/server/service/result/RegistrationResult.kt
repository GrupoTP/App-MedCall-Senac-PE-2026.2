package grupotp.medcall.server.service.result

import grupotp.medcall.common.auth.AuthenticatedAccount
import grupotp.medcall.server.domain.ServiceError

sealed interface RegistrationResult {
    data class Created(val account: AuthenticatedAccount) : RegistrationResult
    data class Rejected(val error: ServiceError) : RegistrationResult
}
