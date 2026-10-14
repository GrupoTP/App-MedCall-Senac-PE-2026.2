package grupotp.recifesaudavel.app.ui

import grupotp.recifesaudavel.common.Appointment
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
                        summaryRow("Paciente", appointment.patientName)
                        summaryRow("Especialidade", appointment.specialtyName)
                        summaryRow("Profissional", appointment.professionalName)
                        summaryRow("Data e hora", "${formatDate(appointment.date)} às ${appointment.time}")
                        summaryRow("Status", appointment.status.label())
                    }
                }
                val phone = appointment.contactPhone.ifBlank { "o telefone do seu cadastro" }
                infoAlert("Confirmação para $phone. Informações médicas não serão expostas em notificações externas.")
                infoAlert("Esta consulta fica guardada no aparelho e pode ser vista em Minhas consultas mesmo sem internet.")
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
