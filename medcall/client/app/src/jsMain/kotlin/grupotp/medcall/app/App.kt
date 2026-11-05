package grupotp.medcall.app

import grupotp.medcall.app.di.useInject
import grupotp.medcall.app.ui.component.menu.FloatingMenu
import grupotp.medcall.app.ui.component.menu.MenuDestination
import grupotp.medcall.app.ui.font.useAppFonts
import grupotp.medcall.app.ui.hook.useScreenScope
import grupotp.medcall.app.ui.screen.auth.LoginScreen
import grupotp.medcall.app.ui.screen.auth.RegisterScreen
import grupotp.medcall.app.ui.screen.auth.WelcomeScreen
import grupotp.medcall.app.ui.screen.home.HomeScreen
import grupotp.medcall.app.ui.screen.pretriage.PreTriageScreen
import grupotp.medcall.app.ui.screen.pretriage.PreTriageSuccessScreen
import grupotp.medcall.app.ui.screen.scheduling.AppointmentsScreen
import grupotp.medcall.app.ui.screen.scheduling.ScheduleScreen
import grupotp.medcall.app.ui.screen.scheduling.ScheduleSuccessScreen
import grupotp.medcall.app.ui.screen.settings.PrivacyPolicyScreen
import grupotp.medcall.app.ui.screen.settings.SettingsScreen
import grupotp.medcall.app.ui.screen.settings.SettingsTab
import grupotp.medcall.app.ui.theme.Palette
import grupotp.medcall.app.ui.theme.Styles
import grupotp.medcall.app.usecase.offline.PrepareOfflineShellUseCase
import grupotp.medcall.app.usecase.session.FlushPendingSignOutsUseCase
import grupotp.medcall.app.usecase.session.LoadSignedInAccountUseCase
import grupotp.medcall.app.usecase.session.RefreshAccountUseCase
import grupotp.medcall.app.usecase.session.RememberSignedInAccountUseCase
import grupotp.medcall.app.usecase.session.SignOutUseCase
import grupotp.medcall.common.auth.AuthenticatedAccount
import grupotp.medcall.common.scheduling.Appointment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ojaynico.kotlin.react.native.component.StatusBar
import ojaynico.kotlin.react.native.component.View
import ojaynico.kotlin.react.native.safearea.SafeAreaProvider
import ojaynico.kotlin.react.native.safearea.initialWindowMetrics
import react.FC
import react.Props
import react.useEffect
import react.useEffectOnce
import react.useState

private const val PENDING_SIGN_OUT_RETRY_MS = 30_000L

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

/**
 * Shows [App] once the fonts are ready.
 */
internal val Root = FC<Props> {
    val fontsReady = useAppFonts()
    SafeAreaProvider {
        initialMetrics = initialWindowMetrics
        if (fontsReady) {
            App()
        }
    }
}

