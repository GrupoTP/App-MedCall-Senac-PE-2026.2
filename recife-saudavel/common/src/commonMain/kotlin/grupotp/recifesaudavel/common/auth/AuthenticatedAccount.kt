package grupotp.recifesaudavel.common.auth

import grupotp.recifesaudavel.common.phone.Phone
import grupotp.recifesaudavel.common.security.SecretString
import kotlinx.serialization.Serializable

/**
 * Account data returned after a successful sign-in or registration.
 * The password never leaves the server.
 *
 * @property sessionToken bearer token for the authenticated API. Empty in profiles saved
 * before the server issued tokens; those must sign in again.
 * @property phone primary phone. Empty in profiles saved before the server returned it.
 */
@Serializable
data class AuthenticatedAccount(
    val id: String,
    val fullName: String,
    val socialName: String,
    val email: String,
    val sessionToken: SecretString = SecretString.EMPTY,
    val phone: Phone = Phone.EMPTY,
)
