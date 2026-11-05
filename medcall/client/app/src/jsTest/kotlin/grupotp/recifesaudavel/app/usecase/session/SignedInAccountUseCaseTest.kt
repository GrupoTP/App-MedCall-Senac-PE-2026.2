package grupotp.recifesaudavel.app.usecase.session

import grupotp.recifesaudavel.app.fake.MemoryLocalAccounts
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class SignedInAccountUseCaseTest {
    @Test
    fun rememberAndLoadKeepTheRegistrationProfileWithoutThePassword() = runTest {
        // Arrange
        val account = AuthenticatedAccount(
            id = 1,
            fullName = "Maria das Graças Silva",
            socialName = "Graça",
            email = "maria@email.com",
        )
        val accounts = MemoryLocalAccounts()
        val remember = RememberSignedInAccountUseCase(accounts)
        val load = LoadSignedInAccountUseCase(accounts)

        // Act
        remember(account)
        val loaded = load()

        // Assert
        assertEquals(account, loaded)
        assertFalse(Json.encodeToString(account).contains("password"))
    }

    @Test
    fun loadReturnsNothingBeforeTheFirstSignIn() = runTest {
        // Arrange
        val load = LoadSignedInAccountUseCase(MemoryLocalAccounts())

        // Act
        val loaded = load()

        // Assert
        assertNull(loaded)
    }
}
