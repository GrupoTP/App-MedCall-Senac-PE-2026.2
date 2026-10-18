package grupotp.recifesaudavel.server.security

/** The signed-in person of a request, set by the JWT provider once its session is found open. */
data class SessionPrincipal(
    val userId: Int,
    val sessionId: Int,
)
