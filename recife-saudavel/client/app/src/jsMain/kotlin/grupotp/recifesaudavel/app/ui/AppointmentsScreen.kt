package grupotp.recifesaudavel.app.ui

import grupotp.recifesaudavel.app.usecase.AppointmentsSnapshot
import grupotp.recifesaudavel.app.usecase.ListAppointments
import grupotp.recifesaudavel.common.Appointment
import grupotp.recifesaudavel.common.AppointmentStatus
import grupotp.recifesaudavel.common.AuthenticatedAccount
import kotlinx.coroutines.launch
import ojaynico.kotlin.react.native.component.ScrollView
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import react.ChildrenBuilder
import react.Key
import react.FC
import react.Props
import react.useEffectOnce
import react.useState

internal external interface AppointmentsScreenProps : Props {
    var account: AuthenticatedAccount
    var listAppointments: ListAppointments
    var onBack: () -> Unit
    var onSignOut: () -> Unit
}

/**
 * Read-only list of the person's appointments. Offline it shows the copy saved on the device.
 */
internal val AppointmentsScreen = FC<AppointmentsScreenProps> { props ->
    var snapshot by useState<AppointmentsSnapshot?>(null)
    val scope = useScreenScope()

    useEffectOnce {
        scope.launch {
            snapshot = props.listAppointments(props.account)
        }
    }

    View {
        style = Styles.screen
        ScreenHeader {
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
                    current.appointments.forEach { appointmentCard(it) }
                    infoAlert("Consultas com status Aguardando ainda serão confirmadas pela clínica.")
                }
            }
        }
        AppFooter()
    }
}

private fun ChildrenBuilder.appointmentCard(appointment: Appointment) {
    View {
        key = Key(appointment.id)
        style = arrayOf(
            Styles.appointmentCard,
            if (appointment.status == AppointmentStatus.Cancelled) Styles.appointmentCancelled else null,
        )
        accessible = true
        accessibilityLabel = "${appointment.specialtyName} com ${appointment.professionalName}, " +
            "${formatSpokenDate(appointment.date)} às ${appointment.time}, ${appointment.status.label()}"
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
                smallCalendarIcon(Palette.MUTED)
                Text {
                    style = Styles.appointmentMetaText
                    +formatDate(appointment.date)
                }
            }
            View {
                style = Styles.appointmentMetaItem
                clockIcon(Palette.MUTED)
                Text {
                    style = Styles.appointmentMetaText
                    +appointment.time
                }
            }
        }
        if (appointment.reason.isNotBlank()) {
            Text {
                style = Styles.appointmentMetaText
                +"Motivo: ${appointment.reason}"
            }
        }
    }
}

internal fun ChildrenBuilder.statusPill(status: AppointmentStatus) {
    View {
        style = arrayOf(
            Styles.statusPill,
            when (status) {
                AppointmentStatus.Pending -> Styles.statusPending
                AppointmentStatus.Confirmed -> Styles.statusConfirmed
                AppointmentStatus.Cancelled -> Styles.statusCancelled
            },
        )
        Text {
            style = when (status) {
                AppointmentStatus.Pending -> Styles.statusPendingText
                AppointmentStatus.Confirmed -> Styles.statusConfirmedText
                AppointmentStatus.Cancelled -> Styles.statusCancelledText
            }
            +status.label()
        }
    }
}
