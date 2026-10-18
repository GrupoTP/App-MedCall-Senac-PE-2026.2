package grupotp.recifesaudavel.app.fake

import grupotp.recifesaudavel.app.domain.RemoteError
import grupotp.recifesaudavel.app.domain.RemoteResult
import grupotp.recifesaudavel.app.domain.account.AccountGateway
import grupotp.recifesaudavel.common.account.AccountProfile
import grupotp.recifesaudavel.common.account.ChangePasswordRequest
import grupotp.recifesaudavel.common.account.UpdateConsentsRequest
import grupotp.recifesaudavel.common.account.UpdateProfileRequest
import grupotp.recifesaudavel.common.security.SecretString

/**
 * Answers every profile call with [result] and records what it was sent.
 */
internal class FakeAccountGateway(
    private val result: RemoteResult<AccountProfile> = RemoteResult.Failed(RemoteError.Offline),
) : AccountGateway {
    val tokens = mutableListOf<String>()
    var passwordRequest: ChangePasswordRequest? = null
    var consentRequest: UpdateConsentsRequest? = null
    var signedOut: String? = null
    var signOutResult: RemoteResult<Unit> = RemoteResult.Success(Unit)

    override suspend fun profile(token: SecretString): RemoteResult<AccountProfile> = result.also { tokens += token.value }

    override suspend fun update(token: SecretString, request: UpdateProfileRequest): RemoteResult<AccountProfile> =
        result.also { tokens += token.value }

    override suspend fun updateConsents(
        token: SecretString,
        request: UpdateConsentsRequest,
    ): RemoteResult<AccountProfile> = result.also {
        tokens += token.value
        consentRequest = request
    }

    override suspend fun changePassword(
        token: SecretString,
        request: ChangePasswordRequest,
    ): RemoteResult<AccountProfile> = result.also {
        tokens += token.value
        passwordRequest = request
    }

    override suspend fun requestDeletion(token: SecretString): RemoteResult<AccountProfile> =
        result.also { tokens += token.value }

    override suspend fun signOut(token: SecretString): RemoteResult<Unit> {
        if (signOutResult is RemoteResult.Success) {
            signedOut = token.value
        }
        return signOutResult
    }
}
