package grupotp.recifesaudavel.app.ui

import grupotp.recifesaudavel.app.usecase.AppointmentsSnapshot
import grupotp.recifesaudavel.app.usecase.ListAppointments
import grupotp.recifesaudavel.app.usecase.SchedulingError
import grupotp.recifesaudavel.app.usecase.toMessage
import grupotp.recifesaudavel.app.usecase.upcomingAppointments
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.scheduling.Appointment
import kotlinx.coroutines.launch
import ojaynico.kotlin.react.json
import ojaynico.kotlin.react.native.component.Pressable
import ojaynico.kotlin.react.native.component.ScrollView
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import ojaynico.kotlin.react.native.safearea.useSafeAreaInsets
import react.ChildrenBuilder
import react.Key
import react.FC
import react.Props
import react.useEffectOnce
import react.useState
import kotlin.js.Date

private const val UPCOMING_ON_HOME = 2

internal const val SCHEDULING_NEEDS_NETWORK_MESSAGE =
    "O agendamento precisa de internet. Suas consultas já marcadas continuam disponíveis em Minhas consultas."

internal const val PRE_TRIAGE_NEEDS_NETWORK_MESSAGE =
    "A pré-triagem precisa de internet. Suas consultas já marcadas continuam disponíveis em Minhas consultas."

internal external interface HomeScreenProps : Props {
    var account: AuthenticatedAccount
    var listAppointments: ListAppointments
    var onSchedule: () -> Unit
    var onPreTriage: () -> Unit
    var onAppointments: () -> Unit
    var onSettings: () -> Unit
    var onSignOut: () -> Unit
}

internal val HomeScreen = FC<HomeScreenProps> { props ->
    var notice by useState<String?>(null)
    var snapshot by useState<AppointmentsSnapshot?>(null)
    var checking by useState(false)
    val insets = useSafeAreaInsets()
    val scope = useScreenScope()
    val displayName = props.account.socialName.ifBlank { props.account.fullName }
    val offline = snapshot?.offline == true

    useEffectOnce {
        scope.launch {
            snapshot = props.listAppointments(props.account)
        }
    }

    // Scheduling and pre-triage are online only. Offline, a press checks the connection again before refusing.
    fun openOnline(open: () -> Unit, offlineMessage: String) {
        if (!offline) {
            open()
            return
        }
        if (checking) {
            return
        }
        checking = true
        scope.launch {
            val fresh = props.listAppointments(props.account)
            snapshot = fresh
            checking = false
            if (fresh.offline) {
                notice = offlineMessage
            } else {
                notice = null
                open()
            }
        }
    }

    View {
        style = Styles.screen
        View {
            style = arrayOf(Styles.homeHeader, json { paddingTop = 16 + insets.top })
            verticalGradient(id = "home-gradient", from = Palette.BLUE, to = Palette.BLUE_HOVER)
            View {
                style = Styles.homeHeaderInner
                View {
                    style = Styles.homeHeaderTools
                    Pressable {
                        style = pressable(Styles.homeIconButton)
                        accessibilityRole = "button"
                        accessibilityLabel = "Configurações"
                        onPress = { props.onSettings() }
                        settingsIcon(Palette.WHITE)
                    }
                }
                Text {
                    style = Styles.homeGreeting
                    +"${greeting()},"
                }
                Text {
                    style = Styles.homeName
                    accessibilityRole = "header"
                    +displayName
                }
                Text {
                    style = Styles.homeEmail
                    +props.account.email
                }
            }
        }
        ScrollView {
            style = Styles.scroll
            contentContainerStyle = Styles.scrollContent
            clinicalDisclaimer()
            if (offline) {
                infoAlert(
                    "Você está offline. Mostrando as consultas guardadas neste aparelho. Para agendar ou fazer a pré-triagem, conecte-se à internet.",
                ) { wifiOffIcon(it) }
            }
            notice?.let { message ->
                errorAlert(message)
            }
            sessionExpiredAlert(snapshot?.error, props.onSignOut)
            View {
                style = Styles.cardCompact
                Text {
                    style = Styles.eyebrow
                    accessibilityRole = "header"
                    +"Ações rápidas"
                }
                View {
                    style = Styles.quickGrid
                    View {
                        style = Styles.quickRow
                        quickAction(
                            title = "Pré-triagem",
                            subtitle = if (offline) "Indisponível offline" else "Informe sintomas",
                            green = false,
                            unavailable = offline,
                            onPress = { openOnline(props.onPreTriage, PRE_TRIAGE_NEEDS_NETWORK_MESSAGE) },
                        ) { clipboardIcon(it) }
                        quickAction(
                            title = "Agendar",
                            subtitle = if (offline) "Indisponível offline" else "Escolha data e hora",
                            green = true,
                            unavailable = offline,
                            onPress = { openOnline(props.onSchedule, SCHEDULING_NEEDS_NETWORK_MESSAGE) },
                        ) { calendarIcon(it) }
                    }
                    Pressable {
                        style = pressable(Styles.quickWide)
                        accessibilityRole = "button"
                        onPress = { props.onAppointments() }
                        View {
                            style = arrayOf(Styles.quickIcon, Styles.quickIconPurple)
                            listIcon(Palette.PURPLE_TEXT)
                        }
                        View {
                            style = Styles.quickCopy
                            Text {
                                style = Styles.quickTitle
                                +"Minhas consultas"
                            }
                            Text {
                                style = Styles.quickSubtitle
                                +"Ver e gerenciar agendamentos"
                            }
                        }
                        chevronIcon(Palette.INK)
                    }
                }
            }
            View {
                style = Styles.cardCompact
                View {
                    style = Styles.sectionRow
                    Text {
                        style = Styles.eyebrowInline
                        accessibilityRole = "header"
                        +"Próximas consultas"
                    }
                    textLink("Ver todas", onPress = props.onAppointments)
                }
                val current = snapshot
                when {
                    current == null -> Text {
                        style = Styles.fieldHint
                        +"Carregando consultas…"
                    }
                    else -> {
                        val upcoming = upcomingAppointments(current.appointments, localNow()).take(UPCOMING_ON_HOME)
                        if (upcoming.isEmpty()) {
                            Text {
                                style = Styles.fieldHint
                                +"Nenhuma consulta agendada."
                            }
                        } else {
                            View {
                                style = Styles.upcomingList
                                upcoming.forEach { upcomingRow(it) }
                            }
                        }
                    }
                }
            }
        }
        AppFooter()
    }
}

