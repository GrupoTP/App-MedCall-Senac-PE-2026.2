package grupotp.recifesaudavel.app.ui

import grupotp.recifesaudavel.app.usecase.AuthError

internal const val NOT_IMPLEMENTED_MESSAGE = "Não está implementado."

internal const val OFFLINE_MESSAGE =
    "Não foi possível falar com o servidor porque o aparelho está offline. Conecte-se à internet e tente de novo."

internal const val FOOTER_TEXT =
    "Copyright © 2026, Polyana Fontes; Thayná Batista da Silva; App Recife Saudável, Senac PE. Todos os direitos reservados."

internal fun AuthError.toMessage(): String = when (this) {
    AuthError.NotImplemented -> NOT_IMPLEMENTED_MESSAGE
    AuthError.Offline -> OFFLINE_MESSAGE
    is AuthError.Rejected -> message
}
