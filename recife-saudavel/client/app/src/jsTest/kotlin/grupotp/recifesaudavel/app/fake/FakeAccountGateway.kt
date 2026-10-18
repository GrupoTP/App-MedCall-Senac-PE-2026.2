package grupotp.recifesaudavel.app.fake

import grupotp.recifesaudavel.app.domain.account.AccountGateway
import grupotp.recifesaudavel.app.domain.scheduling.SchedulingError
import grupotp.recifesaudavel.app.domain.scheduling.SchedulingResult
import grupotp.recifesaudavel.common.account.AccountProfile
import grupotp.recifesaudavel.common.account.ChangePasswordRequest
import grupotp.recifesaudavel.common.account.UpdateConsentsRequest
import grupotp.recifesaudavel.common.account.UpdateProfileRequest
import grupotp.recifesaudavel.common.security.SecretString

/**
 * Answers every profile call with [result] and records what it was sent.
 */
internal class FakeAccountGateway(
    private val result: SchedulingResult<AccountProfile> = SchedulingResult.Failed(SchedulingError.Offline),
) : AccountGateway {
    val tokens = mutableListOf<String>()
    var passwordRequest: ChangePasswordRequest? = null
    var consentRequest: UpdateConsentsRequest? = null
    var signedOut: String? = null
    var signOutResult: SchedulingResult<Unit> = SchedulingResult.Success(Unit)

    override suspend fun profile(token: SecretString): SchedulingResult<AccountProfile> = result.also { tokens += token.value }

    override suspend fun update(token: SecretString, request: UpdateProfileRequest): SchedulingResult<AccountProfile> =
        result.also { tokens += token.value }

    override suspend fun updateConsents(
        token: SecretString,
        request: UpdateConsentsRequest,
    ): SchedulingResult<AccountProfile> = result.also {
        tokens += token.value
        consentRequest = request
    }

    override suspend fun changePassword(
        token: SecretString,
        request: ChangePasswordRequest,
    ): SchedulingResult<AccountProfile> = result.also {
        tokens += token.value
        passwordRequest = request
    }

    override suspend fun requestDeletion(token: SecretString): SchedulingResult<AccountProfile> =
        result.also { tokens += token.value }

    override suspend fun signOut(token: SecretString): SchedulingResult<Unit> {
        if (signOutResult is SchedulingResult.Success) {
            signedOut = token.value
        }
        return signOutResult
    }
}
