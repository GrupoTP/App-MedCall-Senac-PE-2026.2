package grupotp.medcall.app.domain.session

import grupotp.medcall.app.domain.RemoteError
import grupotp.medcall.app.domain.RemoteResult
import grupotp.medcall.app.fake.FakeAccountGateway
import grupotp.medcall.app.fake.MemoryPendingSignOuts
import grupotp.medcall.common.security.SecretString
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
