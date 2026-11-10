package grupotp.medcall.admin

import grupotp.medcall.admin.api.AdminApi
import grupotp.medcall.admin.api.ApiException
import grupotp.medcall.admin.pages.ChangePasswordPage
import grupotp.medcall.admin.pages.LoginPage
import grupotp.medcall.admin.ui.AppLayout
import grupotp.medcall.admin.ui.installTheme
import grupotp.medcall.admin.ui.loading
import grupotp.medcall.common.staff.StaffAccount
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import react.FC
import react.Key
import react.create
import react.dom.client.createRoot
import react.dom.html.ReactHTML.div
import react.useEffectOnce
import react.useMemo
import react.useState
import web.cssom.ClassName
import web.dom.ElementId
import web.dom.document
import web.events.EventHandler
import web.window.window

private data class Toast(val id: Int, val text: String, val error: Boolean)

private var nextToastId = 1

val App = FC {
    var account by useState<StaffAccount?>(null)
    var checking by useState(TokenStore.read() != null)
    var hash by useState(window.location.hash)
    val (toasts, setToasts) = useState(emptyList<Toast>())
    var pendingTick by useState(0)

    val api = useMemo {
        AdminApi(
            tokenProvider = { TokenStore.read() },
            onSessionLost = {
                TokenStore.write(null)
                account = null
            },
        )
    }

    useEffectOnce {
        window.onhashchange = EventHandler { hash = window.location.hash }
        if (TokenStore.read() != null) {
            account = try {
                api.me()
            } catch (_: ApiException) {
                null
            }
            checking = false
        }
    }

    val notify: (String, Boolean) -> Unit = { text, error ->
        val toast = Toast(nextToastId++, text, error)
        setToasts { it + toast }
        appScope.launch {
            delay(4_500)
            setToasts { list -> list.filter { it.id != toast.id } }
        }
    }

    val current = account
    when {
        checking -> div { className = ClassName("login-page"); loading("Abrindo o portal…") }
        current == null -> LoginPage {
            this.api = api
            onSignedIn = { signed ->
                TokenStore.write(signed.sessionToken.value)
                account = signed.copy(sessionToken = grupotp.medcall.common.security.SecretString.EMPTY)
            }
        }
        current.mustChangePassword -> ChangePasswordPage {
            this.api = api
            this.account = current
            onChanged = { account = it }
            onSignOut = {
                appScope.launch { api.signOut() }
                TokenStore.write(null)
                account = null
            }
        }
        else -> SessionContext.Provider {
            value = Session(api, current, notify) { pendingTick++ }
            AppLayout {
                this.route = Router.current(current.role)
                this.hash = hash
                this.pendingTick = pendingTick
                onSignOut = {
                    appScope.launch { api.signOut() }
                    TokenStore.write(null)
                    account = null
                }
            }
        }
    }

    if (toasts.isNotEmpty()) {
        div {
            className = ClassName("toast-area")
            toasts.forEach { toast ->
                div {
                    key = Key(toast.id.toString())
                    className = ClassName("alert ${if (toast.error) "alert-danger" else "alert-success"} shadow mb-0 py-2")
                    asDynamic().role = "status"
                    +toast.text
                }
            }
        }
    }
}

fun main() {
    installTheme()
    val container = document.getElementById(ElementId("root")) ?: error("#root not found")
    createRoot(container).render(App.create())
}
