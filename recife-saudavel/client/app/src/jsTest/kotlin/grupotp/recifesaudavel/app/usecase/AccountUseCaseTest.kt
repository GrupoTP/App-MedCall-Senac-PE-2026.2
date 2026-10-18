package grupotp.recifesaudavel.app.usecase

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
        id = "1",
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
        val local = MemoryAccounts()
        val save = SaveAccountProfile(FakeAccountGateway(SchedulingResult.Success(profile)), local)

        // Act
        val result = save(account, request)

        // Assert
        val saved = assertIs<SchedulingResult.Success<SavedProfile>>(result).value
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
        val local = MemoryAccounts()
        val offline = SaveAccountProfile(
            FakeAccountGateway(SchedulingResult.Failed(SchedulingError.Offline)),
            local,
        )
        val refused = SaveAccountProfile(
            FakeAccountGateway(SchedulingResult.Failed(SchedulingError.Rejected("E-mail já cadastrado."))),
            local,
        )

        // Act
        val offlineResult = offline(account, request)
        val refusedResult = refused(account, request)

        // Assert
        assertEquals(SchedulingError.Offline, assertIs<SchedulingResult.Failed>(offlineResult).error)
        assertEquals(
            SchedulingError.Rejected("E-mail já cadastrado."),
            assertIs<SchedulingResult.Failed>(refusedResult).error,
        )
        assertEquals(null, local.stored)
    }

    @Test
    fun passwordAndConsentCallsCarryTheSessionToken() = runTest {
        // Arrange
        val gateway = FakeAccountGateway(SchedulingResult.Success(profile))

        // Act
        ChangePassword(gateway)(account, SecretString("segredo"), SecretString("novasenha"), signOutOtherSessions = true)
        UpdateNotificationConsent(gateway)(account, true)
        RequestAccountDeletion(gateway)(account)

        // Assert
        assertEquals(listOf("token", "token", "token"), gateway.tokens)
        assertEquals(ChangePasswordRequest(SecretString("segredo"), SecretString("novasenha"), signOutOtherSessions = true), gateway.passwordRequest)
        assertEquals(UpdateConsentsRequest(wantsNotifications = true), gateway.consentRequest)
    }

    @Test
    fun signOutRevokesTheTokenAndForgetsTheDeviceCopy() = runTest {
        // Arrange
        val local = MemoryAccounts().also { it.stored = account }
        val gateway = FakeAccountGateway(SchedulingResult.Success(profile))
        val pending = MemoryPending()

        // Act
        SignOut(local, PendingSignOuts(gateway, pending))(account.sessionToken)

        // Assert
        assertEquals("token", gateway.signedOut)
        assertEquals(null, local.stored)
        assertEquals(emptyList(), pending.tokens)
    }

    @Test
    fun offlineSignOutQueuesTheTokenAndFlushRevokesItOnceOnline() = runTest {
        // Arrange
        val local = MemoryAccounts().also { it.stored = account }
        val gateway = FakeAccountGateway(SchedulingResult.Success(profile))
        gateway.signOutResult = SchedulingResult.Failed(SchedulingError.Offline)
        val pending = MemoryPending()
        val queue = PendingSignOuts(gateway, pending)

        // Act
        SignOut(local, queue)(account.sessionToken)
        queue.flush()
        val stillQueued = pending.tokens
        gateway.signOutResult = SchedulingResult.Success(Unit)
        queue.flush()

        // Assert
        assertEquals(null, local.stored)
        assertEquals(listOf("token"), stillQueued.map { it.value })
        assertEquals(emptyList(), pending.tokens)
        assertEquals("token", gateway.signedOut)
    }

    @Test
    fun expiredSessionIsNotQueuedBecauseThereIsNothingLeftToRevoke() = runTest {
        // Arrange
        val gateway = FakeAccountGateway(SchedulingResult.Success(profile))
        gateway.signOutResult = SchedulingResult.Failed(SchedulingError.SessionExpired)
        val pending = MemoryPending()

        // Act
        PendingSignOuts(gateway, pending).revokeOrQueue(SecretString("token"))

        // Assert
        assertEquals(emptyList(), pending.tokens)
    }

    private class FakeAccountGateway(
        private val result: SchedulingResult<AccountProfile>,
    ) : AccountGateway {
        val tokens = mutableListOf<String>()
        var passwordRequest: ChangePasswordRequest? = null
        var consentRequest: UpdateConsentsRequest? = null

        override suspend fun profile(token: SecretString): SchedulingResult<AccountProfile> = result.also { tokens += token.value }

        override suspend fun update(token: SecretString, request: UpdateProfileRequest): SchedulingResult<AccountProfile> =
            result.also { tokens += token.value }

        override suspend fun updateConsents(
            token: SecretString,
            request: UpdateConsentsRequest,
        ): SchedulingResult<AccountProfile> = result.also {
            tokens += token.value
            consentRequest = request
        }

        override suspend fun changePassword(
            token: SecretString,
            request: ChangePasswordRequest,
        ): SchedulingResult<AccountProfile> = result.also {
            tokens += token.value
            passwordRequest = request
        }

        override suspend fun requestDeletion(token: SecretString): SchedulingResult<AccountProfile> =
            result.also { tokens += token.value }

        var signedOut: String? = null
        var signOutResult: SchedulingResult<Unit> = SchedulingResult.Success(Unit)

        override suspend fun signOut(token: SecretString): SchedulingResult<Unit> {
            if (signOutResult is SchedulingResult.Success) {
                signedOut = token.value
            }
            return signOutResult
        }
    }

    private class MemoryAccounts : LocalAccountGateway {
        var stored: AuthenticatedAccount? = null

        override suspend fun save(account: AuthenticatedAccount) {
            stored = account
        }

        override suspend fun load(): AuthenticatedAccount? = stored

        override suspend fun clear() {
            stored = null
        }
    }
}
