package grupotp.recifesaudavel.server.service

import grupotp.recifesaudavel.common.auth.AuthenticatedAccount

sealed interface RegistrationResult {
    data class Created(val account: AuthenticatedAccount) : RegistrationResult
    data class Rejected(val error: ServiceError) : RegistrationResult
}
