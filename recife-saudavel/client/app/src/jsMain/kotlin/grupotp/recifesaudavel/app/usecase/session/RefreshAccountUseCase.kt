package grupotp.recifesaudavel.app.usecase.session

import grupotp.recifesaudavel.app.domain.auth.AuthResult
import grupotp.recifesaudavel.app.gateway.auth.AuthGateway
import grupotp.recifesaudavel.app.gateway.local.LocalAccountGateway
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount

/**
 * Catches the profile saved on the device up with the server, so changes made there (or fields
 * added by a newer server) reach this device without a new sign-in. Offline-first: when the
 * server cannot be reached or refuses the session, the saved profile stays as it is.
 */
class RefreshAccountUseCase(
    private val gateway: AuthGateway,
    private val accounts: LocalAccountGateway,
) {
    /**
     * @return the updated account when the server holds something different, null otherwise.
     */
    suspend operator fun invoke(account: AuthenticatedAccount): AuthenticatedAccount? {
        if (account.sessionToken.isBlank()) {
            return null
        }
        val fresh = when (val result = gateway.profile(account.sessionToken)) {
            is AuthResult.Success -> result.account
            is AuthResult.Failed -> return null
        }
        val merged = fresh.copy(sessionToken = account.sessionToken)
        if (merged == account) {
            return null
        }
        runCatching { accounts.save(merged) }
        return merged
    }
}
