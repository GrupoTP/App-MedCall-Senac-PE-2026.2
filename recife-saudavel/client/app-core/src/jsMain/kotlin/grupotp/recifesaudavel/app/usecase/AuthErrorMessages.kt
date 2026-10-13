package grupotp.recifesaudavel.app.usecase

const val NOT_IMPLEMENTED_MESSAGE = "Não está implementado."

const val OFFLINE_MESSAGE =
    "Não foi possível falar com o servidor porque o aparelho está offline. Conecte-se à internet e tente de novo."

fun AuthError.toMessage(): String = when (this) {
    AuthError.NotImplemented -> NOT_IMPLEMENTED_MESSAGE
    AuthError.Offline -> OFFLINE_MESSAGE
    is AuthError.Rejected -> message
}
