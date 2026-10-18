package grupotp.recifesaudavel.app.usecase.pretriage

import grupotp.recifesaudavel.app.domain.pretriage.PreTriageGateway
import grupotp.recifesaudavel.app.domain.scheduling.SchedulingError
import grupotp.recifesaudavel.app.domain.scheduling.SchedulingResult
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
        val gateway = FakePreTriageGateway(pending = SchedulingResult.Failed(SchedulingError.Offline))

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
        val gateway = FakePreTriageGateway(pending = SchedulingResult.Success(latest))

        // Act
        val pending = LoadPendingPreTriageUseCase(gateway)(account)

        // Assert
        assertEquals(latest, pending)
    }

    private class FakePreTriageGateway(
        private val pending: SchedulingResult<PreTriage?>,
    ) : PreTriageGateway {
        var lastToken: String? = null

        override suspend fun draft(token: SecretString): SchedulingResult<PreTriageForm> {
            lastToken = token.value
            return SchedulingResult.Success(PreTriageForm())
        }

        override suspend fun submit(token: SecretString, form: PreTriageForm): SchedulingResult<PreTriage> {
            lastToken = token.value
            return SchedulingResult.Success(PreTriage(id = 1, createdAt = Instant.fromEpochMilliseconds(0), form = form))
        }

        override suspend fun pending(token: SecretString): SchedulingResult<PreTriage?> {
            lastToken = token.value
            return pending
        }
    }
}
