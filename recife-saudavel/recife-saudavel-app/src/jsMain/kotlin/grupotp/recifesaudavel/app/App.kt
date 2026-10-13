package grupotp.recifesaudavel.app

import grupotp.recifesaudavel.app.ui.HomeScreen
import grupotp.recifesaudavel.app.ui.LoginScreen
import grupotp.recifesaudavel.app.ui.Palette
import grupotp.recifesaudavel.app.ui.RegisterScreen
import grupotp.recifesaudavel.app.ui.WelcomeScreen
import grupotp.recifesaudavel.app.ui.font.useAppFonts
import grupotp.recifesaudavel.app.ui.useScreenScope
import grupotp.recifesaudavel.app.usecase.ListUpcomingAppointments
import grupotp.recifesaudavel.app.usecase.LoadSignedInAccount
import grupotp.recifesaudavel.app.usecase.PrepareOfflineShell
import grupotp.recifesaudavel.app.usecase.RegisterAccount
import grupotp.recifesaudavel.app.usecase.RememberSignedInAccount
import grupotp.recifesaudavel.app.usecase.RequestPasswordRecovery
import grupotp.recifesaudavel.app.usecase.SignIn
import grupotp.recifesaudavel.common.AuthenticatedAccount
import kotlinx.coroutines.launch
import ojaynico.kotlin.react.native.component.StatusBar
import ojaynico.kotlin.react.native.safearea.SafeAreaProvider
import ojaynico.kotlin.react.native.safearea.initialWindowMetrics
import org.koin.mp.KoinPlatform
import react.FC
import react.Props
import react.useEffectOnce
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

/**
 * Wires Koin into [App] once the fonts are ready.
 */
internal val Root = FC<Props> {
    val fontsReady = useAppFonts()
    SafeAreaProvider {
        initialMetrics = initialWindowMetrics
        if (fontsReady) {
            val koin = KoinPlatform.getKoin()
            App {
                signIn = koin.get()
                registerAccount = koin.get()
                requestPasswordRecovery = koin.get()
                listUpcomingAppointments = koin.get()
                prepareOfflineShell = koin.get()
                rememberSignedInAccount = koin.get()
                loadSignedInAccount = koin.get()
            }
        }
    }
}

internal val App = FC<AppProps> { props ->
    var screen by useState(AppScreen.Welcome)
    var account by useState<AuthenticatedAccount?>(null)
    val scope = useScreenScope()
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

    // Welcome and Home draw blue behind the status bar; the form screens draw white.
    val onBlue = screen == AppScreen.Welcome || screen == AppScreen.Home
    StatusBar {
        barStyle = if (onBlue) "light-content" else "dark-content"
        backgroundColor = if (onBlue) Palette.BLUE else Palette.WHITE
        translucent = true
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
