package grupotp.recifesaudavel.app.ui.screen.scheduling

import grupotp.recifesaudavel.app.di.useInject
import grupotp.recifesaudavel.app.domain.scheduling.SchedulingError
import grupotp.recifesaudavel.app.domain.scheduling.SchedulingOptions
import grupotp.recifesaudavel.app.domain.scheduling.SchedulingResult
import grupotp.recifesaudavel.app.ui.component.alert.infoAlert
import grupotp.recifesaudavel.app.ui.component.alert.infoAlertRich
import grupotp.recifesaudavel.app.ui.component.alert.sessionExpiredAlert
import grupotp.recifesaudavel.app.ui.component.alert.warnAlert
import grupotp.recifesaudavel.app.ui.component.button.primaryButton
import grupotp.recifesaudavel.app.ui.component.button.secondaryButton
import grupotp.recifesaudavel.app.ui.component.form.CheckBox
import grupotp.recifesaudavel.app.ui.component.form.FieldKind
import grupotp.recifesaudavel.app.ui.component.form.TextField
import grupotp.recifesaudavel.app.ui.component.form.choiceChip
import grupotp.recifesaudavel.app.ui.component.form.fieldError
import grupotp.recifesaudavel.app.ui.component.form.formField
import grupotp.recifesaudavel.app.ui.component.layout.AppFooter
import grupotp.recifesaudavel.app.ui.component.layout.ScreenHeader
import grupotp.recifesaudavel.app.ui.component.layout.StepIndicator
import grupotp.recifesaudavel.app.ui.component.layout.sectionTitle
import grupotp.recifesaudavel.app.ui.component.layout.summaryRow
import grupotp.recifesaudavel.app.ui.component.text.emoji
import grupotp.recifesaudavel.app.ui.component.text.strongText
import grupotp.recifesaudavel.app.ui.format.clinicZoneNote
import grupotp.recifesaudavel.app.ui.format.formatClinicTime
import grupotp.recifesaudavel.app.ui.format.formatDate
import grupotp.recifesaudavel.app.ui.format.formatDateTime
import grupotp.recifesaudavel.app.ui.format.formatShortDate
import grupotp.recifesaudavel.app.ui.format.formatSpokenDate
import grupotp.recifesaudavel.app.ui.format.formatTime
import grupotp.recifesaudavel.app.ui.hook.useScreenScope
import grupotp.recifesaudavel.app.ui.icon.checkIcon
import grupotp.recifesaudavel.app.ui.theme.Styles
import grupotp.recifesaudavel.app.usecase.pretriage.LoadPendingPreTriageUseCase
import grupotp.recifesaudavel.app.usecase.scheduling.BookAppointmentUseCase
import grupotp.recifesaudavel.app.usecase.scheduling.LoadAvailabilityUseCase
import grupotp.recifesaudavel.app.usecase.scheduling.LoadSchedulingOptionsUseCase
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.pretriage.PreTriage
import grupotp.recifesaudavel.common.scheduling.Appointment
import grupotp.recifesaudavel.common.scheduling.ClinicAvailability
import grupotp.recifesaudavel.common.scheduling.CreateAppointmentRequest
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import ojaynico.kotlin.react.native.component.ScrollView
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import react.ChildrenBuilder
import react.FC
import react.Key
import react.Props
import react.useEffectOnce
import react.useState

private val STEP_LABELS = arrayOf("Especialidade", "Data e horário", "Confirmação")

internal external interface ScheduleScreenProps : Props {
    var account: AuthenticatedAccount
    var onBack: () -> Unit
    var onBooked: (Appointment) -> Unit
    var onSignOut: () -> Unit
}

/**
 * Three-step booking from the prototype. Every step talks to the server, so the flow stops with a
 * clear message when the device goes offline and never confirms an appointment the server did not store.
 */
