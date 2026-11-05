package grupotp.recifesaudavel.app.usecase.session

import grupotp.recifesaudavel.app.domain.RemoteError
import grupotp.recifesaudavel.app.domain.RemoteResult
import grupotp.recifesaudavel.app.domain.session.PendingSignOutQueue
import grupotp.recifesaudavel.app.fake.FakeAccountGateway
import grupotp.recifesaudavel.app.fake.MemoryLocalAccounts
import grupotp.recifesaudavel.app.fake.MemoryPendingSignOuts
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.security.SecretString
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SignOutUseCaseTest {
    private val account = AuthenticatedAccount(
        id = 1,
        fullName = "Poly",
        socialName = "",
        email = "poly@email.com",
        sessionToken = SecretString("token"),
    )

    @Test
    fun signOutWithoutTokenOnlyForgetsTheProfile() = runTest {
        // Arrange
        val accounts = MemoryLocalAccounts(account)
        val gateway = FakeAccountGateway()
        val signOut = SignOutUseCase(accounts, PendingSignOutQueue(gateway, MemoryPendingSignOuts()))

        // Act
        signOut()

        // Assert
        assertNull(accounts.stored)
        assertNull(gateway.signedOut)
    }

    @Test
    fun signOutRevokesTheTokenAndForgetsTheDeviceCopy() = runTest {
        // Arrange
        val accounts = MemoryLocalAccounts(account)
        val gateway = FakeAccountGateway()
        val pending = MemoryPendingSignOuts()
        val signOut = SignOutUseCase(accounts, PendingSignOutQueue(gateway, pending))

        // Act
        signOut(account.sessionToken)

        // Assert
        assertEquals("token", gateway.signedOut)
        assertNull(accounts.stored)
        assertEquals(emptyList(), pending.tokens)
    }

    @Test
    fun offlineSignOutQueuesTheTokenAndFlushRevokesItOnceOnline() = runTest {
        // Arrange
        val accounts = MemoryLocalAccounts(account)
        val gateway = FakeAccountGateway()
        gateway.signOutResult = RemoteResult.Failed(RemoteError.Offline)
        val pending = MemoryPendingSignOuts()
        val queue = PendingSignOutQueue(gateway, pending)
        val signOut = SignOutUseCase(accounts, queue)
        val flush = FlushPendingSignOutsUseCase(queue)

        // Act
        signOut(account.sessionToken)
        flush()
        val stillQueued = pending.tokens
        gateway.signOutResult = RemoteResult.Success(Unit)
        flush()

        // Assert
        assertNull(accounts.stored)
        assertEquals(listOf("token"), stillQueued.map { it.value })
        assertEquals(emptyList(), pending.tokens)
        assertEquals("token", gateway.signedOut)
    }
}
