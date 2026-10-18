package grupotp.recifesaudavel.app.usecase

import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.pretriage.PreTriage
import grupotp.recifesaudavel.common.pretriage.PreTriageForm
import grupotp.recifesaudavel.common.security.SecretString
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class PreTriageUseCaseTest {
    private val account = AuthenticatedAccount(
        id = "maria",
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
        val pending = LoadPendingPreTriage(gateway)(account)

        // Assert
        assertNull(pending)
        assertEquals("token-da-maria", gateway.lastToken)
    }

    @Test
    fun pendingReturnsTheLatestPreTriage() = runTest {
        // Arrange
        val latest = PreTriage(id = "pt-1", createdAt = Instant.fromEpochMilliseconds(0), form = PreTriageForm(reason = "Tontura"))
        val gateway = FakePreTriageGateway(pending = SchedulingResult.Success(latest))

        // Act
        val pending = LoadPendingPreTriage(gateway)(account)

        // Assert
        assertEquals(latest, pending)
    }

    @Test
    fun birthDateConvertsBetweenTypedAndIsoForms() {
        // Assert
        assertEquals(LocalDate(1980, 5, 31), parseBrazilianDate("31/05/1980"))
        assertNull(parseBrazilianDate("31/05/80"))
        assertNull(parseBrazilianDate("31/02/1980"))
        assertEquals("31/05/1980", formatBrazilianDate(LocalDate(1980, 5, 31)))
        assertEquals("", formatBrazilianDate(null))
        assertEquals("31/05/1980", maskBrazilianDate("31051980"))
        assertEquals("31/05", maskBrazilianDate("31/05"))
        assertEquals("31/05/1980", maskBrazilianDate("31/05/19801"))
    }

    @Test
    fun offlineMessageSaysNothingWasSent() {
        // Assert
        assertEquals(PRE_TRIAGE_OFFLINE_MESSAGE, SchedulingError.Offline.toPreTriageMessage())
        assertEquals(SESSION_EXPIRED_MESSAGE, SchedulingError.SessionExpired.toPreTriageMessage())
    }
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
        return SchedulingResult.Success(PreTriage(id = "pt-1", createdAt = Instant.fromEpochMilliseconds(0), form = form))
    }

    override suspend fun pending(token: SecretString): SchedulingResult<PreTriage?> {
        lastToken = token.value
        return pending
    }
}
