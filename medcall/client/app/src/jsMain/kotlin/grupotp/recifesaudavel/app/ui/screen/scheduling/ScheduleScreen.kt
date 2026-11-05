package grupotp.recifesaudavel.app.ui.screen.scheduling

import grupotp.recifesaudavel.app.di.useInject
import grupotp.recifesaudavel.app.domain.RemoteError
import grupotp.recifesaudavel.app.domain.RemoteResult
import grupotp.recifesaudavel.app.domain.scheduling.SchedulingOptions
import grupotp.recifesaudavel.app.ui.component.alert.sessionExpiredAlert
import grupotp.recifesaudavel.app.ui.component.button.primaryButton
import grupotp.recifesaudavel.app.ui.component.button.secondaryButton
import grupotp.recifesaudavel.app.ui.component.layout.AppFooter
import grupotp.recifesaudavel.app.ui.component.layout.ScreenHeader
import grupotp.recifesaudavel.app.ui.component.layout.StepIndicator
import grupotp.recifesaudavel.app.ui.hook.useScreenScope
import grupotp.recifesaudavel.app.ui.screen.scheduling.step.SchedulingStep3Confirmation
import grupotp.recifesaudavel.app.ui.screen.scheduling.step.SchedulingStep2DateTime
import grupotp.recifesaudavel.app.ui.screen.scheduling.step.SchedulingStep1Specialty
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
    var invalidFields by useState<Map<String, String>>(emptyMap())
    var error by useState<RemoteError?>(null)
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
                is RemoteResult.Success -> options = result.value
                is RemoteResult.Failed -> error = result.error
            }
        }
    }

    val loaded = options
    val specialty = loaded?.specialties?.firstOrNull { it.id == specialtyId }
    val professional = loaded?.professionals?.firstOrNull { it.id == professionalId }
    val clinic = loaded?.clinic(clinicId)

    /** A different specialty starts the professional, clinic, day and time over. */
    fun chooseSpecialty(id: Int) {
        if (id != specialtyId) {
            specialtyId = id
            professionalId = null
            availability = null
            clinicId = null
            date = null
            time = null
        }
        invalidFields = emptyMap()
    }

    /** A different clinic shows its first open day and no time. */
    fun chooseClinic(option: ClinicAvailability) {
        if (option.clinicId != clinicId) {
            clinicId = option.clinicId
            date = option.days.firstOrNull()?.date
            time = null
        }
        invalidFields = invalidFields - "clinic"
    }

    fun chooseDate(day: LocalDate) {
        if (day != date) {
            date = day
            time = null
        }
        invalidFields = invalidFields - "date"
    }

    fun chooseTime(option: LocalTime) {
        time = option
        invalidFields = invalidFields - "time"
    }

    /** Asks the server for open times, then shows step 1. Keeps the choice if it is still open. */
    fun openDateStep() {
        val chosenSpecialty = specialtyId ?: return
        busy = true
        error = null
        scope.launch {
            val result = loadAvailability(props.account, chosenSpecialty, professionalId)
            busy = false
            when (result) {
                is RemoteResult.Success -> {
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
                is RemoteResult.Failed -> error = result.error
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
                is RemoteResult.Success -> props.onBooked(result.value)
                is RemoteResult.Failed -> error = result.error
            }
        }
    }

    fun next() {
        when (step) {
            0 -> if (specialtyId == null) {
                invalidFields = mapOf("specialty" to "Selecione a especialidade.")
            } else {
                openDateStep()
            }
            1 -> {
                val errors = buildMap {
                    if (clinicId == null) put("clinic", "Selecione a clínica.")
                    if (date == null) put("date", "Selecione uma data.")
                    if (time == null) put("time", "Selecione um horário.")
                }
                invalidFields = errors
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
        invalidFields = emptyMap()
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
                    0 -> SchedulingStep1Specialty {
                        this.options = loaded
                        this.specialtyId = specialtyId
                        this.professionalId = professionalId
                        this.reason = reason
                        this.preTriage = preTriage
                        this.attachPreTriage = attachPreTriage
                        fieldErrors = invalidFields
                        onSpecialty = ::chooseSpecialty
                        onProfessional = { professionalId = it }
                        onReason = { reason = it }
                        onAttachPreTriage = { attachPreTriage = it }
                    }
                    1 -> SchedulingStep2DateTime {
                        this.options = loaded
                        this.availability = availability.orEmpty()
                        this.clinicId = clinicId
                        this.date = date
                        this.time = time
                        fieldErrors = invalidFields
                        onClinic = ::chooseClinic
                        onDate = ::chooseDate
                        onTime = ::chooseTime
                    }
                    else -> SchedulingStep3Confirmation {
                        account = props.account
                        this.specialty = specialty
                        this.professional = professional
                        this.clinic = clinic
                        this.date = date
                        this.time = time
                        this.preTriage = preTriage
                        this.attachPreTriage = attachPreTriage
                        this.notes = notes
                        onNotes = { notes = it }
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
