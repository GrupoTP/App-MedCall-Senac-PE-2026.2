package grupotp.recifesaudavel.app.usecase

import grupotp.recifesaudavel.common.AuthenticatedAccount
import grupotp.recifesaudavel.common.RegisterAccountRequest
import grupotp.recifesaudavel.common.SignInRequest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthUseCaseTest {
    @Test
    fun signInDelegatesTheJsonRequest() = runTest {
        // Arrange
        val request = SignInRequest(email = "maria@email.com", password = "segredo")
        val account = AuthenticatedAccount(
            id = "1",
            fullName = "Maria das Graças Silva",
            socialName = "Graça",
            email = "maria@email.com",
        )
        val gateway = FakeAuthGateway(signInResult = AuthResult.Success(account))
        val useCase = SignIn(gateway)

        // Act
        val result = useCase(request)

        // Assert
        assertEquals(request, gateway.signInRequest)
        assertEquals(AuthResult.Success(account), result)
    }

    @Test
    fun registerDelegatesTheJsonRequest() = runTest {
        // Arrange
        val request = sampleRegistration()
        val gateway = FakeAuthGateway(
            registerResult = AuthResult.Failed(AuthError.Rejected("E-mail já cadastrado.")),
        )
        val useCase = RegisterAccount(gateway)

        // Act
        val result = useCase(request)

        // Assert
        assertEquals(request, gateway.registerRequest)
        assertEquals("E-mail já cadastrado.", (result as AuthResult.Failed).error.toMessage())
    }

    @Test
    fun passwordRecoveryStaysUnavailable() = runTest {
        // Arrange
        val gateway = FakeAuthGateway(
            recoveryResult = AuthResult.Failed(AuthError.NotImplemented),
        )
        val useCase = RequestPasswordRecovery(gateway)

        // Act
        val result = useCase("maria@email.com")

        // Assert
        assertEquals(AuthResult.Failed(AuthError.NotImplemented), result)
        assertEquals(NOT_IMPLEMENTED_MESSAGE, AuthError.NotImplemented.toMessage())
        assertEquals(OFFLINE_MESSAGE, AuthError.Offline.toMessage())
    }

    private fun sampleRegistration() = RegisterAccountRequest(
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
}

private class FakeAuthGateway(
    private val signInResult: AuthResult = AuthResult.Failed(AuthError.NotImplemented),
    private val registerResult: AuthResult = AuthResult.Failed(AuthError.NotImplemented),
    private val recoveryResult: AuthResult = AuthResult.Failed(AuthError.NotImplemented),
) : AuthGateway {
    var signInRequest: SignInRequest? = null
    var registerRequest: RegisterAccountRequest? = null

    override suspend fun signIn(request: SignInRequest): AuthResult {
        signInRequest = request
        return signInResult
    }

    override suspend fun register(request: RegisterAccountRequest): AuthResult {
        registerRequest = request
        return registerResult
    }

    override suspend fun requestPasswordRecovery(email: String): AuthResult = recoveryResult

    var profileResult: AuthResult = AuthResult.Failed(AuthError.Offline)

    override suspend fun profile(token: String): AuthResult = profileResult
}
