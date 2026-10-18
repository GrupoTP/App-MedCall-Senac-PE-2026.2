package grupotp.recifesaudavel.app.usecase

import grupotp.recifesaudavel.common.account.AccountProfile
import grupotp.recifesaudavel.common.account.ChangePasswordRequest
import grupotp.recifesaudavel.common.account.UpdateConsentsRequest
import grupotp.recifesaudavel.common.account.UpdateProfileRequest
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
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
        sessionToken = "token",
        phone = "81999999999",
    )
    private val profile = AccountProfile(
        fullName = "Polyana Fontes",
        socialName = "Poly",
        email = "novo@email.com",
        primaryPhone = "81988887777",
        primaryPhoneWhatsapp = true,
        secondaryPhone = "",
        secondaryPhoneWhatsapp = false,
        emergencyContactName = "",
        emergencyContactPhone = "",
        acceptedPrivacyPolicy = true,
        acceptedTreatment = true,
        wantsNotifications = false,
    )
    private val request = UpdateProfileRequest(
        fullName = "Polyana Fontes",
        socialName = "Poly",
        email = "novo@email.com",
        primaryPhone = "81988887777",
        primaryPhoneWhatsapp = true,
        secondaryPhone = "",
        secondaryPhoneWhatsapp = false,
        emergencyContactName = "",
        emergencyContactPhone = "",
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
            phone = "81988887777",
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
        ChangePassword(gateway)(account, "segredo", "novasenha", signOutOtherSessions = true)
        UpdateNotificationConsent(gateway)(account, true)
        RequestAccountDeletion(gateway)(account)

        // Assert
        assertEquals(listOf("token", "token", "token"), gateway.tokens)
        assertEquals(ChangePasswordRequest("segredo", "novasenha", signOutOtherSessions = true), gateway.passwordRequest)
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
        assertEquals(listOf("token"), stillQueued)
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
        PendingSignOuts(gateway, pending).revokeOrQueue("token")

        // Assert
        assertEquals(emptyList(), pending.tokens)
    }

    private class FakeAccountGateway(
        private val result: SchedulingResult<AccountProfile>,
    ) : AccountGateway {
        val tokens = mutableListOf<String>()
        var passwordRequest: ChangePasswordRequest? = null
        var consentRequest: UpdateConsentsRequest? = null

        override suspend fun profile(token: String): SchedulingResult<AccountProfile> = result.also { tokens += token }

        override suspend fun update(token: String, request: UpdateProfileRequest): SchedulingResult<AccountProfile> =
            result.also { tokens += token }

        override suspend fun updateConsents(
            token: String,
            request: UpdateConsentsRequest,
        ): SchedulingResult<AccountProfile> = result.also {
            tokens += token
            consentRequest = request
        }

        override suspend fun changePassword(
            token: String,
            request: ChangePasswordRequest,
        ): SchedulingResult<AccountProfile> = result.also {
            tokens += token
            passwordRequest = request
        }

        override suspend fun requestDeletion(token: String): SchedulingResult<AccountProfile> =
            result.also { tokens += token }

        var signedOut: String? = null
        var signOutResult: SchedulingResult<Unit> = SchedulingResult.Success(Unit)

        override suspend fun signOut(token: String): SchedulingResult<Unit> {
            if (signOutResult is SchedulingResult.Success) {
                signedOut = token
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
