package grupotp.recifesaudavel.server.repository

import grupotp.recifesaudavel.common.auth.AuthenticatedAccount

data class StoredUser(
    val account: AuthenticatedAccount,
    val passwordHash: String,
)
