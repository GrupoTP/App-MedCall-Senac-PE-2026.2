package grupotp.recifesaudavel.server.domain

import kotlin.time.Instant

/**
 * A session just opened. Its token names [id] and carries [createdAt] as `iat`, whole seconds.
 */
data class OpenedSession(
    val id: Int,
    val createdAt: Instant,
)