/**
 * Shown when the server refused the saved token. Signing in again is the only way out.
 */
internal fun ChildrenBuilder.sessionExpiredAlert(
    error: SchedulingError?,
    onSignOut: () -> Unit,
    message: (SchedulingError) -> String = { it.toMessage() },
) {
    if (error == null) {
        return
    }
    errorAlert(message(error))
    if (error == SchedulingError.SessionExpired) {
        textLink("Entrar de novo", onPress = onSignOut)
    }
}

private fun ChildrenBuilder.upcomingRow(appointment: Appointment) {
    View {
        key = Key(appointment.id)
        style = Styles.upcomingRow
        accessible = true
        accessibilityLabel = "${appointment.professionalName}, ${appointment.specialtyName}, " +
            "${formatSpokenDate(appointment.date)} às ${appointment.time}, ${appointment.status.label()}"
        View {
            style = Styles.dateBadge
            Text {
                style = Styles.dateBadgeDay
                +dayOf(appointment.date)
            }
            Text {
                style = Styles.dateBadgeMonth
                +monthAbbreviation(appointment.date)
            }
        }
        View {
            style = Styles.upcomingCopy
            Text {
                style = Styles.upcomingTitle
                numberOfLines = 1
                +appointment.professionalName
            }
            Text {
                style = Styles.upcomingSubtitle
                +"${appointment.specialtyName} · ${appointment.time}"
            }
        }
        statusPill(appointment.status)
    }
}

private fun ChildrenBuilder.quickAction(
    title: String,
    subtitle: String,
    green: Boolean,
    unavailable: Boolean = false,
    onPress: () -> Unit,
    icon: ChildrenBuilder.(color: String) -> Unit,
) {
    Pressable {
        style = pressable(
            Styles.quickTile,
            if (green) Styles.quickTileGreen else null,
            if (unavailable) Styles.quickTileDisabled else null,
        )
        accessibilityRole = "button"
        accessibilityLabel = "$title. $subtitle"
        ariaDisabled = unavailable
        this.onPress = onPress
        View {
            style = arrayOf(Styles.quickIcon, if (green) Styles.quickIconGreen else null)
            icon(this, if (green) Palette.GREEN_TEXT else Palette.BLUE)
        }
        View {
            Text {
                style = Styles.quickTitle
                +title
            }
            Text {
                style = Styles.quickSubtitle
                +subtitle
            }
        }
    }
}

private fun greeting(): String {
    val hour = Date().getHours()
    return when {
        hour < 12 -> "Bom dia"
        hour < 18 -> "Boa tarde"
        else -> "Boa noite"
    }
}
