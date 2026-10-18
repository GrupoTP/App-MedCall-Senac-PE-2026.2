package grupotp.recifesaudavel.app.ui.screen.scheduling.step

import grupotp.recifesaudavel.app.domain.scheduling.SchedulingOptions
import grupotp.recifesaudavel.app.ui.component.alert.infoAlert
import grupotp.recifesaudavel.app.ui.component.form.CheckBox
import grupotp.recifesaudavel.app.ui.component.form.choiceChip
import grupotp.recifesaudavel.app.ui.component.form.fieldError
import grupotp.recifesaudavel.app.ui.component.form.formField
import grupotp.recifesaudavel.app.ui.component.form.labeledTextField
import grupotp.recifesaudavel.app.ui.component.layout.sectionTitle
import grupotp.recifesaudavel.app.ui.format.formatDateTime
import grupotp.recifesaudavel.app.ui.icon.checkIcon
import grupotp.recifesaudavel.app.ui.theme.Styles
import grupotp.recifesaudavel.common.pretriage.PreTriage
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import react.ChildrenBuilder
import react.FC
import react.Props

internal external interface SchedulingStep1SpecialtyProps : Props {
    var options: SchedulingOptions
    var specialtyId: Int?
    var professionalId: Int?
    var reason: String
    var preTriage: PreTriage?
    var attachPreTriage: Boolean
    var fieldErrors: Map<String, String>
    var onSpecialty: (Int) -> Unit
    var onProfessional: (Int?) -> Unit
    var onReason: (String) -> Unit
    var onAttachPreTriage: (Boolean) -> Unit
}

/**
 * Step 1: specialty, an optional professional and the reason. Offers to attach the latest pre-triage.
 */
internal val SchedulingStep1Specialty = FC<SchedulingStep1SpecialtyProps> { props ->
    val options = props.options
    val specialty = options.specialties.firstOrNull { it.id == props.specialtyId }

    View {
        style = Styles.card
        sectionTitle("Especialidade e Profissional")
        formField(caption = "Especialidade médica", required = true) {
            View {
                style = Styles.chipWrap
                accessibilityRole = "radiogroup"
                options.specialties.forEach { item ->
                    choiceChip(item.name, selected = item.id == props.specialtyId) { props.onSpecialty(item.id) }
                }
            }
            fieldError(props.fieldErrors["specialty"])
        }
        if (specialty != null) {
            formField(caption = "Profissional de preferência", hint = "Opcional") {
                View {
                    style = Styles.chipWrap
                    accessibilityRole = "radiogroup"
                    choiceChip("Qualquer profissional disponível", selected = props.professionalId == null) {
                        props.onProfessional(null)
                    }
                    val offered = options.professionalsOf(specialty.id)
                    // A specialty offered in several clinics names the city of each professional.
                    val severalClinics = offered.map { it.clinicId }.distinct().size > 1
                    offered.forEach { item ->
                        val city = options.clinic(item.clinicId)?.city
                        val label = if (severalClinics && city != null) "${item.name} ($city)" else item.name
                        choiceChip(label, selected = item.id == props.professionalId) { props.onProfessional(item.id) }
                    }
                }
            }
        }
        labeledTextField(
            "Motivo breve",
            props.reason,
            "Descreva o motivo...",
            multiline = true,
            hint = "Opcional. Ajuda a clínica a se preparar.",
            onValue = props.onReason,
        )
        props.preTriage?.let { pending ->
            preTriageChoice(pending, props.attachPreTriage, props.onAttachPreTriage)
        }
    }
}

/**
 * Latest pre-triage, attached by default. Unticking it books without one.
 */
private fun ChildrenBuilder.preTriageChoice(preTriage: PreTriage, attached: Boolean, onToggle: (Boolean) -> Unit) {
    val label = "Vincular a pré-triagem de ${formatDateTime(preTriage.createdAt)}"
    CheckBox {
        checked = attached
        this.label = label
        style = Styles.checkLine
        this.onToggle = onToggle
        Text {
            style = Styles.checkCopy
            +label
        }
    }
    if (attached) {
        infoAlert("Motivo pré-preenchido a partir da pré-triagem. A clínica verá suas respostas.") { checkIcon(it) }
    } else {
        Text {
            style = Styles.fieldHint
            +"A consulta será marcada sem pré-triagem. Ela continua guardada para a próxima."
        }
    }
}
