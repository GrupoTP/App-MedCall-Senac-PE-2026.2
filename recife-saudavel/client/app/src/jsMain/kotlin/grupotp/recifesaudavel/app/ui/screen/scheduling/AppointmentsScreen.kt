package grupotp.recifesaudavel.app.ui.screen.scheduling

import grupotp.recifesaudavel.app.di.useInject
import grupotp.recifesaudavel.app.domain.scheduling.AppointmentsSnapshot
import grupotp.recifesaudavel.app.domain.scheduling.SchedulingError
import grupotp.recifesaudavel.app.domain.scheduling.SchedulingResult
import grupotp.recifesaudavel.app.ui.component.alert.errorAlert
import grupotp.recifesaudavel.app.ui.component.alert.infoAlert
import grupotp.recifesaudavel.app.ui.component.alert.sessionExpiredAlert
import grupotp.recifesaudavel.app.ui.component.alert.warnAlert
import grupotp.recifesaudavel.app.ui.component.button.primaryButton
import grupotp.recifesaudavel.app.ui.component.button.secondaryButton
import grupotp.recifesaudavel.app.ui.component.layout.AppFooter
import grupotp.recifesaudavel.app.ui.component.layout.ScreenHeader
import grupotp.recifesaudavel.app.ui.component.text.emoji
import grupotp.recifesaudavel.app.ui.component.text.statusPill
import grupotp.recifesaudavel.app.ui.format.ErrorMessages
import grupotp.recifesaudavel.app.ui.format.formatClinicTime
import grupotp.recifesaudavel.app.ui.format.formatDate
import grupotp.recifesaudavel.app.ui.format.formatSpokenDate
import grupotp.recifesaudavel.app.ui.format.label
import grupotp.recifesaudavel.app.ui.hook.useScreenScope
import grupotp.recifesaudavel.app.ui.icon.largeCalendarIcon
import grupotp.recifesaudavel.app.ui.icon.trashIcon
import grupotp.recifesaudavel.app.ui.icon.wifiOffIcon
import grupotp.recifesaudavel.app.ui.theme.Palette
import grupotp.recifesaudavel.app.ui.theme.Styles
import grupotp.recifesaudavel.app.usecase.scheduling.CancelAppointmentUseCase
import grupotp.recifesaudavel.app.usecase.scheduling.ListAppointmentsUseCase
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.scheduling.Appointment
import grupotp.recifesaudavel.common.scheduling.AppointmentStatus
import kotlinx.coroutines.launch
import ojaynico.kotlin.react.native.component.Pressable
import ojaynico.kotlin.react.native.component.ScrollView
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import react.ChildrenBuilder
import react.FC
import react.Key
import react.Props
import react.useEffectOnce
import react.useState
import kotlin.time.Clock

internal external interface AppointmentsScreenProps : Props {
    var account: AuthenticatedAccount
    var onBack: () -> Unit
    var onSignOut: () -> Unit
}

/**
 * The person's appointments. Offline it shows the copy saved on the device. Cancelling needs the
 * server, so it asks again before sending and never marks an appointment cancelled on its own.
 */
