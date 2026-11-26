package grupotp.medcall.server.service

import grupotp.medcall.common.clinic.AppointmentEventType
import grupotp.medcall.common.clinic.EventOrigin
import grupotp.medcall.common.kpi.ClinicDashboard
import grupotp.medcall.common.kpi.ClinicRanking
import grupotp.medcall.common.kpi.ClinicStatus
import grupotp.medcall.common.kpi.DailyPoint
import grupotp.medcall.common.kpi.IntegrationMode
import grupotp.medcall.common.kpi.IntegrationModeCount
import grupotp.medcall.common.kpi.NoShowCell
import grupotp.medcall.common.kpi.OccupancyCell
import grupotp.medcall.common.kpi.PeakCell
import grupotp.medcall.common.kpi.PlatformDashboard
import grupotp.medcall.common.kpi.PlatformWeek
import grupotp.medcall.common.kpi.ProfessionalKpi
import grupotp.medcall.common.kpi.Rate
import grupotp.medcall.common.kpi.Shift
import grupotp.medcall.common.kpi.SpecialtyKpi
import grupotp.medcall.common.kpi.WeeklyConfirmation
import grupotp.medcall.common.scheduling.AppointmentStatus
import grupotp.medcall.server.repository.AppointmentFact
import grupotp.medcall.server.repository.ClinicFact
import grupotp.medcall.server.repository.KpiFacts
import grupotp.medcall.server.repository.KpiRepository
import grupotp.medcall.server.repository.ProfessionalFact
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

/**
 * Computes the KPIs of docs/kpis.md. Each formula is commented with its code there; change the
 * catalog first, then the code.
 *
 * Dates of a clinic are always its own wall clock: an appointment booked at 23:30 in Manaus counts
 * on that day in Manaus, even if it is already the next day in UTC.
 */
