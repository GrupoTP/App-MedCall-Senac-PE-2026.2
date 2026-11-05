package grupotp.recifesaudavel.app.domain.account

import grupotp.recifesaudavel.common.account.AccountProfile
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount

/**
 * @property account the signed-in profile the device keeps, already updated.
 */
data class SavedProfile(
    val profile: AccountProfile,
    val account: AuthenticatedAccount,
)
