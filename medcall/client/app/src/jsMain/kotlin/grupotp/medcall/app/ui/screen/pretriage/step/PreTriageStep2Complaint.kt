package grupotp.recifesaudavel.app.ui.screen.pretriage.step

import grupotp.recifesaudavel.app.ui.component.form.choiceChip
import grupotp.recifesaudavel.app.ui.component.form.fieldError
import grupotp.recifesaudavel.app.ui.component.form.formField
import grupotp.recifesaudavel.app.ui.component.form.labeledTextField
import grupotp.recifesaudavel.app.ui.component.layout.sectionTitle
import grupotp.recifesaudavel.app.ui.format.label
import grupotp.recifesaudavel.app.ui.theme.Styles
import grupotp.recifesaudavel.common.pretriage.PreTriageForm
import grupotp.recifesaudavel.common.pretriage.SymptomsStart
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import react.FC

/**
 * Step 2: the reason for the visit and the symptoms, in the patient's own words.
 */
internal val PreTriageStep2Complaint = FC<PreTriageStepProps> { props ->
    val current = props.form
    fun update(key: String, change: PreTriageForm.() -> PreTriageForm) = props.onChange(key, change)

    View {
        style = Styles.card
        sectionTitle("Queixa Principal")
        Text {
            style = Styles.fieldHint
            +"Descreva com suas palavras. O aplicativo não realiza diagnóstico."
        }
        labeledTextField(
            "Motivo da consulta",
            current.reason,
            "Ex: Dor no peito ao respirar, tontura frequente...",
            multiline = true,
            required = true,
        ) { update("reason") { copy(reason = it) } }
        fieldError(props.fieldErrors["reason"])
        labeledTextField("Sintomas atuais", current.currentSymptoms, "Liste os sintomas que está sentindo agora...", multiline = true) {
            update("currentSymptoms") { copy(currentSymptoms = it) }
        }
        formField(caption = "Quando começaram os sintomas?") {
            View {
                style = Styles.chipWrap
                accessibilityRole = "radiogroup"
                SymptomsStart.entries.forEach { option ->
                    choiceChip(option.label(), selected = option == current.symptomsStart) {
                        update("symptomsStart") {
                            copy(symptomsStart = if (symptomsStart == option) null else option)
                        }
                    }
                }
            }
        }
        labeledTextField(
            "Duração / padrão dos sintomas",
            current.symptomsPattern,
            "Ex: Dor constante, tontura ao levantar",
            hint = "Contínuo, intermitente, piora ao...",
        ) { update("symptomsPattern") { copy(symptomsPattern = it) } }
        formField(caption = "Intensidade (opcional)", hint = "1 = leve, 10 = insuportável") {
            View {
                style = Styles.chipWrap
                accessibilityRole = "radiogroup"
                choiceChip("Prefiro não informar", selected = current.intensity == null) {
                    update("intensity") { copy(intensity = null) }
                }
                (1..10).forEach { level ->
                    choiceChip(
                        text = level.toString(),
                        selected = level == current.intensity,
                        style = Styles.timeChip,
                        accessibilityLabel = "Intensidade $level${intensityNote(level)}",
                    ) { update("intensity") { copy(intensity = level) } }
                }
            }
        }
        labeledTextField("Observações adicionais", current.complaintNotes, "Algo mais que queira informar...", multiline = true) {
            update("complaintNotes") { copy(complaintNotes = it) }
        }
    }
}

private fun intensityNote(level: Int): String = when (level) {
    1 -> ", muito leve"
    5 -> ", moderado"
    10 -> ", insuportável"
    else -> ""
}
