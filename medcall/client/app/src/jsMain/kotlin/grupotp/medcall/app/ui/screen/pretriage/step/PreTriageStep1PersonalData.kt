package grupotp.recifesaudavel.app.ui.screen.pretriage.step

import grupotp.recifesaudavel.app.ui.component.alert.clinicalDisclaimer
import grupotp.recifesaudavel.app.ui.component.alert.warnAlert
import grupotp.recifesaudavel.app.ui.component.form.FieldKind
import grupotp.recifesaudavel.app.ui.component.form.TextField
import grupotp.recifesaudavel.app.ui.component.form.fieldError
import grupotp.recifesaudavel.app.ui.component.form.formField
import grupotp.recifesaudavel.app.ui.component.form.labeledTextField
import grupotp.recifesaudavel.app.ui.component.form.phoneField
import grupotp.recifesaudavel.app.ui.component.layout.sectionTitle
import grupotp.recifesaudavel.app.ui.theme.Styles
import grupotp.recifesaudavel.common.account.Phone
import grupotp.recifesaudavel.common.pretriage.PreTriageForm
import ojaynico.kotlin.react.native.component.View
import react.FC

internal external interface PreTriageStep1PersonalDataProps : PreTriageStepProps {
    /** Birth date as typed, `dd/mm/aaaa`; the screen parses it when moving on. */
    var birthDate: String
    var onBirthDate: (String) -> Unit
}

/**
 * Step 1: who the patient is and how to reach them. Starts filled with the current profile.
 */
internal val PreTriageStep1PersonalData = FC<PreTriageStep1PersonalDataProps> { props ->
    val current = props.form
    val fieldErrors = props.fieldErrors
    fun update(key: String, change: PreTriageForm.() -> PreTriageForm) = props.onChange(key, change)

    clinicalDisclaimer()
    warnAlert("Emergência? Não aguarde o agendamento. Ligue 192 (SAMU) ou vá ao pronto-socorro.", icon = "🚨")
    View {
        style = Styles.card
        sectionTitle("Dados Pessoais")
        labeledTextField("Nome completo", current.fullName, "Ex: José Carlos da Silva", FieldKind.Name, required = true) {
            update("fullName") { copy(fullName = it) }
        }
        fieldError(fieldErrors["fullName"])
        labeledTextField("Nome Social", current.socialName, "Ex: José", FieldKind.Name, hint = "Como prefere ser chamado(a)") {
            update("socialName") { copy(socialName = it) }
        }
        formField(caption = "Data de nascimento", required = true) {
            TextField {
                label = "Data de nascimento"
                value = props.birthDate
                placeholder = "dd/mm/aaaa"
                kind = FieldKind.Date
                onValue = props.onBirthDate
            }
            fieldError(fieldErrors["birthDate"])
        }
        phoneField(
            caption = "Telefone principal",
            value = current.primaryPhone,
            whatsapp = current.primaryPhoneWhatsapp,
            required = true,
            onValue = { update("primaryPhone") { copy(primaryPhone = it) } },
            onWhatsapp = { update("primaryPhone") { copy(primaryPhoneWhatsapp = it) } },
        )
        fieldError(fieldErrors["primaryPhone"])
        phoneField(
            caption = "Telefone secundário",
            value = current.secondaryPhone,
            whatsapp = current.secondaryPhoneWhatsapp,
            onValue = { update("secondaryPhone") { copy(secondaryPhone = it) } },
            onWhatsapp = { update("secondaryPhone") { copy(secondaryPhoneWhatsapp = it) } },
        )
        labeledTextField("Nome do contato de emergência", current.emergencyContactName, "Ex: Ana Silva (filha)", FieldKind.Name) {
            update("emergencyContactName") { copy(emergencyContactName = it) }
        }
        labeledTextField("Telefone do contato de emergência", current.emergencyContactPhone.digits, "(81) 99888-7766", FieldKind.Phone) {
            update("emergencyContactPhone") { copy(emergencyContactPhone = Phone(it)) }
        }
    }
}
