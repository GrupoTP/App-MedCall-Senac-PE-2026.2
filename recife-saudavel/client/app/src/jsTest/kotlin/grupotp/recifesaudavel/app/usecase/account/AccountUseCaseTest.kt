package grupotp.recifesaudavel.app.usecase.account

import grupotp.recifesaudavel.app.domain.RemoteError
import grupotp.recifesaudavel.app.domain.RemoteResult
import grupotp.recifesaudavel.app.domain.account.SavedProfile
import grupotp.recifesaudavel.app.fake.FakeAccountGateway
import grupotp.recifesaudavel.app.fake.MemoryLocalAccounts
import grupotp.recifesaudavel.common.account.AccountProfile
import grupotp.recifesaudavel.common.account.ChangePasswordRequest
import grupotp.recifesaudavel.common.account.Phone
import grupotp.recifesaudavel.common.account.UpdateConsentsRequest
import grupotp.recifesaudavel.common.account.UpdateProfileRequest
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.security.SecretString
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AccountUseCaseTest {
    private val account = AuthenticatedAccount(
        id = 1,
        fullName = "Poly",
        socialName = "",
        email = "poly@email.com",
        sessionToken = SecretString("token"),
        phone = Phone.parse("81999999999"),
    )
    private val profile = AccountProfile(
        fullName = "Polyana Fontes",
        socialName = "Poly",
        email = "novo@email.com",
        primaryPhone = Phone.parse("81988887777"),
        primaryPhoneWhatsapp = true,
        secondaryPhone = Phone.parse(""),
        secondaryPhoneWhatsapp = false,
        emergencyContactName = "",
        emergencyContactPhone = Phone.parse(""),
        acceptedPrivacyPolicy = true,
        acceptedTreatment = true,
        wantsNotifications = false,
    )
    private val request = UpdateProfileRequest(
        fullName = "Polyana Fontes",
        socialName = "Poly",
        email = "novo@email.com",
        primaryPhone = Phone.parse("81988887777"),
        primaryPhoneWhatsapp = true,
        secondaryPhone = Phone.parse(""),
        secondaryPhoneWhatsapp = false,
        emergencyContactName = "",
        emergencyContactPhone = Phone.parse(""),
    )

    @Test
    fun savedProfileUpdatesTheDeviceCopyKeepingTheToken() = runTest {
        // Arrange
        val local = MemoryLocalAccounts()
        val save = SaveAccountProfileUseCase(FakeAccountGateway(RemoteResult.Success(profile)), local)

        // Act
        val result = save(account, request)

        // Assert
        val saved = assertIs<RemoteResult.Success<SavedProfile>>(result).value
        val expected = account.copy(
            fullName = "Polyana Fontes",
            socialName = "Poly",
            email = "novo@email.com",
            phone = Phone.parse("81988887777"),
        )
        assertEquals(expected, saved.account)
        assertEquals(expected, local.stored)
        assertEquals(profile, saved.profile)
    }

    @Test
    fun refusedOrOfflineSaveLeavesTheDeviceCopyAlone() = runTest {
        // Arrange
        val local = MemoryLocalAccounts()
        val offline = SaveAccountProfileUseCase(
            FakeAccountGateway(RemoteResult.Failed(RemoteError.Offline)),
            local,
        )
        val refused = SaveAccountProfileUseCase(
            FakeAccountGateway(RemoteResult.Failed(RemoteError.Rejected("E-mail já cadastrado."))),
            local,
        )

        // Act
        val offlineResult = offline(account, request)
        val refusedResult = refused(account, request)

        // Assert
        assertEquals(RemoteError.Offline, assertIs<RemoteResult.Failed>(offlineResult).error)
        assertEquals(
            RemoteError.Rejected("E-mail já cadastrado."),
            assertIs<RemoteResult.Failed>(refusedResult).error,
        )
        assertEquals(null, local.stored)
    }

    @Test
    fun passwordAndConsentCallsCarryTheSessionToken() = runTest {
        // Arrange
        val gateway = FakeAccountGateway(RemoteResult.Success(profile))

        // Act
        ChangePasswordUseCase(gateway)(account, SecretString("segredo"), SecretString("novasenha"), signOutOtherSessions = true)
        UpdateNotificationConsentUseCase(gateway)(account, true)
        RequestAccountDeletionUseCase(gateway)(account)

        // Assert
        assertEquals(listOf("token", "token", "token"), gateway.tokens)
        assertEquals(ChangePasswordRequest(SecretString("segredo"), SecretString("novasenha"), signOutOtherSessions = true), gateway.passwordRequest)
        assertEquals(UpdateConsentsRequest(wantsNotifications = true), gateway.consentRequest)
    }
}
