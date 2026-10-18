package grupotp.recifesaudavel.app.gateway.local

import grupotp.recifesaudavel.common.auth.AuthenticatedAccount

/**
 * Local copy of the signed-in registration profile.
 * On the web the service worker owns the database; this gateway only asks it.
 */
interface LocalAccountGateway {
    suspend fun save(account: AuthenticatedAccount)

    suspend fun load(): AuthenticatedAccount?

    /**
     * Wipes the local database: the profile, its token and every copy tied to it, such as the
     * appointments. Only the [PendingSignOutGateway] queue stays, so an offline sign-out still
     * reaches the server later.
     */
    suspend fun clear()
}
