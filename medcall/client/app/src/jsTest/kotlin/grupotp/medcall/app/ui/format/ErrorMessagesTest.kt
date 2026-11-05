package grupotp.recifesaudavel.app.ui.format

import grupotp.recifesaudavel.app.domain.RemoteError
import grupotp.recifesaudavel.app.domain.auth.AuthError
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
        val scheduling = ErrorMessages.scheduling(RemoteError.Offline)
        val cancellation = ErrorMessages.cancellation(RemoteError.Offline)
        val preTriage = ErrorMessages.preTriage(RemoteError.Offline)

        // Assert
        assertEquals(ErrorMessages.SCHEDULING_OFFLINE, scheduling)
        assertEquals(ErrorMessages.CANCELLATION_OFFLINE, cancellation)
        assertEquals(ErrorMessages.PRE_TRIAGE_OFFLINE, preTriage)
    }

    @Test
    fun expiredSessionAndServerRefusalReadTheSameEverywhere() {
        // Act
        val expired = ErrorMessages.cancellation(RemoteError.SessionExpired)
        val rejected = ErrorMessages.preTriage(RemoteError.Rejected("Erro na clínica."))

        // Assert
        assertEquals(ErrorMessages.SESSION_EXPIRED, expired)
        assertEquals("Erro na clínica.", rejected)
    }
}
