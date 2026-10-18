package grupotp.recifesaudavel.app.usecase.pretriage

import grupotp.recifesaudavel.app.domain.RemoteError
import grupotp.recifesaudavel.app.domain.RemoteResult
import grupotp.recifesaudavel.app.domain.pretriage.PreTriageGateway
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.pretriage.PreTriage
import grupotp.recifesaudavel.common.pretriage.PreTriageForm
import grupotp.recifesaudavel.common.security.SecretString
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class PreTriageUseCaseTest {
    private val account = AuthenticatedAccount(
        id = 1,
        fullName = "Maria das Graças Silva",
        socialName = "Graça",
        email = "maria@email.com",
        sessionToken = SecretString("token-da-maria"),
    )

    @Test
    fun pendingFailureCountsAsNone() = runTest {
        // Arrange
        val gateway = FakePreTriageGateway(pending = RemoteResult.Failed(RemoteError.Offline))

        // Act
        val pending = LoadPendingPreTriageUseCase(gateway)(account)

        // Assert
        assertNull(pending)
        assertEquals("token-da-maria", gateway.lastToken)
    }

    @Test
    fun pendingReturnsTheLatestPreTriage() = runTest {
        // Arrange
        val latest = PreTriage(id = 1, createdAt = Instant.fromEpochMilliseconds(0), form = PreTriageForm(reason = "Tontura"))
        val gateway = FakePreTriageGateway(pending = RemoteResult.Success(latest))

        // Act
        val pending = LoadPendingPreTriageUseCase(gateway)(account)

        // Assert
        assertEquals(latest, pending)
    }

    private class FakePreTriageGateway(
        private val pending: RemoteResult<PreTriage?>,
    ) : PreTriageGateway {
        var lastToken: String? = null

        override suspend fun draft(token: SecretString): RemoteResult<PreTriageForm> {
            lastToken = token.value
            return RemoteResult.Success(PreTriageForm())
        }

        override suspend fun submit(token: SecretString, form: PreTriageForm): RemoteResult<PreTriage> {
            lastToken = token.value
            return RemoteResult.Success(PreTriage(id = 1, createdAt = Instant.fromEpochMilliseconds(0), form = form))
        }

        override suspend fun pending(token: SecretString): RemoteResult<PreTriage?> {
            lastToken = token.value
            return pending
        }
    }
}
