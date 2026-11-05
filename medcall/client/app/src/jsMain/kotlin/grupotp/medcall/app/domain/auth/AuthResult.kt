package grupotp.medcall.app.domain.auth

import grupotp.medcall.common.auth.AuthenticatedAccount

sealed interface AuthResult {
    data class Success(val account: AuthenticatedAccount) : AuthResult

    data class Failed(val error: AuthError) : AuthResult
}
