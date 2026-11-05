package grupotp.recifesaudavel.app.usecase.auth

import grupotp.recifesaudavel.app.domain.auth.AuthError
import grupotp.recifesaudavel.app.domain.auth.AuthResult
import grupotp.recifesaudavel.app.fake.FakeAuthGateway
import grupotp.recifesaudavel.common.account.Phone
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.auth.RegisterAccountRequest
import grupotp.recifesaudavel.common.auth.SignInRequest
import grupotp.recifesaudavel.common.security.SecretString
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthUseCaseTest {
    @Test
    fun signInDelegatesTheJsonRequest() = runTest {
        // Arrange
        val request = SignInRequest(email = "maria@email.com", password = SecretString("segredo"))
        val account = AuthenticatedAccount(
            id = 1,
            fullName = "Maria das Graças Silva",
            socialName = "Graça",
            email = "maria@email.com",
        )
        val gateway = FakeAuthGateway(signInResult = AuthResult.Success(account))
        val signIn = SignInUseCase(gateway)

        // Act
        val result = signIn(request)

        // Assert
        assertEquals(request, gateway.signInRequest)
        assertEquals(AuthResult.Success(account), result)
    }

    @Test
    fun registerDelegatesTheJsonRequest() = runTest {
        // Arrange
        val request = sampleRegistration()
        val rejected = AuthResult.Failed(AuthError.Rejected("E-mail já cadastrado."))
        val gateway = FakeAuthGateway(registerResult = rejected)
        val register = RegisterAccountUseCase(gateway)

        // Act
        val result = register(request)

        // Assert
        assertEquals(request, gateway.registerRequest)
        assertEquals(rejected, result)
    }

    @Test
    fun passwordRecoveryStaysUnavailable() = runTest {
        // Arrange
        val gateway = FakeAuthGateway(recoveryResult = AuthResult.Failed(AuthError.NotImplemented))
        val requestRecovery = RequestPasswordRecoveryUseCase(gateway)

        // Act
        val result = requestRecovery("maria@email.com")

        // Assert
        assertEquals(AuthResult.Failed(AuthError.NotImplemented), result)
    }

    private fun sampleRegistration() = RegisterAccountRequest(
        fullName = "Maria das Graças Silva",
        socialName = "Graça",
        email = "maria@email.com",
        password = SecretString("segredo"),
        primaryPhone = Phone.parse("(81) 99999-9999"),
        primaryPhoneWhatsapp = true,
        secondaryPhone = Phone.parse(""),
        secondaryPhoneWhatsapp = false,
        emergencyContactName = "Ana Silva",
        emergencyContactPhone = Phone.parse("(81) 98888-7777"),
        acceptedPrivacyPolicy = true,
        acceptedTreatment = true,
        wantsNotifications = false,
    )
}
