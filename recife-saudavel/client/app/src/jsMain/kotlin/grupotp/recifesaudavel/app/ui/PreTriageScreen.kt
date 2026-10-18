package grupotp.recifesaudavel.app.ui

import grupotp.recifesaudavel.app.platform.clinicNow
import grupotp.recifesaudavel.app.usecase.LoadPreTriageDraft
import grupotp.recifesaudavel.app.usecase.SchedulingError
import grupotp.recifesaudavel.app.usecase.SchedulingResult
import grupotp.recifesaudavel.app.usecase.SubmitPreTriage
import grupotp.recifesaudavel.app.usecase.formatBrazilianDate
import grupotp.recifesaudavel.app.usecase.maskBrazilianDate
import grupotp.recifesaudavel.app.usecase.parseBrazilianDate
import grupotp.recifesaudavel.app.usecase.toPreTriageMessage
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.phone.Phone
import grupotp.recifesaudavel.common.pretriage.PreTriage
import grupotp.recifesaudavel.common.pretriage.PreTriageForm
import grupotp.recifesaudavel.common.pretriage.SymptomsStart
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import ojaynico.kotlin.react.native.component.ScrollView
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import react.ChildrenBuilder
import react.FC
import react.Key
import react.Props
import react.useEffectOnce
import react.useState

private val STEP_LABELS = arrayOf("Dados pessoais", "Queixa principal", "Histórico médico", "Medicamentos")
private const val LAST_STEP = 3
private const val MIN_BIRTH_YEAR = 1900

internal external interface PreTriageScreenProps : Props {
    var account: AuthenticatedAccount
    var loadPreTriageDraft: LoadPreTriageDraft
    var submitPreTriage: SubmitPreTriage
    var onBack: () -> Unit
    var onSent: (PreTriage) -> Unit
    var onSignOut: () -> Unit
}

/**
 * Four-step pre-triage from the prototype. It starts filled with the current profile and with the
 * medical history and medications of the last pre-triage. Personal data edited here stay in the
 * pre-triage: the profile does not change.
 */
