package grupotp.recifesaudavel.app.usecase

import grupotp.recifesaudavel.common.AuthenticatedAccount

/**
 * Local copy of the signed-in registration profile.
 * The service worker owns the database; this gateway only asks it.
 */
interface LocalAccountGateway {
    suspend fun save(account: AuthenticatedAccount)

    suspend fun load(): AuthenticatedAccount?

    /** Forgets the profile and every local copy tied to it, such as the appointments. */
    suspend fun clear()
}
