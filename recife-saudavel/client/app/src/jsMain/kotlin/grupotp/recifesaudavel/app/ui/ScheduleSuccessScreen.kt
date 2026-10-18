package grupotp.recifesaudavel.app.ui

import grupotp.recifesaudavel.common.scheduling.Appointment
import ojaynico.kotlin.react.json
import ojaynico.kotlin.react.native.component.ScrollView
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import ojaynico.kotlin.react.native.safearea.useSafeAreaInsets
import react.FC
import react.Props

internal external interface ScheduleSuccessScreenProps : Props {
    var appointment: Appointment
    var onAppointments: () -> Unit
    var onHome: () -> Unit
}

/**
 * Shown only after the server stored the appointment.
 */
internal val ScheduleSuccessScreen = FC<ScheduleSuccessScreenProps> { props ->
    val insets = useSafeAreaInsets()
    val appointment = props.appointment
    View {
        style = Styles.screen
        ScrollView {
            style = Styles.scroll
            contentContainerStyle = Styles.successScroll
            View {
                style = arrayOf(Styles.successInner, json { paddingTop = 32 + insets.top })
                View {
                    style = Styles.successIcon
                    largeCalendarIcon(Palette.BLUE)
                }
                View {
                    style = Styles.successHeading
                    Text {
                        style = Styles.successTitle
                        accessibilityRole = "header"
                        +"Consulta agendada!"
                    }
                    Text {
                        style = Styles.successSubtitle
                        +"Agendamento registrado com sucesso."
                    }
                }
                View {
                    style = Styles.card
                    View {
                        style = Styles.summaryListLoose
                        summaryRow("Paciente", appointment.patientName, loose = true)
                        summaryRow("Especialidade", appointment.specialtyName, loose = true)
                        summaryRow("Profissional", appointment.professionalName, loose = true)
                        summaryRow("Clínica", "${appointment.clinic.name} · ${appointment.clinic.city}", loose = true)
                        summaryRow(
                            "Data e hora",
                            "${formatDate(appointment.date)} às ${formatClinicTime(appointment.date, appointment.time, appointment.clinic)}",
                            last = true,
                            loose = true,
                        )
                    }
                }
                infoAlertRich(icon = { emoji("📱") }) {
                    if (appointment.contactPhone.isEmpty()) {
                        +"Confirmação para o telefone do seu cadastro. "
                    } else {
                        +"Confirmação para "
                        strongText(appointment.contactPhone.formatted())
                        +". "
                    }
                    +"Informações médicas não serão expostas em notificações externas."
                }
                View {
                    style = Styles.formActions
                    primaryButton("Ver minhas consultas", onPress = props.onAppointments)
                    secondaryButton("Voltar ao início", onPress = props.onHome)
                }
            }
        }
        AppFooter()
    }
}
