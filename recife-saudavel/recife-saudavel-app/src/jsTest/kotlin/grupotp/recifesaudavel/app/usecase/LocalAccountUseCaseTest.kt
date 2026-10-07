package grupotp.recifesaudavel.app.usecase

import grupotp.recifesaudavel.common.AuthenticatedAccount
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class LocalAccountUseCaseTest {
    @Test
    fun rememberAndLoadKeepTheRegistrationProfileWithoutThePassword() = runTest {
        // Arrange
        val account = AuthenticatedAccount(
            id = "1",
            fullName = "Maria das Graças Silva",
            socialName = "Graça",
            email = "maria@email.com",
        )
        val gateway = FakeLocalAccountGateway()
        val remember = RememberSignedInAccount(gateway)
        val load = LoadSignedInAccount(gateway)

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
        val load = LoadSignedInAccount(FakeLocalAccountGateway())

        // Act
        val loaded = load()

        // Assert
        assertNull(loaded)
    }
}

private class FakeLocalAccountGateway : LocalAccountGateway {
    private var stored: AuthenticatedAccount? = null

    override suspend fun save(account: AuthenticatedAccount) {
        stored = account
    }

    override suspend fun load(): AuthenticatedAccount? = stored
}
