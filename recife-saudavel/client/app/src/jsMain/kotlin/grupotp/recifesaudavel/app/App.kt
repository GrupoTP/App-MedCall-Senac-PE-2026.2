package grupotp.recifesaudavel.app

import grupotp.recifesaudavel.app.ui.AppointmentsScreen
import grupotp.recifesaudavel.app.ui.HomeScreen
import grupotp.recifesaudavel.app.ui.LoginScreen
import grupotp.recifesaudavel.app.ui.Palette
import grupotp.recifesaudavel.app.ui.RegisterScreen
import grupotp.recifesaudavel.app.ui.ScheduleScreen
import grupotp.recifesaudavel.app.ui.ScheduleSuccessScreen
import grupotp.recifesaudavel.app.ui.WelcomeScreen
import grupotp.recifesaudavel.app.ui.font.useAppFonts
import grupotp.recifesaudavel.app.ui.useScreenScope
import grupotp.recifesaudavel.app.usecase.BookAppointment
import grupotp.recifesaudavel.app.usecase.CancelAppointment
import grupotp.recifesaudavel.app.usecase.ListAppointments
import grupotp.recifesaudavel.app.usecase.LoadAvailability
import grupotp.recifesaudavel.app.usecase.LoadSchedulingOptions
import grupotp.recifesaudavel.app.usecase.LoadSignedInAccount
import grupotp.recifesaudavel.app.usecase.PrepareOfflineShell
import grupotp.recifesaudavel.app.usecase.RegisterAccount
import grupotp.recifesaudavel.app.usecase.RememberSignedInAccount
import grupotp.recifesaudavel.app.usecase.RequestPasswordRecovery
import grupotp.recifesaudavel.app.usecase.SignIn
import grupotp.recifesaudavel.app.usecase.SignOut
import grupotp.recifesaudavel.common.Appointment
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
    Schedule,
    ScheduleSuccess,
    Appointments,
}

internal external interface AppProps : Props {
    var signIn: SignIn
    var registerAccount: RegisterAccount
    var requestPasswordRecovery: RequestPasswordRecovery
    var prepareOfflineShell: PrepareOfflineShell
    var rememberSignedInAccount: RememberSignedInAccount
    var loadSignedInAccount: LoadSignedInAccount
    var signOut: SignOut
    var listAppointments: ListAppointments
    var loadSchedulingOptions: LoadSchedulingOptions
    var loadAvailability: LoadAvailability
    var bookAppointment: BookAppointment
    var cancelAppointment: CancelAppointment
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
                prepareOfflineShell = koin.get()
                rememberSignedInAccount = koin.get()
                loadSignedInAccount = koin.get()
                signOut = koin.get()
                listAppointments = koin.get()
                loadSchedulingOptions = koin.get()
                loadAvailability = koin.get()
                bookAppointment = koin.get()
                cancelAppointment = koin.get()
            }
        }
    }
}

internal val App = FC<AppProps> { props ->
    var screen by useState(AppScreen.Welcome)
    var account by useState<AuthenticatedAccount?>(null)
    var booked by useState<Appointment?>(null)
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

    // The server refused the saved session: forget it on this device and sign in again.
    fun signOut() {
        account = null
        booked = null
        screen = AppScreen.Login
        scope.launch {
            runCatching { props.signOut() }
        }
    }

    // Welcome and Home draw blue behind the status bar; the other screens draw white or the canvas.
    val onBlue = screen == AppScreen.Welcome || screen == AppScreen.Home
    StatusBar {
        barStyle = if (onBlue) "light-content" else "dark-content"
        backgroundColor = if (onBlue) Palette.BLUE else Palette.WHITE
        translucent = true
    }

    val signedIn = account
    when {
        screen == AppScreen.Welcome -> WelcomeScreen {
            onStart = { screen = AppScreen.Register }
            onLogin = { screen = AppScreen.Login }
        }
        screen == AppScreen.Register -> RegisterScreen {
            registerAccount = props.registerAccount
            onBack = { screen = AppScreen.Welcome }
            onAuthenticated = ::openHome
        }
        screen == AppScreen.Login || signedIn == null -> LoginScreen {
            signIn = props.signIn
            requestPasswordRecovery = props.requestPasswordRecovery
            onRegister = { screen = AppScreen.Register }
            onBack = { screen = AppScreen.Welcome }
            onAuthenticated = ::openHome
        }
        screen == AppScreen.Schedule -> ScheduleScreen {
            this.account = signedIn
            loadSchedulingOptions = props.loadSchedulingOptions
            loadAvailability = props.loadAvailability
            bookAppointment = props.bookAppointment
            onBack = { screen = AppScreen.Home }
            onBooked = { appointment ->
                booked = appointment
                screen = AppScreen.ScheduleSuccess
            }
            onSignOut = ::signOut
        }
        screen == AppScreen.ScheduleSuccess && booked != null -> ScheduleSuccessScreen {
            appointment = booked!!
            onAppointments = { screen = AppScreen.Appointments }
            onHome = { screen = AppScreen.Home }
        }
        screen == AppScreen.Appointments -> AppointmentsScreen {
            this.account = signedIn
            listAppointments = props.listAppointments
            cancelAppointment = props.cancelAppointment
            onBack = { screen = AppScreen.Home }
            onSignOut = ::signOut
        }
        else -> HomeScreen {
            this.account = signedIn
            listAppointments = props.listAppointments
            onSchedule = { screen = AppScreen.Schedule }
            onAppointments = { screen = AppScreen.Appointments }
            onSignOut = ::signOut
        }
    }
}
