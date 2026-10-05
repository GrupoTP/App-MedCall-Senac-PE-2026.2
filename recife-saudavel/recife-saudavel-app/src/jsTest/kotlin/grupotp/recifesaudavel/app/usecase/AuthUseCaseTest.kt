package grupotp.recifesaudavel.app.usecase

import grupotp.recifesaudavel.app.gateway.NotImplementedAuthGateway
import grupotp.recifesaudavel.app.ui.NOT_IMPLEMENTED_MESSAGE
import grupotp.recifesaudavel.app.ui.toMessage
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthUseCaseTest {
    @Test
    fun signInReportsThatAccessIsNotImplemented() {
        // Arrange
        val useCase = SignIn(NotImplementedAuthGateway())
        val command = SignInCommand(email = "maria@email.com", password = "segredo")

        // Act
        val result = useCase(command)

        // Assert
        assertEquals(AuthResult.Failed(AuthError.NotImplemented), result)
        assertEquals(NOT_IMPLEMENTED_MESSAGE, AuthError.NotImplemented.toMessage())
    }

    @Test
    fun registerReportsThatAccessIsNotImplemented() {
        // Arrange
        val useCase = RegisterAccount(NotImplementedAuthGateway())
        val command = RegisterAccountCommand(
            fullName = "Maria das Graças Silva",
            socialName = "Graça",
            email = "maria@email.com",
            password = "segredo",
            primaryPhone = "(81) 99999-9999",
            primaryPhoneWhatsapp = true,
            secondaryPhone = "",
            secondaryPhoneWhatsapp = false,
            emergencyContactName = "Ana Silva",
            emergencyContactPhone = "(81) 98888-7777",
            acceptedPrivacyPolicy = true,
            acceptedTreatment = true,
            wantsNotifications = false,
        )

        // Act
        val result = useCase(command)

        // Assert
        assertEquals(AuthResult.Failed(AuthError.NotImplemented), result)
    }

    @Test
    fun passwordRecoveryReportsThatAccessIsNotImplemented() {
        // Arrange
        val useCase = RequestPasswordRecovery(NotImplementedAuthGateway())

        // Act
        val result = useCase("maria@email.com")

        // Assert
        assertEquals(AuthResult.Failed(AuthError.NotImplemented), result)
    }
}
