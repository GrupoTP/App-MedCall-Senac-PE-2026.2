package grupotp.recifesaudavel.app.ui

import grupotp.recifesaudavel.app.usecase.BookAppointment
import grupotp.recifesaudavel.app.usecase.LoadAvailability
import grupotp.recifesaudavel.app.usecase.LoadSchedulingOptions
import grupotp.recifesaudavel.app.usecase.SchedulingError
import grupotp.recifesaudavel.app.usecase.SchedulingOptions
import grupotp.recifesaudavel.app.usecase.SchedulingResult
import grupotp.recifesaudavel.common.Appointment
import grupotp.recifesaudavel.common.AuthenticatedAccount
import grupotp.recifesaudavel.common.AvailableDay
import grupotp.recifesaudavel.common.CreateAppointmentRequest
import kotlinx.coroutines.launch
import ojaynico.kotlin.react.native.component.ScrollView
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import react.ChildrenBuilder
import react.Key
import react.FC
import react.Props
import react.useEffectOnce
import react.useState

private val STEP_LABELS = arrayOf("Especialidade", "Data e horário", "Confirmação")

internal external interface ScheduleScreenProps : Props {
    var account: AuthenticatedAccount
    var loadSchedulingOptions: LoadSchedulingOptions
    var loadAvailability: LoadAvailability
    var bookAppointment: BookAppointment
    var onBack: () -> Unit
    var onBooked: (Appointment) -> Unit
    var onSignOut: () -> Unit
}

/**
 * Three-step booking from the prototype. Every step talks to the server, so the flow stops with a
 * clear message when the device goes offline and never confirms an appointment the server did not store.
 */
