package grupotp.recifesaudavel.app.usecase

/**
 * Account data collected by the registration screen.
 */
data class RegisterAccountCommand(
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
 * Asks the gateway to create an account.
 */
class RegisterAccount(
    private val authGateway: AuthGateway,
) {
    operator fun invoke(command: RegisterAccountCommand): AuthResult = authGateway.register(command)
}
