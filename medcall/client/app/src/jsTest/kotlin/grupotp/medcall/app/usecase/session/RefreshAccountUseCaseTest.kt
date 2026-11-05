package grupotp.recifesaudavel.app.usecase.session

import grupotp.recifesaudavel.app.domain.auth.AuthError
import grupotp.recifesaudavel.app.domain.auth.AuthResult
import grupotp.recifesaudavel.app.fake.FakeAuthGateway
import grupotp.recifesaudavel.app.fake.MemoryLocalAccounts
import grupotp.recifesaudavel.common.account.Phone
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.security.SecretString
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RefreshAccountUseCaseTest {
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
        val local = MemoryLocalAccounts(saved)
        val refresh = RefreshAccountUseCase(FakeAuthGateway(profileResult = AuthResult.Success(server)), local)

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
        val local = MemoryLocalAccounts(saved)
        val sameProfile = AuthResult.Success(saved.copy(sessionToken = SecretString.EMPTY))
        val refresh = RefreshAccountUseCase(FakeAuthGateway(profileResult = sameProfile), local)

        // Act
        val result = refresh(saved)

        // Assert
        assertNull(result)
        assertEquals(saved, local.stored)
    }

    @Test
    fun offlineOrExpiredSessionKeepsTheSavedProfile() = runTest {
        // Arrange
        val local = MemoryLocalAccounts(saved)
        val offline = RefreshAccountUseCase(FakeAuthGateway(profileResult = AuthResult.Failed(AuthError.Offline)), local)
        val expired = RefreshAccountUseCase(FakeAuthGateway(profileResult = AuthResult.Failed(AuthError.SessionExpired)), local)

        // Act
        val offlineResult = offline(saved)
        val expiredResult = expired(saved)

        // Assert
        assertNull(offlineResult)
        assertNull(expiredResult)
        assertEquals(saved, local.stored)
    }
}
