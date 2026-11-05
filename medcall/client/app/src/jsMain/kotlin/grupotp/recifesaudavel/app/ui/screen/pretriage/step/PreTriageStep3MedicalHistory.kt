package grupotp.recifesaudavel.app.ui.screen.pretriage.step

import grupotp.recifesaudavel.app.ui.component.form.labeledTextField
import grupotp.recifesaudavel.app.ui.component.layout.sectionTitle
import grupotp.recifesaudavel.app.ui.theme.Styles
import grupotp.recifesaudavel.common.pretriage.PreTriageForm
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import react.FC

/**
 * Step 3: conditions, surgeries, allergies and treatments. Starts with the answers of the last pre-triage.
 */
internal val PreTriageStep3MedicalHistory = FC<PreTriageStepProps> { props ->
    val current = props.form
    fun update(key: String, change: PreTriageForm.() -> PreTriageForm) = props.onChange(key, change)

    View {
        style = Styles.card
        sectionTitle("Histórico Médico")
        Text {
            style = Styles.fieldHint
            +"Dados de saúde são informações sensíveis protegidas pela LGPD."
        }
        labeledTextField(
            "Doenças anteriores ou crônicas",
            current.previousConditions,
            "Ex: Hipertensão há 10 anos, diabetes tipo 2",
            multiline = true,
            hint = "Ex: hipertensão, diabetes, asma",
        ) { update("previousConditions") { copy(previousConditions = it) } }
        labeledTextField("Cirurgias realizadas", current.surgeries, "Nenhuma, ou descreva com ano aproximado...", multiline = true) {
            update("surgeries") { copy(surgeries = it) }
        }
        labeledTextField(
            "Alergias conhecidas",
            current.allergies,
            "Ex: Dipirona, penicilina, amendoim",
            hint = "Medicamentos, alimentos, látex, outros",
        ) { update("allergies") { copy(allergies = it) } }
        labeledTextField("Tratamento atual", current.currentTreatment, "Acompanhamento médico, fisioterapia, etc.", multiline = true) {
            update("currentTreatment") { copy(currentTreatment = it) }
        }
        labeledTextField("Observações relevantes", current.historyNotes, "Outras informações importantes...", multiline = true) {
            update("historyNotes") { copy(historyNotes = it) }
        }
    }
}