internal val ScheduleScreen = FC<ScheduleScreenProps> { props ->
    var options by useState<SchedulingOptions?>(null)
    var step by useState(0)
    var specialtyId by useState<String?>(null)
    var professionalId by useState<String?>(null)
    var reason by useState("")
    var days by useState<List<AvailableDay>?>(null)
    var date by useState<String?>(null)
    var time by useState<String?>(null)
    var notes by useState("")
    var fieldErrors by useState<Map<String, String>>(emptyMap())
    var error by useState<SchedulingError?>(null)
    var busy by useState(false)
    val scope = useScreenScope()

    useEffectOnce {
        scope.launch {
            when (val result = props.loadSchedulingOptions(props.account)) {
                is SchedulingResult.Success -> options = result.value
                is SchedulingResult.Failed -> error = result.error
            }
        }
    }

    val loaded = options
    val specialty = loaded?.specialties?.firstOrNull { it.id == specialtyId }
    val professional = loaded?.professionals?.firstOrNull { it.id == professionalId }

    /** Asks the server for open times, then shows step 1. Keeps the choice if it is still open. */
    fun openDateStep() {
        val chosenSpecialty = specialtyId ?: return
        busy = true
        error = null
        scope.launch {
            val result = props.loadAvailability(props.account, chosenSpecialty, professionalId)
            busy = false
            when (result) {
                is SchedulingResult.Success -> {
                    val fresh = result.value
                    days = fresh
                    val freshDay = fresh.firstOrNull { it.date == date }
                    if (freshDay == null) {
                        date = fresh.firstOrNull()?.date
                        time = null
                    } else if (time !in freshDay.times) {
                        time = null
                    }
                    step = 1
                }
                is SchedulingResult.Failed -> error = result.error
            }
        }
    }

    fun confirm() {
        val chosenSpecialty = specialtyId ?: return
        val chosenDate = date ?: return
        val chosenTime = time ?: return
        if (busy) {
            return
        }
        busy = true
        error = null
        scope.launch {
            val result = props.bookAppointment(
                props.account,
                CreateAppointmentRequest(
                    specialtyId = chosenSpecialty,
                    professionalId = professionalId,
                    date = chosenDate,
                    time = chosenTime,
                    reason = reason.trim(),
                    notes = notes.trim(),
                ),
            )
            busy = false
            when (result) {
                is SchedulingResult.Success -> props.onBooked(result.value)
                is SchedulingResult.Failed -> error = result.error
            }
        }
    }

    fun next() {
        when (step) {
            0 -> if (specialtyId == null) {
                fieldErrors = mapOf("specialty" to "Selecione a especialidade.")
            } else {
                openDateStep()
            }
            1 -> {
                val errors = buildMap {
                    if (date == null) put("date", "Selecione uma data.")
                    if (time == null) put("time", "Selecione um horário.")
                }
                fieldErrors = errors
                if (errors.isEmpty()) {
                    error = null
                    step = 2
                }
            }
            else -> confirm()
        }
    }

    fun back() {
        error = null
        fieldErrors = emptyMap()
        when (step) {
            0 -> props.onBack()
            // Times may have been taken meanwhile, so going back to them asks the server again.
            2 -> openDateStep()
            else -> step -= 1
        }
    }

    View {
        style = Styles.screen
        ScreenHeader {
            title = "Agendar Consulta"
            onBack = ::back
        }
        if (loaded == null) {
            ScrollView {
                style = Styles.scroll
                contentContainerStyle = Styles.scrollContent
                val failure = error
                if (failure == null) {
                    Text {
                        style = Styles.loadingText
                        accessibilityRole = "progressbar"
                        +"Carregando especialidades…"
                    }
                } else {
                    sessionExpiredAlert(failure, props.onSignOut)
                    secondaryButton("Voltar ao início", onPress = props.onBack)
                }
            }
        } else {
            StepIndicator {
                current = step
                labels = STEP_LABELS
            }
            ScrollView {
                // A new key per step starts each step scrolled to the top.
                key = Key("step-$step")
                style = Styles.scroll
                contentContainerStyle = Styles.scrollContent
                keyboardShouldPersistTaps = "handled"
                when (step) {
                    0 -> View {
                        style = Styles.card
                        sectionTitle("Especialidade e Profissional")
                        formField(caption = "Especialidade médica", required = true) {
                            View {
                                style = Styles.chipWrap
                                accessibilityRole = "radiogroup"
                                loaded.specialties.forEach { item ->
                                    choiceChip(item.name, selected = item.id == specialtyId) {
                                        if (item.id != specialtyId) {
                                            specialtyId = item.id
                                            professionalId = null
                                            days = null
                                            date = null
                                            time = null
                                        }
                                        fieldErrors = emptyMap()
                                    }
                                }
                            }
                            fieldError(fieldErrors["specialty"])
                        }
                        if (specialty != null) {
                            formField(caption = "Profissional de preferência", hint = "Opcional") {
                                View {
                                    style = Styles.chipWrap
                                    accessibilityRole = "radiogroup"
                                    choiceChip("Qualquer profissional disponível", selected = professionalId == null) {
                                        professionalId = null
                                    }
                                    loaded.professionalsOf(specialty.id).forEach { item ->
                                        choiceChip(item.name, selected = item.id == professionalId) {
                                            professionalId = item.id
                                        }
                                    }
                                }
                            }
                        }
                        formField(caption = "Motivo breve", hint = "Opcional. Ajuda a clínica a se preparar.") {
                            TextField {
                                label = "Motivo breve"
                                value = reason
                                placeholder = "Descreva o motivo..."
                                kind = FieldKind.Text
                                multiline = true
                                onValue = { reason = it }
                            }
                        }
                    }
                    1 -> View {
                        style = Styles.card
                        sectionTitle("Data e Horário")
                        infoAlert("Horários disponíveis são validados pelo servidor no momento da confirmação.")
                        val openDays = days.orEmpty()
                        if (openDays.isEmpty()) {
                            warnAlert(
                                "Não há horários livres nos próximos 14 dias para esta escolha. " +
                                    "Volte e escolha outro profissional ou especialidade.",
                            )
                        } else {
                            formField(caption = "Data desejada", required = true) {
                                View {
                                    style = Styles.chipWrap
                                    accessibilityRole = "radiogroup"
                                    openDays.forEach { day ->
                                        choiceChip(
                                            text = formatShortDate(day.date),
                                            selected = day.date == date,
                                            accessibilityLabel = formatSpokenDate(day.date),
                                        ) {
                                            if (day.date != date) {
                                                date = day.date
                                                time = null
                                            }
                                            fieldErrors = fieldErrors - "date"
                                        }
                                    }
                                }
                                fieldError(fieldErrors["date"])
                            }
                            val times = openDays.firstOrNull { it.date == date }?.times.orEmpty()
                            formField(caption = "Horário desejado", required = true) {
                                View {
                                    style = Styles.chipWrap
                                    accessibilityRole = "radiogroup"
                                    times.forEach { option ->
                                        choiceChip(option, selected = option == time, style = Styles.timeChip) {
                                            time = option
                                            fieldErrors = fieldErrors - "time"
                                        }
                                    }
                                }
                                if (times.isEmpty()) {
                                    Text {
                                        style = Styles.fieldHint
                                        +"Escolha uma data para ver os horários."
                                    }
                                }
                                fieldError(fieldErrors["time"])
                            }
                        }
                    }
                    else -> {
                        View {
                            style = Styles.card
                            sectionTitle("Resumo do Agendamento")
                            View {
                                summaryRow("Especialidade", specialty?.name.orEmpty())
                                summaryRow("Profissional", professional?.name ?: "Qualquer disponível")
                                summaryRow("Data", date?.let(::formatDate).orEmpty())
                                summaryRow("Horário", time.orEmpty())
                                summaryRow("Paciente", props.account.socialName.ifBlank { props.account.fullName })
                            }
                        }
                        formField(caption = "Observação adicional", hint = "Opcional") {
                            TextField {
                                label = "Observação adicional"
                                value = notes
                                placeholder = "Informações complementares para a clínica..."
                                kind = FieldKind.Text
                                multiline = true
                                onValue = { notes = it }
                            }
                        }
                        infoAlert(
                            "A confirmação será enviada para o telefone principal do seu cadastro. " +
                                "Informações médicas não serão expostas em notificações.",
                        )
                        warnAlert("Conflito de horário é impedido pelo servidor. O horário definitivo será confirmado pela clínica.")
                    }
                }
                sessionExpiredAlert(error, props.onSignOut)
                View {
                    style = Styles.buttonRow
                    if (step > 0) {
                        secondaryButton("← Voltar", grow = true, onPress = ::back)
                    }
                    val action = when {
                        busy && step == 2 -> "Confirmando…"
                        busy -> "Carregando…"
                        step < 2 -> "Próximo →"
                        else -> "Confirmar agendamento ✓"
                    }
                    val canContinue = !busy && !(step == 1 && days.isNullOrEmpty())
                    primaryButton(action, enabled = canContinue, grow = true, onPress = ::next)
                }
            }
        }
        AppFooter()
    }
}

internal fun ChildrenBuilder.summaryRow(label: String, value: String) {
    View {
        style = Styles.summaryRow
        Text {
            style = Styles.summaryLabel
            +label
        }
        Text {
            style = Styles.summaryValue
            +value
        }
    }
}