internal val AppointmentsScreen = FC<AppointmentsScreenProps> { props ->
    val listAppointments = useInject<ListAppointmentsUseCase>()
    val cancelAppointment = useInject<CancelAppointmentUseCase>()
    var snapshot by useState<AppointmentsSnapshot?>(null)
    var confirming by useState<Int?>(null)
    var cancelling by useState<Int?>(null)
    var cancelError by useState<SchedulingError?>(null)
    val scope = useScreenScope()

    fun cancel(id: Int) {
        if (cancelling != null) {
            return
        }
        cancelling = id
        cancelError = null
        scope.launch {
            val result = cancelAppointment(props.account, id)
            cancelling = null
            when (result) {
                is SchedulingResult.Success -> {
                    confirming = null
                    snapshot = snapshot?.let { current ->
                        current.copy(appointments = current.appointments.map { if (it.id == id) result.value else it })
                    }
                }
                is SchedulingResult.Failed -> cancelError = result.error
            }
        }
    }

    useEffectOnce {
        scope.launch {
            snapshot = listAppointments(props.account)
        }
    }

    View {
        style = Styles.screen
        ScreenHeader {
            menuSpace = true
            title = "Minhas Consultas"
            onBack = props.onBack
        }
        ScrollView {
            style = Styles.scroll
            contentContainerStyle = Styles.scrollContent
            val current = snapshot
            if (current == null) {
                Text {
                    style = Styles.loadingText
                    accessibilityRole = "progressbar"
                    +"Carregando consultas…"
                }
            } else {
                if (current.offline) {
                    infoAlert(
                        "Você está offline. Estas são as consultas guardadas neste aparelho na última conexão. " +
                            "Para agendar ou ver mudanças da clínica, conecte-se à internet.",
                    ) { wifiOffIcon(it) }
                }
                sessionExpiredAlert(current.error, props.onSignOut)
                val failure = cancelError
                if (failure is SchedulingError.SessionExpired) {
                    sessionExpiredAlert(failure, props.onSignOut)
                } else if (failure != null) {
                    errorAlert(ErrorMessages.cancellation(failure))
                }
                if (current.appointments.isEmpty()) {
                    View {
                        style = Styles.emptyState
                        largeCalendarIcon(Palette.MUTED)
                        Text {
                            style = Styles.emptyStateText
                            +if (current.offline) {
                                "Nenhuma consulta guardada neste aparelho."
                            } else {
                                "Nenhuma consulta encontrada."
                            }
                        }
                    }
                } else {
                    current.appointments.forEach { item ->
                        appointmentCard(
                            appointment = item,
                            confirming = confirming == item.id,
                            busy = cancelling == item.id,
                            onAsk = {
                                cancelError = null
                                confirming = item.id
                            },
                            onKeep = { confirming = null },
                            onConfirm = { cancel(item.id) },
                        )
                    }
                    warnAlert("Cancelamento sujeito à política de cada clínica. O histórico é mantido para fins de auditoria.")
                }
            }
        }
        AppFooter()
    }
}

private fun ChildrenBuilder.appointmentCard(
    appointment: Appointment,
    confirming: Boolean,
    busy: Boolean,
    onAsk: () -> Unit,
    onKeep: () -> Unit,
    onConfirm: () -> Unit,
) {
    View {
        key = Key(appointment.id)
        style = arrayOf(
            Styles.appointmentCard,
            if (appointment.status == AppointmentStatus.Cancelled) Styles.appointmentCancelled else null,
        )
        accessible = true
        accessibilityLabel = "${appointment.specialtyName} com ${appointment.professionalName}, " +
            "${formatSpokenDate(appointment.date)} às ${formatClinicTime(appointment.date, appointment.time, appointment.clinic)}, " +
            "${appointment.clinic.name}, ${appointment.status.label()}"
        View {
            style = Styles.appointmentTop
            View {
                style = Styles.upcomingCopy
                Text {
                    style = Styles.appointmentTitle
                    +appointment.specialtyName
                }
                Text {
                    style = Styles.upcomingSubtitle
                    +appointment.professionalName
                }
            }
            statusPill(appointment.status)
        }
        View {
            style = Styles.appointmentMeta
            View {
                style = Styles.appointmentMetaItem
                emoji("📅", Styles.emojiMeta)
                Text {
                    style = Styles.appointmentMetaText
                    +formatDate(appointment.date)
                }
            }
            View {
                style = Styles.appointmentMetaItem
                emoji("🕐", Styles.emojiMeta)
                Text {
                    style = Styles.appointmentMetaText
                    +formatClinicTime(appointment.date, appointment.time, appointment.clinic)
                }
            }
        }
        Text {
            style = Styles.appointmentMetaText
            +"${appointment.clinic.name} · ${appointment.clinic.city}"
        }
        if (appointment.reason.isNotBlank()) {
            Text {
                style = Styles.appointmentMetaText
                +"Motivo: ${appointment.reason}"
            }
        }
        if (appointment.preTriageId != null) {
            Text {
                style = Styles.appointmentMetaText
                +"Pré-triagem vinculada"
            }
        }
        if (appointment.isCancellableAt(Clock.System.now())) {
            if (confirming) {
                Text {
                    style = Styles.appointmentMetaText
                    accessibilityRole = "alert"
                    +"Deseja cancelar esta consulta?"
                }
                View {
                    style = Styles.buttonRow
                    secondaryButton("Manter consulta", grow = true, onPress = onKeep)
                    primaryButton(
                        if (busy) "Cancelando…" else "Sim, cancelar",
                        enabled = !busy,
                        grow = true,
                        onPress = onConfirm,
                    )
                }
            } else {
                Pressable {
                    style = Styles.cancelButton
                    accessibilityRole = "button"
                    accessibilityLabel = "Cancelar consulta de ${appointment.specialtyName}"
                    this.onPress = onAsk
                    trashIcon(Palette.CANCELLED_TEXT)
                    Text {
                        style = Styles.cancelButtonText
                        +"Cancelar consulta"
                    }
                }
            }
        }
    }
}
