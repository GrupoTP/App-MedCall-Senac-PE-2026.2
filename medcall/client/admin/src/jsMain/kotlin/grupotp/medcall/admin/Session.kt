package grupotp.medcall.admin

import grupotp.medcall.admin.api.AdminApi
import grupotp.medcall.common.staff.StaffAccount
import kotlinx.coroutines.MainScope
import react.createRequiredContext
import react.use
import web.storage.localStorage

/** Everything a page needs from the signed-in session. */
class Session(
    val api: AdminApi,
    val account: StaffAccount,
    /** Shows a short message in the corner; [error] paints it red. */
    val notify: (message: String, error: Boolean) -> Unit,
    /** Asks the layout to read the pending count again, after an agenda action. */
    val refreshPending: () -> Unit,
)

val SessionContext = createRequiredContext<Session>()

/** The signed-in session; only inside the provider set up by App. */
fun useSession(): Session = use(SessionContext) ?: error("No session: render inside SessionContext.")

/** Scope of the portal's calls; effects started in it end with their component. */
val appScope = MainScope()

/**
 * The staff token, kept in the browser so a reload keeps the session. It only opens the portals,
 * never the patient app, and the server can revoke it at any time.
 */
object TokenStore {
    private const val KEY = "medcall.portal.token"

    fun read(): String? = runCatching { localStorage.getItem(KEY) }.getOrNull()

    fun write(token: String?) {
        runCatching {
            if (token == null) localStorage.removeItem(KEY) else localStorage.setItem(KEY, token)
        }
    }
}
