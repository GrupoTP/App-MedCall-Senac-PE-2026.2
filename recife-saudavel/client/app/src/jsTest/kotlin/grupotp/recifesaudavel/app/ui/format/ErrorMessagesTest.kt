package grupotp.recifesaudavel.app.ui.format

import grupotp.recifesaudavel.app.domain.auth.AuthError
import grupotp.recifesaudavel.app.domain.scheduling.SchedulingError
import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorMessagesTest {
    @Test
    fun authErrorsShowTheirOwnText() {
        // Act
        val notImplemented = ErrorMessages.of(AuthError.NotImplemented)
        val offline = ErrorMessages.of(AuthError.Offline)
        val rejected = ErrorMessages.of(AuthError.Rejected("E-mail já cadastrado."))

        // Assert
        assertEquals(ErrorMessages.NOT_IMPLEMENTED, notImplemented)
        assertEquals(ErrorMessages.OFFLINE, offline)
        assertEquals("E-mail já cadastrado.", rejected)
    }

    @Test
    fun offlineMessagesSayWhatWasNotDone() {
        // Act
        val scheduling = ErrorMessages.scheduling(SchedulingError.Offline)
        val cancellation = ErrorMessages.cancellation(SchedulingError.Offline)
        val preTriage = ErrorMessages.preTriage(SchedulingError.Offline)

        // Assert
        assertEquals(ErrorMessages.SCHEDULING_OFFLINE, scheduling)
        assertEquals(ErrorMessages.CANCELLATION_OFFLINE, cancellation)
        assertEquals(ErrorMessages.PRE_TRIAGE_OFFLINE, preTriage)
    }

    @Test
    fun expiredSessionAndServerRefusalReadTheSameEverywhere() {
        // Act
        val expired = ErrorMessages.cancellation(SchedulingError.SessionExpired)
        val rejected = ErrorMessages.preTriage(SchedulingError.Rejected("Erro na clínica."))

        // Assert
        assertEquals(ErrorMessages.SESSION_EXPIRED, expired)
        assertEquals("Erro na clínica.", rejected)
    }
}
