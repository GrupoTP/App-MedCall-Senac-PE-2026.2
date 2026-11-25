package grupotp.medcall.app.usecase.auth

import grupotp.medcall.app.domain.auth.AuthError
import grupotp.medcall.app.domain.auth.AuthResult
import grupotp.medcall.app.domain.auth.RecoveryResult
import grupotp.medcall.common.auth.ConfirmPasswordRecoveryRequest
import grupotp.medcall.app.fake.FakeAuthGateway
import grupotp.medcall.common.account.Phone
import grupotp.medcall.common.auth.AuthenticatedAccount
import grupotp.medcall.common.auth.RegisterAccountRequest
import grupotp.medcall.common.auth.SignInRequest
import grupotp.medcall.common.security.SecretString
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
    fun passwordRecoverySendsTheAddressAndTheCode() = runTest {
        val gateway = FakeAuthGateway(recoveryResult = RecoveryResult.Accepted, confirmResult = RecoveryResult.Accepted)
        val requestRecovery = RequestPasswordRecoveryUseCase(gateway)
        val confirmRecovery = ConfirmPasswordRecoveryUseCase(gateway)
        val confirmation = ConfirmPasswordRecoveryRequest(
            email = "maria@email.com",
            code = SecretString("codigo"),
            newPassword = SecretString("novasenha"),
        )

        val asked = requestRecovery("maria@email.com")
        val confirmed = confirmRecovery(confirmation)

        assertEquals("maria@email.com", gateway.recoveryEmail)
        assertEquals(RecoveryResult.Accepted, asked)
        assertEquals(confirmation, gateway.confirmRequest)
        assertEquals(RecoveryResult.Accepted, confirmed)
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
