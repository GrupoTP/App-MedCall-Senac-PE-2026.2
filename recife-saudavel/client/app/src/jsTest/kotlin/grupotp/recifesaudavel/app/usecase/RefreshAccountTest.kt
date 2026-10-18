package grupotp.recifesaudavel.app.usecase

import grupotp.recifesaudavel.common.account.Phone
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.auth.RegisterAccountRequest
import grupotp.recifesaudavel.common.auth.SignInRequest
import grupotp.recifesaudavel.common.security.SecretString
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RefreshAccountTest {
    private val saved = AuthenticatedAccount(
        id = 1,
        fullName = "Poly",
        socialName = "",
        email = "poly@email.com",
        sessionToken = SecretString("token-do-aparelho"),
    )

    @Test
    fun serverChangesReachTheDeviceAndKeepTheLocalToken() = runTest {
        // Arrange
        val server = saved.copy(phone = Phone.parse("(81) 99123-4567"), sessionToken = SecretString.EMPTY)
        val local = MemoryAccounts(saved)
        val refresh = RefreshAccount(ProfileGateway(AuthResult.Success(server)), local)

        // Act
        val result = refresh(saved)

        // Assert
        val expected = saved.copy(phone = Phone.parse("(81) 99123-4567"))
        assertEquals(expected, result)
        assertEquals(expected, local.stored)
    }

    @Test
    fun nothingChangesWhenTheServerHoldsTheSameProfile() = runTest {
        // Arrange
        val local = MemoryAccounts(saved)
        val refresh = RefreshAccount(ProfileGateway(AuthResult.Success(saved.copy(sessionToken = SecretString.EMPTY))), local)

        // Act
        val result = refresh(saved)

        // Assert
        assertNull(result)
        assertEquals(saved, local.stored)
    }

    @Test
    fun offlineOrExpiredSessionKeepsTheSavedProfile() = runTest {
        // Arrange
        val local = MemoryAccounts(saved)

        // Act
        val offline = RefreshAccount(ProfileGateway(AuthResult.Failed(AuthError.Offline)), local)(saved)
        val expired = RefreshAccount(ProfileGateway(AuthResult.Failed(AuthError.SessionExpired)), local)(saved)

        // Assert
        assertNull(offline)
        assertNull(expired)
        assertEquals(saved, local.stored)
    }

    private class ProfileGateway(private val result: AuthResult) : AuthGateway {
        override suspend fun signIn(request: SignInRequest): AuthResult = error("not used")

        override suspend fun register(request: RegisterAccountRequest): AuthResult = error("not used")

        override suspend fun requestPasswordRecovery(email: String): AuthResult = error("not used")

        override suspend fun profile(token: SecretString): AuthResult = result
    }

    private class MemoryAccounts(var stored: AuthenticatedAccount?) : LocalAccountGateway {
        override suspend fun save(account: AuthenticatedAccount) {
            stored = account
        }

        override suspend fun load(): AuthenticatedAccount? = stored

        override suspend fun clear() {
            stored = null
        }
    }
}