internal val PreTriageScreen = FC<PreTriageScreenProps> { props ->
    var form by useState<PreTriageForm?>(null)
    var birthDate by useState("")
    var step by useState(0)
    var fieldErrors by useState<Map<String, String>>(emptyMap())
    var error by useState<SchedulingError?>(null)
    var busy by useState(false)
    val scope = useScreenScope()

    useEffectOnce {
        scope.launch {
            when (val result = props.loadPreTriageDraft(props.account)) {
                is SchedulingResult.Success -> {
                    form = result.value
                    birthDate = formatBrazilianDate(result.value.birthDate)
                }
                is SchedulingResult.Failed -> error = result.error
            }
        }
    }

    fun update(key: String, change: PreTriageForm.() -> PreTriageForm) {
        form = form?.change()
        fieldErrors = fieldErrors - key
    }

    fun validate(current: PreTriageForm): Map<String, String> = buildMap {
        when (step) {
            0 -> {
                if (current.fullName.isBlank()) put("fullName", "Nome completo é obrigatório.")
                when {
                    birthDate.isBlank() -> put("birthDate", "Informe sua data de nascimento.")
                    !isValidBirthDate(parseBrazilianDate(birthDate)) ->
                        put("birthDate", "Informe uma data válida, no formato dd/mm/aaaa.")
                }
                if (current.primaryPhone.isEmpty()) put("primaryPhone", "Telefone principal é obrigatório.")
            }
            1 -> if (current.reason.isBlank()) put("reason", "Descreva o motivo da consulta.")
        }
    }

    fun send(current: PreTriageForm) {
        if (busy) {
            return
        }
        busy = true
        error = null
        scope.launch {
            val result = props.submitPreTriage(props.account, current.copy(birthDate = parseBrazilianDate(birthDate)))
            busy = false
            when (result) {
                is SchedulingResult.Success -> props.onSent(result.value)
                is SchedulingResult.Failed -> error = result.error
            }
        }
    }

    fun next() {
        val current = form ?: return
        val errors = validate(current)
        fieldErrors = errors
        if (errors.isNotEmpty()) {
            return
        }
        error = null
        if (step < LAST_STEP) step += 1 else send(current)
    }

    fun back() {
        error = null
        fieldErrors = emptyMap()
        if (step == 0) props.onBack() else step -= 1
    }

    View {
        style = Styles.screen
        ScreenHeader {
            menuSpace = true
            title = "Pré-triagem"
            onBack = ::back
        }
        val current = form
        if (current == null) {
            ScrollView {
                style = Styles.scroll
                contentContainerStyle = Styles.scrollContent
                val failure = error
                if (failure == null) {
                    Text {
                        style = Styles.loadingText
                        accessibilityRole = "progressbar"
                        +"Carregando seus dados…"
                    }
                } else {
                    sessionExpiredAlert(failure, props.onSignOut) { it.toPreTriageMessage() }
                    secondaryButton("Voltar ao início", onPress = props.onBack)
                }
            }
        } else {
            StepIndicator {
                this.current = step
                labels = STEP_LABELS
            }
            ScrollView {
                // A new key per step starts each step scrolled to the top.
                key = Key("step-$step")
                style = Styles.scroll
                contentContainerStyle = Styles.scrollContent
                keyboardShouldPersistTaps = "handled"
                when (step) {
                    0 -> {
                        clinicalDisclaimer()
                        warnAlert("Emergência? Não aguarde o agendamento. Ligue 192 (SAMU) ou vá ao pronto-socorro.", icon = "🚨")
                        View {
                            style = Styles.card
                            sectionTitle("Dados Pessoais")
                            textField("Nome completo", current.fullName, "Ex: José Carlos da Silva", FieldKind.Name, required = true) {
                                update("fullName") { copy(fullName = it) }
                            }
                            fieldError(fieldErrors["fullName"])
                            textField("Nome Social", current.socialName, "Ex: José", FieldKind.Name, hint = "Como prefere ser chamado(a)") {
                                update("socialName") { copy(socialName = it) }
                            }
                            formField(caption = "Data de nascimento", required = true) {
                                TextField {
                                    label = "Data de nascimento"
                                    value = birthDate
                                    placeholder = "dd/mm/aaaa"
                                    kind = FieldKind.Date
                                    onValue = {
                                        birthDate = maskBrazilianDate(it)
                                        fieldErrors = fieldErrors - "birthDate"
                                    }
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
                            textField("Nome do contato de emergência", current.emergencyContactName, "Ex: Ana Silva (filha)", FieldKind.Name) {
                                update("emergencyContactName") { copy(emergencyContactName = it) }
                            }
                            textField("Telefone do contato de emergência", current.emergencyContactPhone.digits, "(81) 99888-7766", FieldKind.Phone) {
                                update("emergencyContactPhone") { copy(emergencyContactPhone = Phone(it)) }
                            }
                        }
                    }
                    1 -> View {
                        style = Styles.card
                        sectionTitle("Queixa Principal")
                        Text {
                            style = Styles.fieldHint
                            +"Descreva com suas palavras. O aplicativo não realiza diagnóstico."
                        }
                        textField(
                            "Motivo da consulta",
                            current.reason,
                            "Ex: Dor no peito ao respirar, tontura frequente...",
                            multiline = true,
                            required = true,
                        ) { update("reason") { copy(reason = it) } }
                        fieldError(fieldErrors["reason"])
                        textField("Sintomas atuais", current.currentSymptoms, "Liste os sintomas que está sentindo agora...", multiline = true) {
                            update("currentSymptoms") { copy(currentSymptoms = it) }
                        }
                        formField(caption = "Quando começaram os sintomas?") {
                            View {
                                style = Styles.chipWrap
                                accessibilityRole = "radiogroup"
                                SymptomsStart.entries.forEach { option ->
                                    choiceChip(option.label, selected = option == current.symptomsStart) {
                                        update("symptomsStart") {
                                            copy(symptomsStart = if (symptomsStart == option) null else option)
                                        }
                                    }
                                }
                            }
                        }
                        textField(
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
                        textField("Observações adicionais", current.complaintNotes, "Algo mais que queira informar...", multiline = true) {
                            update("complaintNotes") { copy(complaintNotes = it) }
                        }
                    }
                    2 -> View {
                        style = Styles.card
                        sectionTitle("Histórico Médico")
                        Text {
                            style = Styles.fieldHint
                            +"Dados de saúde são informações sensíveis protegidas pela LGPD."
                        }
                        textField(
                            "Doenças anteriores ou crônicas",
                            current.previousConditions,
                            "Ex: Hipertensão há 10 anos, diabetes tipo 2",
                            multiline = true,
                            hint = "Ex: hipertensão, diabetes, asma",
                        ) { update("previousConditions") { copy(previousConditions = it) } }
                        textField("Cirurgias realizadas", current.surgeries, "Nenhuma, ou descreva com ano aproximado...", multiline = true) {
                            update("surgeries") { copy(surgeries = it) }
                        }
                        textField(
                            "Alergias conhecidas",
                            current.allergies,
                            "Ex: Dipirona, penicilina, amendoim",
                            hint = "Medicamentos, alimentos, látex, outros",
                        ) { update("allergies") { copy(allergies = it) } }
                        textField("Tratamento atual", current.currentTreatment, "Acompanhamento médico, fisioterapia, etc.", multiline = true) {
                            update("currentTreatment") { copy(currentTreatment = it) }
                        }
                        textField("Observações relevantes", current.historyNotes, "Outras informações importantes...", multiline = true) {
                            update("historyNotes") { copy(historyNotes = it) }
                        }
                    }
                    else -> View {
                        style = Styles.card
                        sectionTitle("Medicamentos e Suplementos")
                        Text {
                            style = Styles.fieldHint
                            +"O aplicativo não recomenda alterações de tratamento."
                        }
                        textField(
                            "Medicamentos de uso regular",
                            current.regularMedications,
                            "Ex: Losartana 50mg, Metformina 850mg",
                            multiline = true,
                            hint = "Nome e dose, se souber",
                        ) { update("regularMedications") { copy(regularMedications = it) } }
                        textField(
                            "Medicamentos de uso contínuo (todo dia)",
                            current.continuousMedications,
                            "Ex: AAS 100mg — coração; Sinvastatina — colesterol",
                            multiline = true,
                        ) { update("continuousMedications") { copy(continuousMedications = it) } }
                        textField("Suplementos vitamínicos ou naturais", current.supplements, "Ex: Vitamina D 2000UI, Ômega 3, Complexo B", multiline = true) {
                            update("supplements") { copy(supplements = it) }
                        }
                        textField("Outros medicamentos em uso", current.otherMedications, "Medicamentos pontuais, antibióticos, etc.", multiline = true) {
                            update("otherMedications") { copy(otherMedications = it) }
                        }
                        textField(
                            "Observações sobre medicamentos",
                            current.medicationNotes,
                            "Dificuldade de engolir, horários especiais...",
                            multiline = true,
                        ) { update("medicationNotes") { copy(medicationNotes = it) } }
                    }
                }
                sessionExpiredAlert(error, props.onSignOut) { it.toPreTriageMessage() }
                View {
                    style = Styles.buttonRow
                    if (step > 0) {
                        secondaryButton("← Voltar", grow = true, onPress = ::back)
                    }
                    val action = when {
                        busy -> "Enviando…"
                        step < LAST_STEP -> "Próximo →"
                        else -> "Enviar triagem ✓"
                    }
                    primaryButton(action, enabled = !busy, grow = true, onPress = ::next)
                }
            }
        }
        AppFooter()
    }
}

private fun ChildrenBuilder.textField(
    caption: String,
    value: String,
    placeholder: String,
    kind: FieldKind = FieldKind.Text,
    multiline: Boolean = false,
    required: Boolean = false,
    hint: String? = null,
    onValue: (String) -> Unit,
) {
    formField(caption = caption, required = required, hint = hint) {
        TextField {
            label = caption
            this.value = value
            this.placeholder = placeholder
            this.kind = kind
            this.multiline = multiline
            this.onValue = onValue
        }
    }
}

private fun intensityNote(level: Int): String = when (level) {
    1 -> ", muito leve"
    5 -> ", moderado"
    10 -> ", insuportável"
    else -> ""
}

/** A real calendar date between 1900 and today. */
private fun isValidBirthDate(date: LocalDate?): Boolean =
    date != null && date.year >= MIN_BIRTH_YEAR && date <= clinicNow().date
