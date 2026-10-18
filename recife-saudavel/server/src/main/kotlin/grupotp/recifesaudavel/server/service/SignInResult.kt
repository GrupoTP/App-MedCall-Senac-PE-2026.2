package grupotp.recifesaudavel.server.service

import grupotp.recifesaudavel.common.auth.AuthenticatedAccount

sealed interface SignInResult {
    data class Authenticated(val account: AuthenticatedAccount) : SignInResult
    /** Unknown e-mail or wrong password; which one is not told. */
    data object Rejected : SignInResult
}
