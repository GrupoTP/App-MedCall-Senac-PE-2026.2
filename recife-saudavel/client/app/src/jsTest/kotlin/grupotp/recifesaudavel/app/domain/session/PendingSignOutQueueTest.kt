package grupotp.recifesaudavel.app.domain.session

import grupotp.recifesaudavel.app.domain.RemoteError
import grupotp.recifesaudavel.app.domain.RemoteResult
import grupotp.recifesaudavel.app.fake.FakeAccountGateway
import grupotp.recifesaudavel.app.fake.MemoryPendingSignOuts
import grupotp.recifesaudavel.common.security.SecretString
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PendingSignOutQueueTest {
    @Test
    fun expiredSessionIsNotQueuedBecauseThereIsNothingLeftToRevoke() = runTest {
        // Arrange
        val gateway = FakeAccountGateway()
        gateway.signOutResult = RemoteResult.Failed(RemoteError.SessionExpired)
        val pending = MemoryPendingSignOuts()

        // Act
        PendingSignOutQueue(gateway, pending).revokeOrQueue(SecretString("token"))

        // Assert
        assertEquals(emptyList(), pending.tokens)
    }

    @Test
    fun flushStopsAtTheFirstTokenThatIsStillUnreachable() = runTest {
        // Arrange
        val gateway = FakeAccountGateway()
        gateway.signOutResult = RemoteResult.Failed(RemoteError.Offline)
        val pending = MemoryPendingSignOuts()
        val queue = PendingSignOutQueue(gateway, pending)
        queue.revokeOrQueue(SecretString("first"))
        queue.revokeOrQueue(SecretString("second"))

        // Act
        queue.flush()

        // Assert
        assertEquals(listOf("first", "second"), pending.tokens.map { it.value })
    }
}
