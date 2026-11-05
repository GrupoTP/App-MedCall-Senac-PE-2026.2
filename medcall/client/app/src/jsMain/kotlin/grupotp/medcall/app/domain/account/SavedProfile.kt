package grupotp.medcall.app.domain.account

import grupotp.medcall.common.account.AccountProfile
import grupotp.medcall.common.auth.AuthenticatedAccount

/**
 * @property account the signed-in profile the device keeps, already updated.
 */
data class SavedProfile(
    val profile: AccountProfile,
    val account: AuthenticatedAccount,
)
