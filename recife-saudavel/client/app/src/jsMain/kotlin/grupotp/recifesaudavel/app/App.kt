package grupotp.recifesaudavel.app

import grupotp.recifesaudavel.app.ui.AppointmentsScreen
import grupotp.recifesaudavel.app.ui.HomeScreen
import grupotp.recifesaudavel.app.ui.LoginScreen
import grupotp.recifesaudavel.app.ui.Palette
import grupotp.recifesaudavel.app.ui.PreTriageScreen
import grupotp.recifesaudavel.app.ui.PreTriageSuccessScreen
import grupotp.recifesaudavel.app.ui.PrivacyPolicyScreen
import grupotp.recifesaudavel.app.ui.RegisterScreen
import grupotp.recifesaudavel.app.ui.ScheduleScreen
import grupotp.recifesaudavel.app.ui.ScheduleSuccessScreen
import grupotp.recifesaudavel.app.ui.SettingsScreen
import grupotp.recifesaudavel.app.ui.SettingsTab
import grupotp.recifesaudavel.app.ui.WelcomeScreen
import grupotp.recifesaudavel.app.ui.font.useAppFonts
import grupotp.recifesaudavel.app.ui.useScreenScope
import grupotp.recifesaudavel.app.usecase.BookAppointment
import grupotp.recifesaudavel.app.usecase.CancelAppointment
import grupotp.recifesaudavel.app.usecase.ChangePassword
import grupotp.recifesaudavel.app.usecase.ListAppointments
import grupotp.recifesaudavel.app.usecase.LoadAccountProfile
import grupotp.recifesaudavel.app.usecase.LoadAvailability
import grupotp.recifesaudavel.app.usecase.LoadPendingPreTriage
import grupotp.recifesaudavel.app.usecase.LoadPreTriageDraft
import grupotp.recifesaudavel.app.usecase.LoadSchedulingOptions
import grupotp.recifesaudavel.app.usecase.LoadSignedInAccount
import grupotp.recifesaudavel.app.usecase.PrepareOfflineShell
import grupotp.recifesaudavel.app.usecase.RefreshAccount
import grupotp.recifesaudavel.app.usecase.RegisterAccount
import grupotp.recifesaudavel.app.usecase.RememberSignedInAccount
import grupotp.recifesaudavel.app.usecase.RequestAccountDeletion
import grupotp.recifesaudavel.app.usecase.RequestPasswordRecovery
import grupotp.recifesaudavel.app.usecase.SaveAccountProfile
import grupotp.recifesaudavel.app.usecase.SignIn
import grupotp.recifesaudavel.app.usecase.SignOut
import grupotp.recifesaudavel.app.usecase.SubmitPreTriage
import grupotp.recifesaudavel.app.usecase.UpdateNotificationConsent
import grupotp.recifesaudavel.common.Appointment
import grupotp.recifesaudavel.common.AuthenticatedAccount
import kotlinx.coroutines.launch
import ojaynico.kotlin.react.native.component.StatusBar
import ojaynico.kotlin.react.native.safearea.SafeAreaProvider
import ojaynico.kotlin.react.native.safearea.initialWindowMetrics
import org.koin.mp.KoinPlatform
import react.FC
import react.Props
import react.useEffect
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
    PreTriage,
    PreTriageSuccess,
    Settings,
    PrivacyPolicy,
}

