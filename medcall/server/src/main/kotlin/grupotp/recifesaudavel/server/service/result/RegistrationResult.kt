package grupotp.recifesaudavel.server.service.result

import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.server.domain.ServiceError

sealed interface RegistrationResult {
    data class Created(val account: AuthenticatedAccount) : RegistrationResult
    data class Rejected(val error: ServiceError) : RegistrationResult
}
