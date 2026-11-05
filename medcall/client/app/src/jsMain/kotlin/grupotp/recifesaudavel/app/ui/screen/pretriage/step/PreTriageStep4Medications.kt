package grupotp.recifesaudavel.app.ui.screen.pretriage.step

import grupotp.recifesaudavel.app.ui.component.form.labeledTextField
import grupotp.recifesaudavel.app.ui.component.layout.sectionTitle
import grupotp.recifesaudavel.app.ui.theme.Styles
import grupotp.recifesaudavel.common.pretriage.PreTriageForm
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import react.FC

/**
 * Step 4: medications and supplements. Starts with the answers of the last pre-triage.
 */
internal val PreTriageStep4Medications = FC<PreTriageStepProps> { props ->
    val current = props.form
    fun update(key: String, change: PreTriageForm.() -> PreTriageForm) = props.onChange(key, change)

    View {
        style = Styles.card
        sectionTitle("Medicamentos e Suplementos")
        Text {
            style = Styles.fieldHint
            +"O aplicativo não recomenda alterações de tratamento."
        }
        labeledTextField(
            "Medicamentos de uso regular",
            current.regularMedications,
            "Ex: Losartana 50mg, Metformina 850mg",
            multiline = true,
            hint = "Nome e dose, se souber",
        ) { update("regularMedications") { copy(regularMedications = it) } }
        labeledTextField(
            "Medicamentos de uso contínuo (todo dia)",
            current.continuousMedications,
            "Ex: AAS 100mg — coração; Sinvastatina — colesterol",
            multiline = true,
        ) { update("continuousMedications") { copy(continuousMedications = it) } }
        labeledTextField("Suplementos vitamínicos ou naturais", current.supplements, "Ex: Vitamina D 2000UI, Ômega 3, Complexo B", multiline = true) {
            update("supplements") { copy(supplements = it) }
        }
        labeledTextField("Outros medicamentos em uso", current.otherMedications, "Medicamentos pontuais, antibióticos, etc.", multiline = true) {
            update("otherMedications") { copy(otherMedications = it) }
        }
        labeledTextField(
            "Observações sobre medicamentos",
            current.medicationNotes,
            "Dificuldade de engolir, horários especiais...",
            multiline = true,
        ) { update("medicationNotes") { copy(medicationNotes = it) } }
    }
}
