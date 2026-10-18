package grupotp.recifesaudavel.app.usecase

import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
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
    fun signOutForgetsTheProfile() = runTest {
        // Arrange
        val gateway = FakeLocalAccountGateway()
        RememberSignedInAccount(gateway)(AuthenticatedAccount(id = "1", fullName = "Ana", socialName = "", email = "ana@email.com"))

        // Act
        SignOut(gateway, PendingSignOuts(NoRemote, MemoryPending()))()

        // Assert
        assertNull(LoadSignedInAccount(gateway)())
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

    override suspend fun clear() {
        stored = null
    }
}

private object NoRemote : AccountGateway {
    override suspend fun profile(token: String) = error("not used")

    override suspend fun update(token: String, request: grupotp.recifesaudavel.common.account.UpdateProfileRequest) = error("not used")

    override suspend fun updateConsents(token: String, request: grupotp.recifesaudavel.common.account.UpdateConsentsRequest) =
        error("not used")

    override suspend fun changePassword(token: String, request: grupotp.recifesaudavel.common.account.ChangePasswordRequest) =
        error("not used")

    override suspend fun requestDeletion(token: String) = error("not used")

    override suspend fun signOut(token: String): SchedulingResult<Unit> = error("not used")
}

internal class MemoryPending : PendingSignOutGateway {
    var tokens: List<String> = emptyList()

    override suspend fun loadPendingSignOuts(): List<String> = tokens

    override suspend fun savePendingSignOuts(tokens: List<String>) {
        this.tokens = tokens
    }
}