internal val ScheduleScreen = FC<ScheduleScreenProps> { props ->
    val loadSchedulingOptions = useInject<LoadSchedulingOptionsUseCase>()
    val loadAvailability = useInject<LoadAvailabilityUseCase>()
    val bookAppointment = useInject<BookAppointmentUseCase>()
    val loadPendingPreTriage = useInject<LoadPendingPreTriageUseCase>()
    var options by useState<SchedulingOptions?>(null)
    var step by useState(0)
    var specialtyId by useState<Int?>(null)
    var professionalId by useState<Int?>(null)
    var reason by useState("")
    var availability by useState<List<ClinicAvailability>?>(null)
    var clinicId by useState<Int?>(null)
    var date by useState<LocalDate?>(null)
    var time by useState<LocalTime?>(null)
    var notes by useState("")
    var fieldErrors by useState<Map<String, String>>(emptyMap())
    var error by useState<SchedulingError?>(null)
    var busy by useState(false)
    var preTriage by useState<PreTriage?>(null)
    var attachPreTriage by useState(true)
    val scope = useScreenScope()

    useEffectOnce {
        scope.launch {
            // The latest pre-triage goes with the next booking, unless the patient unticks it.
            val pending = loadPendingPreTriage(props.account)
            if (pending != null) {
                preTriage = pending
                reason = pending.form.reason
            }
            when (val result = loadSchedulingOptions(props.account)) {
                is SchedulingResult.Success -> options = result.value
                is SchedulingResult.Failed -> error = result.error
            }
        }
    }

    val loaded = options
    val specialty = loaded?.specialties?.firstOrNull { it.id == specialtyId }
    val professional = loaded?.professionals?.firstOrNull { it.id == professionalId }
    val clinicChoice = availability?.firstOrNull { it.clinicId == clinicId }
    val clinic = loaded?.clinic(clinicId)
    val days = clinicChoice?.days

    /** Asks the server for open times, then shows step 1. Keeps the choice if it is still open. */
    fun openDateStep() {
        val chosenSpecialty = specialtyId ?: return
        busy = true
        error = null
        scope.launch {
            val result = loadAvailability(props.account, chosenSpecialty, professionalId)
            busy = false
            when (result) {
                is SchedulingResult.Success -> {
                    val fresh = result.value
                    availability = fresh
                    // One clinic needs no choice; with several, a choice still offered is kept.
                    val freshClinic = fresh.singleOrNull() ?: fresh.firstOrNull { it.clinicId == clinicId }
                    clinicId = freshClinic?.clinicId
                    val freshDays = freshClinic?.days.orEmpty()
                    val freshDay = freshDays.firstOrNull { it.date == date }
                    if (freshDay == null) {
                        date = freshDays.firstOrNull()?.date
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
            val result = bookAppointment(
                props.account,
                CreateAppointmentRequest(
                    specialtyId = chosenSpecialty,
                    professionalId = professionalId,
                    clinicId = clinicId,
                    date = chosenDate,
                    time = chosenTime,
                    reason = reason.trim(),
                    notes = notes.trim(),
                    preTriageId = preTriage?.id?.takeIf { attachPreTriage },
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
                    if (clinicId == null) put("clinic", "Selecione a clínica.")
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
            menuSpace = true
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
                                            availability = null
                                            clinicId = null
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
                                    val offered = loaded.professionalsOf(specialty.id)
                                    // A specialty offered in several clinics names the city of each professional.
                                    val severalClinics = offered.map { it.clinicId }.distinct().size > 1
                                    offered.forEach { item ->
                                        val city = loaded.clinic(item.clinicId)?.city
                                        val label = if (severalClinics && city != null) "${item.name} ($city)" else item.name
                                        choiceChip(label, selected = item.id == professionalId) {
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
                        preTriage?.let { pending ->
                            preTriageChoice(pending, attachPreTriage) { attachPreTriage = it }
                        }
                    }
                    1 -> View {
                        style = Styles.card
                        sectionTitle("Data e Horário")
                        infoAlert("Horários disponíveis são validados pelo servidor no momento da confirmação.")
                        val clinics = availability.orEmpty()
                        if (clinics.size > 1) {
                            formField(caption = "Clínica", required = true) {
                                View {
                                    style = Styles.chipWrap
                                    accessibilityRole = "radiogroup"
                                    clinics.forEach { option ->
                                        val place = loaded.clinic(option.clinicId)
                                        choiceChip(
                                            place?.let { "${it.name} · ${it.city}" } ?: option.clinicId.toString(),
                                            selected = option.clinicId == clinicId,
                                        ) {
                                            if (option.clinicId != clinicId) {
                                                clinicId = option.clinicId
                                                date = option.days.firstOrNull()?.date
                                                time = null
                                            }
                                            fieldErrors = fieldErrors - "clinic"
                                        }
                                    }
                                }
                                fieldError(fieldErrors["clinic"])
                            }
                        }
                        val openDays = days.orEmpty()
                        val note = clinic?.let { clinicZoneNote(it, date ?: openDays.firstOrNull()?.date ?: return@let null) }
                        if (note != null) {
                            infoAlert(note)
                        }
                        if (clinic == null && clinics.size > 1) {
                            Text {
                                style = Styles.fieldHint
                                +"Escolha a clínica para ver as datas e os horários."
                            }
                        } else if (openDays.isEmpty()) {
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
                                        choiceChip(formatTime(option), selected = option == time, style = Styles.timeChip) {
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
                                style = Styles.summaryList
                                val phone = props.account.phone
                                summaryRow("Especialidade", specialty?.name.orEmpty())
                                summaryRow("Profissional", professional?.name ?: "Qualquer disponível")
                                summaryRow("Clínica", clinic?.let { "${it.name} · ${it.city}" }.orEmpty())
                                summaryRow("Data", date?.let(::formatDate).orEmpty())
                                summaryRow(
                                    "Horário",
                                    if (date != null && time != null && clinic != null) formatClinicTime(date!!, time!!, clinic) else "",
                                )
                                preTriage?.let { pending ->
                                    summaryRow(
                                        "Pré-triagem",
                                        if (attachPreTriage) "Vinculada (${formatDateTime(pending.createdAt)})" else "Não vinculada",
                                    )
                                }
                                summaryRow(
                                    "Paciente",
                                    props.account.socialName.ifBlank { props.account.fullName },
                                    last = phone.isEmpty(),
                                )
                                if (!phone.isEmpty()) {
                                    summaryRow("Telefone", phone.formatted(), last = true)
                                }
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
                        infoAlertRich(icon = { emoji("📱") }) {
                            if (!props.account.phone.isEmpty()) {
                                +"Confirmação enviada para "
                                strongText(props.account.phone.formatted())
                                +". "
                            } else {
                                +"A confirmação será enviada para o telefone principal do seu cadastro. "
                            }
                            +"Informações médicas não serão expostas em notificações."
                        }
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
                    val canContinue = !busy && !(step == 1 && availability.orEmpty().all { it.days.isEmpty() })
                    primaryButton(action, enabled = canContinue, grow = true, onPress = ::next)
                }
            }
        }
        AppFooter()
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
