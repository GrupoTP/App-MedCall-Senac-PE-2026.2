package grupotp.recifesaudavel.app

import grupotp.recifesaudavel.app.ui.HomeScreen
import grupotp.recifesaudavel.app.ui.LoginScreen
import grupotp.recifesaudavel.app.ui.RegisterScreen
import grupotp.recifesaudavel.app.ui.WelcomeScreen
import grupotp.recifesaudavel.app.usecase.ListUpcomingAppointments
import grupotp.recifesaudavel.app.usecase.RegisterAccount
import grupotp.recifesaudavel.app.usecase.RequestPasswordRecovery
import grupotp.recifesaudavel.app.usecase.SignIn
import grupotp.recifesaudavel.common.AuthenticatedAccount
import react.FC
import react.Props
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
}

internal val App = FC<AppProps> { props ->
    var screen by useState(AppScreen.Welcome)
    var account by useState<AuthenticatedAccount?>(null)

    fun openHome(authenticated: AuthenticatedAccount) {
        account = authenticated
        screen = AppScreen.Home
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
