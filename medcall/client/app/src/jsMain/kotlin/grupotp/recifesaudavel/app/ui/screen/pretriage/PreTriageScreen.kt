package grupotp.recifesaudavel.app.ui.screen.pretriage

import grupotp.recifesaudavel.app.di.useInject
import grupotp.recifesaudavel.app.domain.RemoteError
import grupotp.recifesaudavel.app.domain.RemoteResult
import grupotp.recifesaudavel.app.ui.component.alert.sessionExpiredAlert
import grupotp.recifesaudavel.app.ui.component.button.primaryButton
import grupotp.recifesaudavel.app.ui.component.button.secondaryButton
import grupotp.recifesaudavel.app.ui.component.layout.AppFooter
import grupotp.recifesaudavel.app.ui.component.layout.ScreenHeader
import grupotp.recifesaudavel.app.ui.component.layout.StepIndicator
import grupotp.recifesaudavel.app.ui.format.BrazilianDate
import grupotp.recifesaudavel.app.ui.format.ErrorMessages
import grupotp.recifesaudavel.app.ui.hook.useScreenScope
import grupotp.recifesaudavel.app.ui.screen.pretriage.step.PreTriageStep2Complaint
import grupotp.recifesaudavel.app.ui.screen.pretriage.step.PreTriageStep3MedicalHistory
import grupotp.recifesaudavel.app.ui.screen.pretriage.step.PreTriageStep4Medications
import grupotp.recifesaudavel.app.ui.screen.pretriage.step.PreTriageStep1PersonalData
import grupotp.recifesaudavel.app.ui.theme.Styles
import grupotp.recifesaudavel.app.usecase.pretriage.LoadPreTriageDraftUseCase
import grupotp.recifesaudavel.app.usecase.pretriage.SubmitPreTriageUseCase
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.pretriage.PreTriage
import grupotp.recifesaudavel.common.pretriage.PreTriageForm
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ojaynico.kotlin.react.native.component.ScrollView
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import react.FC
import react.Key
import react.Props
import react.useEffectOnce
import react.useState
import kotlin.time.Clock

private val STEP_LABELS = arrayOf("Dados pessoais", "Queixa principal", "Histórico médico", "Medicamentos")
private const val LAST_STEP = 3
private const val MIN_BIRTH_YEAR = 1900

internal external interface PreTriageScreenProps : Props {
    var account: AuthenticatedAccount
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
    val loadPreTriageDraft = useInject<LoadPreTriageDraftUseCase>()
    val submitPreTriage = useInject<SubmitPreTriageUseCase>()
    var form by useState<PreTriageForm?>(null)
    var birthDate by useState("")
    var step by useState(0)
    var invalidFields by useState<Map<String, String>>(emptyMap())
    var error by useState<RemoteError?>(null)
    var busy by useState(false)
    val scope = useScreenScope()

    useEffectOnce {
        scope.launch {
            when (val result = loadPreTriageDraft(props.account)) {
                is RemoteResult.Success -> {
                    form = result.value
                    birthDate = BrazilianDate.format(result.value.birthDate)
                }
                is RemoteResult.Failed -> error = result.error
            }
        }
    }

    fun update(key: String, change: (PreTriageForm) -> PreTriageForm) {
        form = form?.let(change)
        invalidFields = invalidFields - key
    }

    fun validate(current: PreTriageForm): Map<String, String> = buildMap {
        when (step) {
            0 -> {
                if (current.fullName.isBlank()) put("fullName", "Nome completo é obrigatório.")
                when {
                    birthDate.isBlank() -> put("birthDate", "Informe sua data de nascimento.")
                    !isValidBirthDate(BrazilianDate.parse(birthDate)) ->
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
            val result = submitPreTriage(props.account, current.copy(birthDate = BrazilianDate.parse(birthDate)))
            busy = false
            when (result) {
                is RemoteResult.Success -> props.onSent(result.value)
                is RemoteResult.Failed -> error = result.error
            }
        }
    }

    fun next() {
        val current = form ?: return
        val found = validate(current)
        invalidFields = found
        if (found.isNotEmpty()) {
            return
        }
        error = null
        if (step < LAST_STEP) step += 1 else send(current)
    }

    fun back() {
        error = null
        invalidFields = emptyMap()
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
                    sessionExpiredAlert(failure, props.onSignOut) { ErrorMessages.preTriage(it) }
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
                    0 -> PreTriageStep1PersonalData {
                        this.form = current
                        fieldErrors = invalidFields
                        onChange = ::update
                        this.birthDate = birthDate
                        onBirthDate = {
                            birthDate = BrazilianDate.mask(it)
                            invalidFields = invalidFields - "birthDate"
                        }
                    }
                    1 -> PreTriageStep2Complaint {
                        this.form = current
                        fieldErrors = invalidFields
                        onChange = ::update
                    }
                    2 -> PreTriageStep3MedicalHistory {
                        this.form = current
                        fieldErrors = invalidFields
                        onChange = ::update
                    }
                    else -> PreTriageStep4Medications {
                        this.form = current
                        fieldErrors = invalidFields
                        onChange = ::update
                    }
                }
                sessionExpiredAlert(error, props.onSignOut) { ErrorMessages.preTriage(it) }
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

/** A real calendar date between 1900 and today. */
private fun isValidBirthDate(date: LocalDate?): Boolean =
    date != null && date.year >= MIN_BIRTH_YEAR &&
        date <= Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
