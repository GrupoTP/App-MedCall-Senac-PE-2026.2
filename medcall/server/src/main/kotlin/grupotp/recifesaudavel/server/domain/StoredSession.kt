package grupotp.recifesaudavel.server.domain

import kotlin.time.Instant

data class StoredSession(
    val userId: Int,
    val createdAt: Instant,
    val lastUsedAt: Instant,
)
