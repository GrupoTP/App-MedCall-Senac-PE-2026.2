package grupotp.recifesaudavel.app.domain.account

import grupotp.recifesaudavel.app.domain.scheduling.SchedulingResult
import grupotp.recifesaudavel.common.account.AccountProfile
import grupotp.recifesaudavel.common.account.ChangePasswordRequest
import grupotp.recifesaudavel.common.account.UpdateConsentsRequest
import grupotp.recifesaudavel.common.account.UpdateProfileRequest
import grupotp.recifesaudavel.common.security.SecretString

/**
 * Settings API of the server. Every call needs the session token and the network.
 */
interface AccountGateway {
    suspend fun profile(token: SecretString): SchedulingResult<AccountProfile>

    suspend fun update(token: SecretString, request: UpdateProfileRequest): SchedulingResult<AccountProfile>

    suspend fun updateConsents(token: SecretString, request: UpdateConsentsRequest): SchedulingResult<AccountProfile>

    suspend fun changePassword(token: SecretString, request: ChangePasswordRequest): SchedulingResult<AccountProfile>

    suspend fun requestDeletion(token: SecretString): SchedulingResult<AccountProfile>

    /** Asks the server to forget [token]. */
    suspend fun signOut(token: SecretString): SchedulingResult<Unit>
}
