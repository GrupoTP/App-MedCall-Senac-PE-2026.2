package grupotp.recifesaudavel.app.ui.screen.scheduling.step

import grupotp.recifesaudavel.app.ui.component.alert.infoAlertRich
import grupotp.recifesaudavel.app.ui.component.alert.warnAlert
import grupotp.recifesaudavel.app.ui.component.form.labeledTextField
import grupotp.recifesaudavel.app.ui.component.layout.sectionTitle
import grupotp.recifesaudavel.app.ui.component.layout.summaryRow
import grupotp.recifesaudavel.app.ui.component.text.emoji
import grupotp.recifesaudavel.app.ui.component.text.strongText
import grupotp.recifesaudavel.app.ui.format.formatClinicTime
import grupotp.recifesaudavel.app.ui.format.formatDate
import grupotp.recifesaudavel.app.ui.format.formatDateTime
import grupotp.recifesaudavel.app.ui.theme.Styles
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.pretriage.PreTriage
import grupotp.recifesaudavel.common.scheduling.Clinic
import grupotp.recifesaudavel.common.scheduling.Professional
import grupotp.recifesaudavel.common.scheduling.Specialty
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import ojaynico.kotlin.react.native.component.View
import react.FC
import react.Props

internal external interface SchedulingStep3ConfirmationProps : Props {
    var account: AuthenticatedAccount
    var specialty: Specialty?
    /** Null when any available professional will do. */
    var professional: Professional?
    var clinic: Clinic?
    var date: LocalDate?
    var time: LocalTime?
    var preTriage: PreTriage?
    var attachPreTriage: Boolean
    var notes: String
    var onNotes: (String) -> Unit
}

/**
 * Step 3: summary of the choices before the server books them, plus an optional note for the clinic.
 */
internal val SchedulingStep3Confirmation = FC<SchedulingStep3ConfirmationProps> { props ->
    val account = props.account
    val phone = account.phone
    val clinic = props.clinic
    val date = props.date
    val time = props.time

    View {
        style = Styles.card
        sectionTitle("Resumo do Agendamento")
        View {
            style = Styles.summaryList
            summaryRow("Especialidade", props.specialty?.name.orEmpty())
            summaryRow("Profissional", props.professional?.name ?: "Qualquer disponível")
            summaryRow("Clínica", clinic?.let { "${it.name} · ${it.city}" }.orEmpty())
            summaryRow("Data", date?.let(::formatDate).orEmpty())
            summaryRow("Horário", if (date != null && time != null && clinic != null) formatClinicTime(date, time, clinic) else "")
            props.preTriage?.let { pending ->
                summaryRow(
                    "Pré-triagem",
                    if (props.attachPreTriage) "Vinculada (${formatDateTime(pending.createdAt)})" else "Não vinculada",
                )
            }
            summaryRow("Paciente", account.socialName.ifBlank { account.fullName }, last = phone.isEmpty())
            if (!phone.isEmpty()) {
                summaryRow("Telefone", phone.formatted(), last = true)
            }
        }
    }
    labeledTextField(
        "Observação adicional",
        props.notes,
        "Informações complementares para a clínica...",
        multiline = true,
        hint = "Opcional",
        onValue = props.onNotes,
    )
    infoAlertRich(icon = { emoji("📱") }) {
        if (!phone.isEmpty()) {
            +"Confirmação enviada para "
            strongText(phone.formatted())
            +". "
        } else {
            +"A confirmação será enviada para o telefone principal do seu cadastro. "
        }
        +"Informações médicas não serão expostas em notificações."
    }
    warnAlert("Conflito de horário é impedido pelo servidor. O horário definitivo será confirmado pela clínica.")
}
