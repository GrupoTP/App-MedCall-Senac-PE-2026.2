package grupotp.medcall.server.domain

import grupotp.medcall.common.auth.AuthenticatedAccount

data class StoredUser(
    val account: AuthenticatedAccount,
    val passwordHash: String,
)