class KpiService(
    private val repository: KpiRepository,
    private val clock: Clock,
) {
    /** Dashboard of the clinic portal. Null when the clinic does not exist. */
    fun clinicDashboard(clinicId: Int, from: LocalDate, to: LocalDate, specialtyId: Int? = null, professionalId: Int? = null): ClinicDashboard? {
        val facts = load(listOf(clinicId), from, to)
        val clinic = facts.clinics.singleOrNull() ?: return null
        return Scope(facts, listOf(clinic), now(), specialtyId, professionalId).clinicDashboard(from, to)
    }

    /**
     * The same dashboard seen from the MedCall panel: rates over fewer than [Rate.MIN_GROUP] cases
     * are masked, as in the rest of the panel.
     */
    fun clinicDashboardForMedcall(clinicId: Int, from: LocalDate, to: LocalDate): ClinicDashboard? =
        clinicDashboard(clinicId, from, to)?.let(::masked)

    /** Overview of the MedCall panel, over [clinicIds] or every clinic. */
    fun platformDashboard(from: LocalDate, to: LocalDate, clinicIds: Set<Int>? = null): PlatformDashboard {
        val facts = load(clinicIds, from, to)
        val now = now()
        val days = from.daysUntil(to) + 1
        val previousFrom = from.minus(DatePeriod(days = days))
        val all = Scope(facts, facts.clinics, now)
        val current = all.bookedIn(from, to)
        val previous = all.bookedIn(previousFrom, from.minus(DatePeriod(days = 1)))
        val signUps = repository.signUps(utcStart(previousFrom)..utcEnd(to))
        val signUpDates = signUps.map { it.toLocalDateTime(TimeZone.UTC).date }
        val lastBooking = repository.lastBookingByClinic()
        val (delivered, attempted, dead) = repository.deliveries(utcStart(from)..utcEnd(to))
        val rankings = facts.clinics.sortedBy { it.name }.map { clinic ->
            val scope = Scope(facts, listOf(clinic), now)
            val booked = scope.bookedIn(from, to)
            ClinicRanking(
                clinicId = clinic.id,
                name = clinic.name,
                city = clinic.city,
                status = clinic.status,
                integrationMode = clinic.mode(),
                appointments = maskCount(booked.size),
                occupancy = mask(scope.occupancy(from, to)),
                confirmation = mask(scope.confirmation(booked)),
                medianConfirmationHours = scope.confirmationHours(booked).takeIf { it.size >= Rate.MIN_GROUP }?.let(::median),
                noShow = mask(scope.noShow(from, to)),
                pendingOver24h = maskCount(scope.pendingOver24h()),
                lastAppointmentAt = lastBooking[clinic.id],
            )
        }
        return PlatformDashboard(
            from = from,
            to = to,
            newPatients = signUpDates.count { it in from..to },
            newPatientsPrevious = signUpDates.count { it < from },
            patientsWhoBooked = current.map { it.patientId }.distinct().size,
            patientsWhoBookedPrevious = previous.map { it.patientId }.distinct().size,
            appointments = current.size,
            appointmentsPrevious = previous.size,
            activeClinics = current.map { it.clinicId }.distinct().size,
            totalClinics = facts.clinics.count { it.status == ClinicStatus.Active },
            medianConfirmationHours = median(all.confirmationHours(current)),
            preTriageUsage = Rate(current.count { it.hasPreTriage }, current.size),
            confirmation = all.confirmation(current),
            noShow = all.noShow(from, to),
            occupancy = all.occupancy(from, to),
            weekly = weeksOf(from, to).map { week ->
                val end = week.plus(DatePeriod(days = 6))
                PlatformWeek(
                    weekStart = week,
                    newPatients = signUpDates.count { it in week..end && it in from..to },
                    appointments = all.bookedIn(maxOf(week, from), minOf(end, to)).size,
                )
            },
            clinics = rankings,
            integrationModes = IntegrationMode.entries.map { mode ->
                IntegrationModeCount(mode, facts.clinics.count { it.status == ClinicStatus.Active && it.mode() == mode })
            },
            webhookDeliveries = Rate(delivered, attempted),
            deadLetters = dead,
        )
    }

    private fun load(clinicIds: Collection<Int>?, from: LocalDate, to: LocalDate): KpiFacts {
        val days = from.daysUntil(to) + 1
        val previousFrom = from.minus(DatePeriod(days = days))
        // A day longer on each side covers every clinic's zone; the exact cut is made per clinic.
        val booked = utcStart(previousFrom.minus(DatePeriod(days = 1)))..utcEnd(to.plus(DatePeriod(days = 1)))
        val today = now().toLocalDateTime(TimeZone.UTC).date
        return repository.facts(clinicIds, previousFrom..to, booked, pendingFrom = today.minus(DatePeriod(days = 1)))
    }

    /**
     * The appointments and professionals of some clinics, optionally narrowed to a specialty or a
     * professional, with the formulas over them.
     */
    private class Scope(
        private val facts: KpiFacts,
        clinics: List<ClinicFact>,
        private val now: Instant,
        specialtyId: Int? = null,
        professionalId: Int? = null,
    ) {
        private val zones: Map<Int, TimeZone> = clinics.associate { it.id to TimeZone.of(it.timeZoneId) }

        val professionals: List<ProfessionalFact> = facts.professionals.filter {
            it.clinicId in zones && (specialtyId == null || it.specialtyId == specialtyId) &&
                (professionalId == null || it.id == professionalId)
        }
        private val professionalIds = professionals.map { it.id }.toSet()

        val appointments: List<AppointmentFact> = facts.appointments.filter { it.professionalId in professionalIds }

        private fun zone(a: AppointmentFact): TimeZone = zones.getValue(a.clinicId)

        // Zone conversions are the costly part; each appointment's are computed once.
        private val bookedDates: Map<Int, LocalDate> = appointments.associate { it.id to it.createdAt.toLocalDateTime(zone(it)).date }
        private val starts: Map<Int, Instant> = appointments.associate { it.id to LocalDateTime(it.date, it.time).toInstant(zone(it)) }

        fun bookedOn(a: AppointmentFact): LocalDate = bookedDates.getValue(a.id)

        private fun startsAt(a: AppointmentFact): Instant = starts.getValue(a.id)

        private fun hasStarted(a: AppointmentFact): Boolean = startsAt(a) <= now

        /** C01: booked in the period, any status. */
        fun bookedIn(from: LocalDate, to: LocalDate): List<AppointmentFact> = appointments.filter { bookedOn(it) in from..to }

        private fun consultationsIn(from: LocalDate, to: LocalDate) = appointments.filter { it.date in from..to }

        /** C02: slots held ÷ slots offered (active professionals × weekdays × fixed grid). */
        fun occupancy(from: LocalDate, to: LocalDate, among: List<ProfessionalFact> = professionals): Rate {
            val ids = among.map { it.id }.toSet()
            val offered = among.count { it.active } * weekdays(from, to).size * SchedulingService.OPENING_TIMES.size
            val taken = consultationsIn(from, to).count { it.professionalId in ids && it.status.holdsSlot }
            return Rate(taken, offered)
        }

        /** The clinic's first answer: confirm, reschedule or decline, not the patient's cancel. */
        private fun firstDecision(a: AppointmentFact) = facts.events[a.id].orEmpty().firstOrNull {
            it.type in DECISIONS && it.origin != EventOrigin.PatientApp
        }

        /** C03: confirmed (or rescheduled) ÷ answered, by first answer. */
        fun confirmation(booked: List<AppointmentFact>): Rate {
            val decisions = booked.mapNotNull(::firstDecision)
            return Rate(decisions.count { it.type != AppointmentEventType.Declined }, decisions.size)
        }

        /** C04 samples: hours from booking to the clinic's first answer. */
        fun confirmationHours(booked: List<AppointmentFact>): List<Double> = booked.mapNotNull { a ->
            firstDecision(a)?.let { hours(it.occurredAt - a.createdAt) }
        }

        /** C05: still pending, booked more than 24 h ago, consultation not started. */
        fun pendingOver24h(): Int = appointments.count {
            it.status == AppointmentStatus.Pending && now - it.createdAt > 24.hours && !hasStarted(it)
        }

        /** C06: cancelled by the patient ÷ booked. */
        private fun patientCancellation(booked: List<AppointmentFact>): Rate = Rate(
            booked.count { a -> facts.events[a.id].orEmpty().any { it.type == AppointmentEventType.Cancelled && it.origin == EventOrigin.PatientApp } },
            booked.size,
        )

        /** C07: no-shows ÷ consultations with an attendance mark. */
        fun noShow(from: LocalDate, to: LocalDate, among: List<AppointmentFact> = consultationsIn(from, to)): Rate {
            val marked = among.filter { it.status == AppointmentStatus.Attended || it.status == AppointmentStatus.NoShow }
            return Rate(marked.count { it.status == AppointmentStatus.NoShow }, marked.size)
        }

        /** C07 coverage: marked ÷ past consultations that were confirmed. */
        private fun attendanceCoverage(from: LocalDate, to: LocalDate): Rate {
            val past = consultationsIn(from, to).filter {
                hasStarted(it) && it.status in setOf(AppointmentStatus.Confirmed, AppointmentStatus.Attended, AppointmentStatus.NoShow)
            }
            return Rate(past.count { it.status != AppointmentStatus.Confirmed }, past.size)
        }

        fun clinicDashboard(from: LocalDate, to: LocalDate): ClinicDashboard {
            val days = from.daysUntil(to) + 1
            val booked = bookedIn(from, to)
            val previous = bookedIn(from.minus(DatePeriod(days = days)), from.minus(DatePeriod(days = 1)))
            val consultations = consultationsIn(from, to)
            val dates = generateSequence(from) { it.plus(DatePeriod(days = 1)) }.takeWhile { it <= to }.toList()
            val bookedByDay = booked.groupingBy { bookedOn(it) }.eachCount()
            val consultationsByDay = consultations.groupBy { it.date }
            val bySpecialty = professionals.groupBy { it.specialtyId }
            val weekdayDates = weekdays(from, to)
            return ClinicDashboard(
                from = from,
                to = to,
                received = booked.size,
                receivedPrevious = previous.size,
                occupancy = occupancy(from, to),
                confirmation = confirmation(booked),
                medianConfirmationHours = median(confirmationHours(booked)),
                pendingOver24h = pendingOver24h(),
                patientCancellation = patientCancellation(booked),
                noShow = noShow(from, to),
                attendanceCoverage = attendanceCoverage(from, to),
                // C08: days from booking to the consultation.
                medianLeadDays = median(booked.map { (startsAt(it) - it.createdAt).inWholeMinutes / MINUTES_PER_DAY }),
                daily = dates.map { date ->
                    val onDay = consultationsByDay[date].orEmpty()
                    DailyPoint(
                        date = date,
                        received = bookedByDay[date] ?: 0,
                        attended = onDay.count { it.status == AppointmentStatus.Attended },
                        noShows = onDay.count { it.status == AppointmentStatus.NoShow },
                    )
                },
                occupancyBySpecialtyDay = bySpecialty.flatMap { (specialtyId, among) ->
                    val ids = among.map { it.id }.toSet()
                    val offeredPerDay = among.count { it.active } * SchedulingService.OPENING_TIMES.size
                    weekdayDates.map { date ->
                        OccupancyCell(
                            specialtyId = specialtyId,
                            date = date,
                            taken = consultationsByDay[date].orEmpty().count { it.professionalId in ids && it.status.holdsSlot },
                            offered = offeredPerDay,
                        )
                    }
                },
                confirmationByWeek = weeksOf(from, to).map { week ->
                    val inWeek = booked.filter { weekStart(bookedOn(it)) == week }
                    val hours = confirmationHours(inWeek)
                    WeeklyConfirmation(
                        weekStart = week,
                        medianHours = median(hours),
                        answered = hours.size,
                        stillPending = inWeek.count { it.status == AppointmentStatus.Pending },
                    )
                },
                noShowByWeekdayShift = (1..5).flatMap { day ->
                    Shift.entries.map { shift ->
                        val cell = consultations.filter { it.date.dayOfWeek.isoDayNumber == day && shiftOf(it.time) == shift }
                        val rate = noShow(from, to, cell)
                        NoShowCell(day, shift, rate.numerator, rate.denominator)
                    }
                },
                peak = consultations.filter { it.status.holdsSlot }
                    .groupingBy { it.date.dayOfWeek.isoDayNumber to it.time }
                    .eachCount()
                    .map { (key, count) -> PeakCell(key.first, key.second, count) }
                    .sortedWith(compareBy({ it.dayOfWeek }, { it.time })),
                professionals = professionals.sortedBy { it.name }.map { professional ->
                    val mine = booked.filter { it.professionalId == professional.id }
                    ProfessionalKpi(
                        professionalId = professional.id,
                        name = professional.name,
                        specialtyName = professional.specialtyName,
                        occupancy = occupancy(from, to, listOf(professional)),
                        noShow = noShow(from, to, consultations.filter { it.professionalId == professional.id }),
                        medianConfirmationHours = median(confirmationHours(mine)),
                    )
                },
                specialties = bySpecialty.map { (specialtyId, among) ->
                    val ids = among.map { it.id }.toSet()
                    SpecialtyKpi(
                        specialtyId = specialtyId,
                        name = among.first().specialtyName,
                        occupancy = occupancy(from, to, among),
                        received = booked.count { it.professionalId in ids },
                    )
                }.sortedBy { it.name },
            )
        }
    }

    /**
     * The clinic's appointments of the period as CSV, for the "base utilizada" of the Data Science
     * evaluation: one row per appointment, no patient data. Columns in docs/base-sintetica.md.
     * Separator `;` and decimal comma, so Excel in Portuguese opens it as is.
     */
    fun clinicCsv(clinicId: Int, from: LocalDate, to: LocalDate): String? {
        val facts = load(listOf(clinicId), from, to)
        val clinic = facts.clinics.singleOrNull() ?: return null
        val zone = TimeZone.of(clinic.timeZoneId)
        val professionals = facts.professionals.associateBy { it.id }
        val rows = facts.appointments
            .filter { it.date in from..to || it.createdAt.toLocalDateTime(zone).date in from..to }
            .sortedBy { it.id }
        val header = listOf(
            "agendamento_id", "especialidade", "profissional", "data_consulta", "hora_consulta", "dia_semana", "turno",
            "situacao", "agendado_em", "primeira_resposta", "primeira_resposta_em", "horas_ate_resposta", "origem_resposta",
            "cancelado_pelo_paciente", "tem_pre_triagem", "antecedencia_dias",
        )
        val lines = rows.map { a ->
            val professional = professionals.getValue(a.professionalId)
            val events = facts.events[a.id].orEmpty()
            val decision = events.firstOrNull { it.type in DECISIONS && it.origin != EventOrigin.PatientApp }
            val bookedAt = a.createdAt.toLocalDateTime(zone)
            val start = LocalDateTime(a.date, a.time).toInstant(zone)
            listOf(
                a.id.toString(),
                professional.specialtyName,
                professional.name,
                a.date.toString(),
                a.time.toString(),
                WEEKDAYS[a.date.dayOfWeek.isoDayNumber - 1],
                if (shiftOf(a.time) == Shift.Morning) "manha" else "tarde",
                STATUS_NAMES.getValue(a.status),
                bookedAt.toString().replace('T', ' ').take(16),
                decision?.type?.let { DECISION_NAMES[it] }.orEmpty(),
                decision?.occurredAt?.toLocalDateTime(zone)?.toString()?.replace('T', ' ')?.take(16).orEmpty(),
                decision?.let { decimal(hours(it.occurredAt - a.createdAt)) }.orEmpty(),
                decision?.origin?.let { ORIGIN_NAMES[it] }.orEmpty(),
                if (events.any { it.type == AppointmentEventType.Cancelled && it.origin == EventOrigin.PatientApp }) "sim" else "nao",
                if (a.hasPreTriage) "sim" else "nao",
                decimal((start - a.createdAt).inWholeMinutes / MINUTES_PER_DAY),
            ).joinToString(";") { it.replace(";", ",") }
        }
        return BOM + (listOf(header.joinToString(";")) + lines).joinToString(CRLF, postfix = CRLF)
    }

    private fun ClinicFact.mode(): IntegrationMode = if (hasActiveWebhook) IntegrationMode.Webhook else IntegrationMode.Manual

    private fun masked(dashboard: ClinicDashboard): ClinicDashboard = dashboard.copy(
        confirmation = mask(dashboard.confirmation),
        patientCancellation = mask(dashboard.patientCancellation),
        noShow = mask(dashboard.noShow),
        attendanceCoverage = mask(dashboard.attendanceCoverage),
        noShowByWeekdayShift = dashboard.noShowByWeekdayShift.map {
            if (it.marked < Rate.MIN_GROUP) it.copy(noShows = 0, marked = 0) else it
        },
        professionals = dashboard.professionals.map { it.copy(noShow = mask(it.noShow)) },
    )

    private fun now(): Instant = clock.instant().toKotlinInstant()

    private companion object {
        const val BOM = "﻿"
        const val CRLF = "\r\n"
        val WEEKDAYS = listOf("segunda", "terca", "quarta", "quinta", "sexta", "sabado", "domingo")
        val STATUS_NAMES = mapOf(
            AppointmentStatus.Pending to "aguardando",
            AppointmentStatus.Confirmed to "confirmada",
            AppointmentStatus.Declined to "recusada",
            AppointmentStatus.Cancelled to "cancelada",
            AppointmentStatus.Attended to "compareceu",
            AppointmentStatus.NoShow to "faltou",
        )
        val DECISION_NAMES = mapOf(
            AppointmentEventType.Confirmed to "confirmou",
            AppointmentEventType.Rescheduled to "remarcou",
            AppointmentEventType.Declined to "recusou",
        )
        val ORIGIN_NAMES = mapOf(
            EventOrigin.ClinicPortal to "portal",
            EventOrigin.PublicApi to "api",
            EventOrigin.DemoSeed to "portal",
            EventOrigin.PatientApp to "app",
        )

        fun decimal(value: Double): String = (Math.round(value * 10) / 10.0).toString().replace('.', ',')

        val DECISIONS = setOf(AppointmentEventType.Confirmed, AppointmentEventType.Rescheduled, AppointmentEventType.Declined)
        const val MINUTES_PER_DAY = 1440.0

        fun mask(rate: Rate): Rate = if (rate.denominator in 1 until Rate.MIN_GROUP) Rate.MASKED else rate

        fun maskCount(count: Int): Int? = if (count in 1 until Rate.MIN_GROUP) null else count

        fun hours(duration: Duration): Double = duration.inWholeMinutes / 60.0

        fun median(values: List<Double>): Double? {
            if (values.isEmpty()) return null
            val sorted = values.sorted()
            val middle = sorted.size / 2
            val value = if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2
            return Math.round(value * 10) / 10.0
        }

        fun weekdays(from: LocalDate, to: LocalDate): List<LocalDate> =
            generateSequence(from) { it.plus(DatePeriod(days = 1)) }
                .takeWhile { it <= to }
                .filter { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
                .toList()

        fun weekStart(date: LocalDate): LocalDate = date.minus(DatePeriod(days = date.dayOfWeek.isoDayNumber - 1))

        fun weeksOf(from: LocalDate, to: LocalDate): List<LocalDate> =
            generateSequence(weekStart(from)) { it.plus(DatePeriod(days = 7)) }.takeWhile { it <= to }.toList()

        fun shiftOf(time: LocalTime): Shift = if (time.hour < 12) Shift.Morning else Shift.Afternoon

        fun utcStart(date: LocalDate): Instant = date.atStartOfDayIn(TimeZone.UTC)

        fun utcEnd(date: LocalDate): Instant = date.plus(DatePeriod(days = 1)).atStartOfDayIn(TimeZone.UTC) - 1.milliseconds
    }
}
