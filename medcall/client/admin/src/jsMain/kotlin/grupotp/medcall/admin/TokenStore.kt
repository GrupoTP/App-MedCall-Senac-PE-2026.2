package grupotp.medcall.admin

import web.storage.localStorage

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

