package grupotp.medcall.app.gateway.account

import grupotp.medcall.app.domain.RemoteResult
import grupotp.medcall.common.account.AccountProfile
import grupotp.medcall.common.account.ChangePasswordRequest
import grupotp.medcall.common.account.UpdateConsentsRequest
import grupotp.medcall.common.account.UpdateProfileRequest
import grupotp.medcall.common.security.SecretString

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