internal val App = FC<Props> {
    val prepareOfflineShell = useInject<PrepareOfflineShellUseCase>()
    val loadSignedInAccount = useInject<LoadSignedInAccountUseCase>()
    val rememberSignedInAccount = useInject<RememberSignedInAccountUseCase>()
    val refreshAccount = useInject<RefreshAccountUseCase>()
    val signOutOfDevice = useInject<SignOutUseCase>()
    val flushPendingSignOuts = useInject<FlushPendingSignOutsUseCase>()
    var screen by useState(AppScreen.Welcome)
    var account by useState<AuthenticatedAccount?>(null)
    var booked by useState<Appointment?>(null)
    var settingsTab by useState(SettingsTab.Account)
    val scope = useScreenScope()
    useEffectOnce {
        scope.launch {
            runCatching { prepareOfflineShell() }
            val remembered = runCatching { loadSignedInAccount() }.getOrNull()
            if (remembered != null) {
                account = remembered
                screen = AppScreen.Home
            }
        }
    }

    // Sign-outs made offline reach the server once the device can reach it: tried at start,
    // on every screen change and then every 30 seconds while the app is open.
    useEffect(screen) {
        scope.launch { runCatching { flushPendingSignOuts() } }
    }
    useEffectOnce {
        scope.launch {
            while (true) {
                delay(PENDING_SIGN_OUT_RETRY_MS)
                runCatching { flushPendingSignOuts() }
            }
        }
    }

    // Each time the home opens the device asks the server whether the profile changed.
    val signedInId = account?.id
    useEffect(screen, signedInId) {
        val current = account
        if (screen == AppScreen.Home && current != null) {
            scope.launch {
                val fresh = runCatching { refreshAccount(current) }.getOrNull()
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
            runCatching { rememberSignedInAccount(authenticated) }
        }
    }

    // The server refused the saved session: forget it on this device and sign in again.
    fun signOut() {
        account = null
        booked = null
        screen = AppScreen.Login
        scope.launch {
            runCatching { signOutOfDevice() }
        }
    }

    // Leaving on purpose: the device forgets the profile and the person lands on the welcome screen.
    fun logout() {
        val token = account?.sessionToken
        account = null
        booked = null
        settingsTab = SettingsTab.Account
        screen = AppScreen.Welcome
        scope.launch {
            runCatching { signOutOfDevice(token) }
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
    val showMenu = signedIn != null &&
        screen != AppScreen.Welcome && screen != AppScreen.Register && screen != AppScreen.Login
    fun navigate(destination: MenuDestination) {
        booked = null
        if (destination != MenuDestination.Settings) {
            settingsTab = SettingsTab.Account
        }
        screen = when (destination) {
            MenuDestination.Home -> AppScreen.Home
            MenuDestination.PreTriage -> AppScreen.PreTriage
            MenuDestination.Schedule -> AppScreen.Schedule
            MenuDestination.Appointments -> AppScreen.Appointments
            MenuDestination.Settings -> AppScreen.Settings
            MenuDestination.Privacy -> AppScreen.PrivacyPolicy
        }
    }
    val activeDestination = when (screen) {
        AppScreen.Home -> MenuDestination.Home
        AppScreen.PreTriage, AppScreen.PreTriageSuccess -> MenuDestination.PreTriage
        AppScreen.Schedule, AppScreen.ScheduleSuccess -> MenuDestination.Schedule
        AppScreen.Appointments -> MenuDestination.Appointments
        AppScreen.Settings -> MenuDestination.Settings
        AppScreen.PrivacyPolicy -> MenuDestination.Privacy
        else -> null
    }

    View {
        style = Styles.screen
        when {
            screen == AppScreen.Welcome -> WelcomeScreen {
                onStart = { screen = AppScreen.Register }
                onLogin = { screen = AppScreen.Login }
            }
            screen == AppScreen.Register -> RegisterScreen {
                onBack = { screen = AppScreen.Welcome }
                onAuthenticated = ::openHome
            }
            screen == AppScreen.Login || signedIn == null -> LoginScreen {
                onRegister = { screen = AppScreen.Register }
                onBack = { screen = AppScreen.Welcome }
                onAuthenticated = ::openHome
            }
            screen == AppScreen.Schedule -> ScheduleScreen {
                this.account = signedIn
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
                onBack = { screen = AppScreen.Home }
                onSignOut = ::signOut
            }
            screen == AppScreen.Settings -> SettingsScreen {
                this.account = signedIn
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
                onSchedule = { screen = AppScreen.Schedule }
                onPreTriage = { screen = AppScreen.PreTriage }
                onAppointments = { screen = AppScreen.Appointments }
                onSettings = { screen = AppScreen.Settings }
                onSignOut = ::signOut
            }
        }
        if (showMenu && signedIn != null) {
            FloatingMenu {
                displayName = signedIn.socialName.ifBlank { signedIn.fullName }
                active = activeDestination
                onNavigate = ::navigate
                onLogout = ::logout
            }
        }
    }
}
