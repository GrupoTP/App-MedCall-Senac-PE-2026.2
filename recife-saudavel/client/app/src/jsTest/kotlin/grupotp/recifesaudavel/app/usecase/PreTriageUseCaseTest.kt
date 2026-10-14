package grupotp.recifesaudavel.app.usecase

import grupotp.recifesaudavel.common.AuthenticatedAccount
import grupotp.recifesaudavel.common.PreTriage
import grupotp.recifesaudavel.common.PreTriageForm
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PreTriageUseCaseTest {
    private val account = AuthenticatedAccount(
        id = "maria",
        fullName = "Maria das Graças Silva",
        socialName = "Graça",
        email = "maria@email.com",
        sessionToken = "token-da-maria",
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
        val latest = PreTriage(id = "pt-1", createdAt = 0, form = PreTriageForm(reason = "Tontura"))
        val gateway = FakePreTriageGateway(pending = SchedulingResult.Success(latest))

        // Act
        val pending = LoadPendingPreTriage(gateway)(account)

        // Assert
        assertEquals(latest, pending)
    }

    @Test
    fun birthDateConvertsBetweenTypedAndIsoForms() {
        // Assert
        assertEquals("1980-05-31", brazilianDateToIso("31/05/1980"))
        assertEquals("", brazilianDateToIso("31/05/80"))
        assertEquals("31/05/1980", isoDateToBrazilian("1980-05-31"))
        assertEquals("", isoDateToBrazilian(""))
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

    override suspend fun draft(token: String): SchedulingResult<PreTriageForm> {
        lastToken = token
        return SchedulingResult.Success(PreTriageForm())
    }

    override suspend fun submit(token: String, form: PreTriageForm): SchedulingResult<PreTriage> {
        lastToken = token
        return SchedulingResult.Success(PreTriage(id = "pt-1", createdAt = 0, form = form))
    }

    override suspend fun pending(token: String): SchedulingResult<PreTriage?> {
        lastToken = token
        return pending
    }
}