internal external interface AppProps : Props {
    var signIn: SignIn
    var registerAccount: RegisterAccount
    var requestPasswordRecovery: RequestPasswordRecovery
    var prepareOfflineShell: PrepareOfflineShell
    var rememberSignedInAccount: RememberSignedInAccount
    var loadSignedInAccount: LoadSignedInAccount
    var signOut: SignOut
    var refreshAccount: RefreshAccount
    var listAppointments: ListAppointments
    var loadSchedulingOptions: LoadSchedulingOptions
    var loadAvailability: LoadAvailability
    var bookAppointment: BookAppointment
    var cancelAppointment: CancelAppointment
    var loadPreTriageDraft: LoadPreTriageDraft
    var submitPreTriage: SubmitPreTriage
    var loadPendingPreTriage: LoadPendingPreTriage
    var loadAccountProfile: LoadAccountProfile
    var saveAccountProfile: SaveAccountProfile
    var updateNotificationConsent: UpdateNotificationConsent
    var changePassword: ChangePassword
    var requestAccountDeletion: RequestAccountDeletion
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
                refreshAccount = koin.get()
                listAppointments = koin.get()
                loadSchedulingOptions = koin.get()
                loadAvailability = koin.get()
                bookAppointment = koin.get()
                cancelAppointment = koin.get()
                loadPreTriageDraft = koin.get()
                submitPreTriage = koin.get()
                loadPendingPreTriage = koin.get()
                loadAccountProfile = koin.get()
                saveAccountProfile = koin.get()
                updateNotificationConsent = koin.get()
                changePassword = koin.get()
                requestAccountDeletion = koin.get()
            }
        }
    }
}

internal val App = FC<AppProps> { props ->
    var screen by useState(AppScreen.Welcome)
    var account by useState<AuthenticatedAccount?>(null)
    var booked by useState<Appointment?>(null)
    var settingsTab by useState(SettingsTab.Account)
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

    // Each time the home opens the device asks the server whether the profile changed.
    val signedInId = account?.id
    useEffect(screen, signedInId) {
        val current = account
        if (screen == AppScreen.Home && current != null) {
            scope.launch {
                val fresh = runCatching { props.refreshAccount(current) }.getOrNull()
                if (fresh != null && account?.id == fresh.id) {
                    account = fresh
                }
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

    // Leaving on purpose: the device forgets the profile and the person lands on the welcome screen.
    fun logout() {
        account = null
        booked = null
        settingsTab = SettingsTab.Account
        screen = AppScreen.Welcome
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
            loadPendingPreTriage = props.loadPendingPreTriage
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
        screen == AppScreen.PreTriage -> PreTriageScreen {
            this.account = signedIn
            loadPreTriageDraft = props.loadPreTriageDraft
            submitPreTriage = props.submitPreTriage
            onBack = { screen = AppScreen.Home }
            onSent = { screen = AppScreen.PreTriageSuccess }
            onSignOut = ::signOut
        }
        screen == AppScreen.PreTriageSuccess -> PreTriageSuccessScreen {
            onSchedule = { screen = AppScreen.Schedule }
            onHome = { screen = AppScreen.Home }
        }
        screen == AppScreen.Appointments -> AppointmentsScreen {
            this.account = signedIn
            listAppointments = props.listAppointments
            cancelAppointment = props.cancelAppointment
            onBack = { screen = AppScreen.Home }
            onSignOut = ::signOut
        }
        screen == AppScreen.Settings -> SettingsScreen {
            this.account = signedIn
            loadAccountProfile = props.loadAccountProfile
            saveAccountProfile = props.saveAccountProfile
            updateNotificationConsent = props.updateNotificationConsent
            changePassword = props.changePassword
            requestAccountDeletion = props.requestAccountDeletion
            tab = settingsTab
            onTab = { settingsTab = it }
            onBack = {
                settingsTab = SettingsTab.Account
                screen = AppScreen.Home
            }
            onPrivacyPolicy = { screen = AppScreen.PrivacyPolicy }
            onAccountChanged = { account = it }
            onLogout = ::logout
            onSignOut = ::signOut
        }
        screen == AppScreen.PrivacyPolicy -> PrivacyPolicyScreen {
            onBack = { screen = AppScreen.Settings }
        }
        else -> HomeScreen {
            this.account = signedIn
            listAppointments = props.listAppointments
            onSchedule = { screen = AppScreen.Schedule }
            onPreTriage = { screen = AppScreen.PreTriage }
            onAppointments = { screen = AppScreen.Appointments }
            onSettings = { screen = AppScreen.Settings }
            onSignOut = ::signOut
        }
    }
}
