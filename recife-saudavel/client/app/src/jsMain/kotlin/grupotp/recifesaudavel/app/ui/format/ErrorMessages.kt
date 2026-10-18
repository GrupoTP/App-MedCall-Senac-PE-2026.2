package grupotp.recifesaudavel.app.ui.format

import grupotp.recifesaudavel.app.domain.RemoteError
import grupotp.recifesaudavel.app.domain.auth.AuthError

/**
 * Texts shown for the failures the use cases return.
 */
internal object ErrorMessages {
    const val NOT_IMPLEMENTED = "Não está implementado."

    const val OFFLINE =
        "Não foi possível falar com o servidor porque o aparelho está offline. Conecte-se à internet e tente de novo."

    const val SESSION_EXPIRED = "Sua sessão expirou. Entre de novo para continuar."

    const val SCHEDULING_OFFLINE =
        "O agendamento precisa de internet. Conecte-se e tente de novo. Nenhuma consulta foi marcada."

    const val CANCELLATION_OFFLINE =
        "O cancelamento precisa de internet. Conecte-se e tente de novo. Nenhuma consulta foi cancelada."

    const val PRE_TRIAGE_OFFLINE =
        "A pré-triagem precisa de internet. Conecte-se e tente de novo. Nada foi enviado."

    fun of(error: AuthError): String = when (error) {
        AuthError.NotImplemented -> NOT_IMPLEMENTED
        AuthError.Offline -> OFFLINE
        AuthError.SessionExpired -> SESSION_EXPIRED
        is AuthError.Rejected -> error.message
    }

    fun scheduling(error: RemoteError): String = withOffline(error, SCHEDULING_OFFLINE)

    fun cancellation(error: RemoteError): String = withOffline(error, CANCELLATION_OFFLINE)

    fun preTriage(error: RemoteError): String = withOffline(error, PRE_TRIAGE_OFFLINE)

    private fun withOffline(error: RemoteError, offline: String): String = when (error) {
        RemoteError.Offline -> offline
        RemoteError.SessionExpired -> SESSION_EXPIRED
        is RemoteError.Rejected -> error.message
    }
}
