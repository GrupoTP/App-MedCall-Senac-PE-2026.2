package grupotp.recifesaudavel.common

import kotlinx.serialization.Serializable

@Serializable
data class SignInRequest(
    val email: String,
    val password: String,
)

@Serializable
data class RegisterAccountRequest(
    val fullName: String,
    val socialName: String,
    val email: String,
    val password: String,
    val primaryPhone: String,
    val primaryPhoneWhatsapp: Boolean,
    val secondaryPhone: String,
    val secondaryPhoneWhatsapp: Boolean,
    val emergencyContactName: String,
    val emergencyContactPhone: String,
    val acceptedPrivacyPolicy: Boolean,
    val acceptedTreatment: Boolean,
    val wantsNotifications: Boolean,
)

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
    val sessionToken: String = "",
    val phone: String = "",
)

@Serializable
data class ApiError(
    val message: String,
)
