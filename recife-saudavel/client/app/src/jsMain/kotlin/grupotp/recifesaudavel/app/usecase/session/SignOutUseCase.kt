package grupotp.recifesaudavel.app.usecase.session

import grupotp.recifesaudavel.app.domain.session.LocalAccountGateway
import grupotp.recifesaudavel.app.domain.session.PendingSignOutQueue
import grupotp.recifesaudavel.common.security.SecretString

/**
 * Forgets the signed-in profile and its appointments on this device.
 */
class SignOutUseCase(
    private val accounts: LocalAccountGateway,
    private val pendingSignOuts: PendingSignOutQueue,
) {
    /**
     * @param token session to revoke on the server too. A failure never stops the device from
     * forgetting the profile; when the server could not be reached the token waits in
     * [PendingSignOutQueue] and is revoked as soon as the device is online again.
     */
    suspend operator fun invoke(token: SecretString? = null) {
        if (token != null && !token.isBlank()) {
            runCatching { pendingSignOuts.revokeOrQueue(token) }
        }
        accounts.clear()
    }
}
