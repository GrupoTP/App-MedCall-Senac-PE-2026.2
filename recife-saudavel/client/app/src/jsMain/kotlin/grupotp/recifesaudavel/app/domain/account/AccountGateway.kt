package grupotp.recifesaudavel.app.domain.account

import grupotp.recifesaudavel.app.domain.RemoteResult
import grupotp.recifesaudavel.common.account.AccountProfile
import grupotp.recifesaudavel.common.account.ChangePasswordRequest
import grupotp.recifesaudavel.common.account.UpdateConsentsRequest
import grupotp.recifesaudavel.common.account.UpdateProfileRequest
import grupotp.recifesaudavel.common.security.SecretString

/**
 * Settings API of the server. Every call needs the session token and the network.
 */
interface AccountGateway {
    suspend fun profile(token: SecretString): RemoteResult<AccountProfile>

    suspend fun update(token: SecretString, request: UpdateProfileRequest): RemoteResult<AccountProfile>

    suspend fun updateConsents(token: SecretString, request: UpdateConsentsRequest): RemoteResult<AccountProfile>

    suspend fun changePassword(token: SecretString, request: ChangePasswordRequest): RemoteResult<AccountProfile>

    suspend fun requestDeletion(token: SecretString): RemoteResult<AccountProfile>

    /** Asks the server to forget [token]. */
    suspend fun signOut(token: SecretString): RemoteResult<Unit>
}
