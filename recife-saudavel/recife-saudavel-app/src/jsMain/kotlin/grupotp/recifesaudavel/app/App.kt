package grupotp.recifesaudavel.app

import grupotp.recifesaudavel.app.ui.HomeScreen
import grupotp.recifesaudavel.app.ui.LoginScreen
import grupotp.recifesaudavel.app.ui.RegisterScreen
import grupotp.recifesaudavel.app.ui.WelcomeScreen
import grupotp.recifesaudavel.app.usecase.ListUpcomingAppointments
import grupotp.recifesaudavel.app.usecase.LoadSignedInAccount
import grupotp.recifesaudavel.app.usecase.PrepareOfflineShell
import grupotp.recifesaudavel.app.usecase.RegisterAccount
import grupotp.recifesaudavel.app.usecase.RememberSignedInAccount
import grupotp.recifesaudavel.app.usecase.RequestPasswordRecovery
import grupotp.recifesaudavel.app.usecase.SignIn
import grupotp.recifesaudavel.common.AuthenticatedAccount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import react.FC
import react.Props
import react.useEffectOnce
import react.useRef
import react.useState

internal enum class AppScreen {
    Welcome,
    Register,
    Login,
    Home,
}

internal external interface AppProps : Props {
    var signIn: SignIn
    var registerAccount: RegisterAccount
    var requestPasswordRecovery: RequestPasswordRecovery
    var listUpcomingAppointments: ListUpcomingAppointments
    var prepareOfflineShell: PrepareOfflineShell
    var rememberSignedInAccount: RememberSignedInAccount
    var loadSignedInAccount: LoadSignedInAccount
}

internal val App = FC<AppProps> { props ->
    var screen by useState(AppScreen.Welcome)
    var account by useState<AuthenticatedAccount?>(null)
    val scopeHolder = useRef<CoroutineScope>(null)
    val scope = scopeHolder.current ?: CoroutineScope(SupervisorJob()).also { scopeHolder.current = it }
    useEffectOnce {
        try {
            awaitCancellation()
        } finally {
            scope.cancel()
        }
    }
    useEffectOnce {
        scope.launch {
            runCatching { props.prepareOfflineShell() }
            val remembered = runCatching { props.loadSignedInAccount() }.getOrNull()
            if (remembered != null) {
                account = remembered
                screen = AppScreen.Home
            }
        }
    }

    fun openHome(authenticated: AuthenticatedAccount) {
        account = authenticated
        screen = AppScreen.Home
        scope.launch {
            runCatching { props.rememberSignedInAccount(authenticated) }
        }
    }

    when (screen) {
        AppScreen.Welcome -> WelcomeScreen {
            onStart = { screen = AppScreen.Register }
            onLogin = { screen = AppScreen.Login }
        }
        AppScreen.Register -> RegisterScreen {
            registerAccount = props.registerAccount
            onBack = { screen = AppScreen.Welcome }
            onAuthenticated = ::openHome
        }
        AppScreen.Login -> LoginScreen {
            signIn = props.signIn
            requestPasswordRecovery = props.requestPasswordRecovery
            onRegister = { screen = AppScreen.Register }
            onBack = { screen = AppScreen.Welcome }
            onAuthenticated = ::openHome
        }
        AppScreen.Home -> HomeScreen {
            this.account = account!!
            listUpcomingAppointments = props.listUpcomingAppointments
        }
    }
}
