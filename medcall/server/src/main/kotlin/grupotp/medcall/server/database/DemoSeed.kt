package grupotp.medcall.server.database

import grupotp.medcall.common.account.Phone
import grupotp.medcall.common.clinic.AppointmentEventType
import grupotp.medcall.common.clinic.DeclineReason
import grupotp.medcall.common.clinic.EventOrigin
import grupotp.medcall.common.kpi.ClinicStatus
import grupotp.medcall.common.pretriage.PreTriageForm
import grupotp.medcall.common.scheduling.AppointmentStatus
import grupotp.medcall.common.security.SecretString
import grupotp.medcall.common.staff.StaffRole
import grupotp.medcall.server.database.table.AppointmentEventsTable
import grupotp.medcall.server.database.table.AppointmentsTable
import grupotp.medcall.server.database.table.ClinicProfilesTable
import grupotp.medcall.server.database.table.ClinicsTable
import grupotp.medcall.server.database.table.PreTriagesTable
import grupotp.medcall.server.database.table.ProfessionalProfilesTable
import grupotp.medcall.server.database.table.ProfessionalsTable
import grupotp.medcall.server.database.table.SpecialtiesTable
import grupotp.medcall.server.database.table.UsersTable
import grupotp.medcall.server.repository.DeclineDetail
import grupotp.medcall.server.security.PasswordHasher
import grupotp.medcall.server.service.SchedulingService
import grupotp.medcall.server.service.StaffService
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Clock
import kotlin.math.exp
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

/**
 * Fictitious data for the dashboards (docs/base-sintetica.md): a third clinic, ~400 patients and
 * about 90 days of appointments with their events, plus demo staff accounts.
 *
 * Only runs when `DEMO_SEED=true` and the database has no appointment yet; production never sets
 * it. The random generator has a fixed seed, so the same day gives the same numbers.
 *
 * The data carries on purpose one pattern for each question of the Data Science presentation
 * (docs/kpis.md):
 * 1. Ortopedia fills the agenda in the first 10 days of every month.
 * 2. Clínica Boa Vista's reception got slow in the last three weeks (time until confirmation).
 * 3. More no-shows on Monday mornings.
 */
class DemoSeed(
    private val database: Database,
    private val passwords: PasswordHasher,
    private val staff: StaffService,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(DemoSeed::class.java)
    private val random = Random(SEED)
    private val json = Json

    private class Prof(val id: Int, val clinicId: Int, val specialty: String, val zone: TimeZone)

    fun runIfEmpty(staffPassword: SecretString) {
        val empty = transaction(database) { AppointmentsTable.selectAll().limit(1).empty() }
        if (!empty) {
            return
        }
        val started = System.currentTimeMillis()
        val now = clock.instant().toKotlinInstant()
        val clinics = ensureClinics(now)
        createStaff(clinics, staffPassword)
        val patients = createPatients(now)
        val count = createAppointments(now, patients)
        log.info("Demo data: {} appointments in {} ms.", count, System.currentTimeMillis() - started)
    }

    /** The catalog's clinics plus Policlínica Derby and a few more professionals; all active. */
    private fun ensureClinics(now: Instant): Map<String, Int> = transaction(database) {
        val specialties = SpecialtiesTable.selectAll().associate { it[SpecialtiesTable.name] to it[SpecialtiesTable.id].value }
        val clinics = ClinicsTable.selectAll().associate { it[ClinicsTable.name] to it[ClinicsTable.id].value }.toMutableMap()
        val derby = clinics[DERBY] ?: ClinicsTable.insertAndGetId {
            it[name] = DERBY
            it[city] = "Recife"
            it[timeZone] = "America/Recife"
        }.value.also { clinics[DERBY] = it }
        fun professional(name: String, specialty: String, clinicId: Int) {
            ProfessionalsTable.insert {
                it[this.name] = name
                it[this.specialty] = specialties.getValue(specialty)
                it[clinic] = clinicId
            }
        }
        professional("Dr. Caio Lins", "Ortopedia", derby)
        professional("Dra. Marina Tavares", "Ortopedia", derby)
        professional("Dr. Igor Brandão", "Clínica Geral", derby)
        professional("Dra. Lívia Moraes", "Pediatria", derby)
        professional("Dr. Otávio Rangel", "Cardiologia", derby)
        professional("Dra. Helena Prado", "Dermatologia", derby)
        val manaus = clinics.getValue(SchedulingCatalog.manaus.name)
        professional("Dr. Raimundo Farias", "Clínica Geral", manaus)
        professional("Dra. Iara Batista", "Pediatria", manaus)
        val addresses = mapOf(
            SchedulingCatalog.boaVista.name to ("Rua da Aurora, 100, Boa Vista" to "(81) 3000-1000"),
            SchedulingCatalog.manaus.name to ("Av. Eduardo Ribeiro, 520, Centro" to "(92) 3000-2000"),
            DERBY to ("Praça do Derby, 45, Derby" to "(81) 3000-3000"),
        )
        clinics.forEach { (name, id) ->
            if (ClinicProfilesTable.selectAll().where { ClinicProfilesTable.clinic eq id }.empty()) {
                ClinicProfilesTable.insert {
                    it[clinic] = id
                    it[status] = ClinicStatus.Active.wireName()
                    it[address] = addresses[name]?.first.orEmpty()
                    it[phone] = addresses[name]?.second.orEmpty()
                    it[createdAt] = now
                }
            }
        }
        ProfessionalsTable.selectAll().forEach { row ->
            val id = row[ProfessionalsTable.id].value
            if (ProfessionalProfilesTable.selectAll().where { ProfessionalProfilesTable.professional eq id }.empty()) {
                ProfessionalProfilesTable.insert {
                    it[professional] = id
                    it[registration] = "CRM-${if (row[ProfessionalsTable.clinic].value == manaus) "AM" else "PE"} ${10000 + id * 37}"
                    it[active] = true
                }
            }
        }
        clinics
    }

    private fun createStaff(clinics: Map<String, Int>, password: SecretString) {
        if (password.isBlank()) {
            log.warn("DEMO_STAFF_PASSWORD is blank: no demo staff accounts were created.")
            return
        }
        val hash = passwords.hash(password)
        val slugs = mapOf(SchedulingCatalog.boaVista.name to "boavista", SchedulingCatalog.manaus.name to "manaus", DERBY to "derby")
        staff.createDemoAccount("Equipe MedCall (demo)", "medcall@demo.medcall.dev", StaffRole.MedcallAdmin, null, hash)
        clinics.forEach { (name, id) ->
            val slug = slugs[name] ?: return@forEach
            staff.createDemoAccount("Administração $name", "admin.$slug@demo.medcall.dev", StaffRole.ClinicAdmin, id, hash)
            staff.createDemoAccount("Recepção $name", "recepcao.$slug@demo.medcall.dev", StaffRole.Receptionist, id, hash)
        }
    }

    /** Patients, signing up a little faster every month. Returns (id, sign-up time). */
    private fun createPatients(now: Instant): List<Pair<Int, Instant>> = transaction(database) {
        // Demo patients never sign in: one hash of a random password serves them all.
        val hash = passwords.hash(SecretString(java.util.UUID.randomUUID().toString()))
        val rows = (1..PATIENTS).map { index ->
            // Square root skews sign-ups towards recent days: the base grows.
            val daysAgo = (SIGN_UP_DAYS * kotlin.math.sqrt(random.nextDouble())).toInt()
            val createdAt = now - (SIGN_UP_DAYS - daysAgo).toLong().let { it * 24 }.hours + random.nextInt(600).minutes
            Triple(index, createdAt.coerceAtMost(now - 1.hours), name())
        }
        UsersTable.batchInsert(rows) { (index, createdAt, name) ->
            this[UsersTable.fullName] = name
            this[UsersTable.socialName] = ""
            this[UsersTable.email] = "paciente%04d@demo.medcall.dev".format(index)
            this[UsersTable.passwordHash] = hash
            this[UsersTable.primaryPhone] = "819%04d%04d".format(random.nextInt(10000), random.nextInt(10000))
            this[UsersTable.primaryPhoneWhatsapp] = true
            this[UsersTable.secondaryPhone] = ""
            this[UsersTable.secondaryPhoneWhatsapp] = false
            this[UsersTable.emergencyContactName] = ""
            this[UsersTable.emergencyContactPhone] = ""
            this[UsersTable.acceptedPrivacyPolicy] = true
            this[UsersTable.acceptedTreatment] = true
            this[UsersTable.wantsNotifications] = true
            this[UsersTable.createdAt] = createdAt
            this[UsersTable.lastLoginAt] = createdAt
        }.map { it[UsersTable.id].value to it[UsersTable.createdAt] }
    }

    private class Planned(
        val prof: Prof,
        val patientId: Int,
        val patientName: String,
        val date: LocalDate,
        val time: kotlinx.datetime.LocalTime,
        val createdAt: Instant,
        val status: AppointmentStatus,
        val events: List<Triple<AppointmentEventType, Instant, String>>,
        val reason: String,
        val preTriage: Boolean,
    )

    private fun createAppointments(now: Instant, patients: List<Pair<Int, Instant>>): Int {
        val (profs, names) = transaction(database) {
            val zones = ClinicsTable.selectAll().associate { it[ClinicsTable.id].value to TimeZone.of(it[ClinicsTable.timeZone]) }
            val specialties = SpecialtiesTable.selectAll().associate { it[SpecialtiesTable.id].value to it[SpecialtiesTable.name] }
            val profs = ProfessionalsTable.selectAll().map {
                val clinicId = it[ProfessionalsTable.clinic].value
                Prof(it[ProfessionalsTable.id].value, clinicId, specialties.getValue(it[ProfessionalsTable.specialty].value), zones.getValue(clinicId))
            }
            profs to UsersTable.selectAll().associate { it[UsersTable.id].value to it[UsersTable.fullName] }
        }
        val clinicNames = transaction(database) { ClinicsTable.selectAll().associate { it[ClinicsTable.id].value to it[ClinicsTable.name] } }
        val planned = mutableListOf<Planned>()
        for (prof in profs) {
            val today = now.toLocalDateTime(prof.zone).date
            val clinicName = clinicNames.getValue(prof.clinicId)
            var date = today.minus(DatePeriod(days = HISTORY_DAYS))
            val last = today.plus(DatePeriod(days = SchedulingService.BOOKING_WINDOW_DAYS - 1))
            while (date <= last) {
                if (date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY) {
                    for (time in SchedulingService.OPENING_TIMES) {
                        plan(prof, clinicName, date, time, today, now, patients, names)?.let(planned::add)
                    }
                }
                date = date.plus(DatePeriod(days = 1))
            }
        }
        insert(planned)
        return planned.size
    }

    /** Whether this slot was booked, and what happened to it. */
    private fun plan(
        prof: Prof,
        clinicName: String,
        date: LocalDate,
        time: kotlinx.datetime.LocalTime,
        today: LocalDate,
        now: Instant,
        patients: List<Pair<Int, Instant>>,
        names: Map<Int, String>,
    ): Planned? {
        val start = LocalDateTime(date, time).toInstant(prof.zone)
        val daysAhead = date.toEpochDays() - today.toEpochDays()
        var chance = when (prof.specialty) {
            "Ortopedia" -> if (date.day <= 10) 0.94 else 0.58
            "Cardiologia" -> 0.68
            "Pediatria" -> if (time.hour < 12) 0.74 else 0.5
            "Dermatologia" -> 0.46
            "Psiquiatria" -> 0.62
            else -> 0.56
        }
        // Slow growth over the period, and fewer bookings the further ahead.
        chance *= 0.9 + 0.15 * (HISTORY_DAYS + minOf(daysAhead, 0L)).toDouble() / HISTORY_DAYS
        if (daysAhead > 0) {
            chance *= (1.0 - daysAhead / 18.0).coerceAtLeast(0.15)
            if (prof.specialty == "Ortopedia" && date.day <= 10) chance = maxOf(chance, 0.85)
        }
        if (random.nextDouble() >= chance.coerceAtMost(0.97)) {
            return null
        }
        // Booked 1 to ~13 days before, at a waking hour, never in the future.
        val leadDays = (1 + (-kotlin.math.ln(1 - random.nextDouble()) * 3.5).toInt()).coerceAtMost(13)
        val bookedDay = date.minus(DatePeriod(days = leadDays))
        var createdAt = LocalDateTime(bookedDay.year, bookedDay.month, bookedDay.day, 7 + random.nextInt(15), random.nextInt(60))
            .toInstant(prof.zone)
        // A booking cannot be in the future: those of upcoming days spread over the last two weeks.
        if (createdAt > now - 30.minutes) {
            createdAt = now - (30 + random.nextInt(60 * 24 * 13)).minutes
        }
        if (createdAt >= start) {
            return null
        }
        val (patientId, _) = patients.filter { it.second < createdAt }.ifEmpty { patients }.let { it[random.nextInt(it.size)] }
        val events = mutableListOf(Triple(AppointmentEventType.Created, createdAt, ""))
        var status = AppointmentStatus.Pending

        // The patient gives up on about 7%, some time before the appointment.
        if (random.nextDouble() < 0.07) {
            val at = createdAt + ((start - createdAt).inWholeMinutes * random.nextDouble()).toLong().minutes
            if (at < now) {
                events += Triple(AppointmentEventType.Cancelled, at, "")
                return planned(prof, patientId, names, date, time, createdAt, AppointmentStatus.Cancelled, events)
            }
        }
        var decidedAt = createdAt + confirmationDelay(clinicName, createdAt, now)
        // A clinic always answers before an appointment that already happened, if only at the last minute.
        if (start < now && decidedAt >= start) {
            decidedAt = createdAt + ((start - createdAt).inWholeMinutes * (0.5 + 0.45 * random.nextDouble())).toLong().minutes
        }
        if (decidedAt < now && decidedAt < start) {
            if (random.nextDouble() < 0.05) {
                val reason = DeclineReason.entries[random.nextInt(DeclineReason.entries.size - 1)]
                events += Triple(AppointmentEventType.Declined, decidedAt, DeclineDetail.encode(reason, ""))
                return planned(prof, patientId, names, date, time, createdAt, AppointmentStatus.Declined, events)
            }
            events += Triple(AppointmentEventType.Confirmed, decidedAt, "")
            status = AppointmentStatus.Confirmed
        }
        // Past confirmed appointments: most get an attendance mark a little after they start.
        val markedAt = start + (60 + random.nextInt(180)).minutes
        if (status == AppointmentStatus.Confirmed && markedAt < now && random.nextDouble() < coverage(clinicName)) {
            val noShow = random.nextDouble() < noShowChance(date, time)
            events += Triple(if (noShow) AppointmentEventType.NoShow else AppointmentEventType.Attended, markedAt, "")
            status = if (noShow) AppointmentStatus.NoShow else AppointmentStatus.Attended
        }
        return planned(prof, patientId, names, date, time, createdAt, status, events)
    }

    private fun planned(
        prof: Prof,
        patientId: Int,
        names: Map<Int, String>,
        date: LocalDate,
        time: kotlinx.datetime.LocalTime,
        createdAt: Instant,
        status: AppointmentStatus,
        events: List<Triple<AppointmentEventType, Instant, String>>,
    ) = Planned(prof, patientId, names.getValue(patientId), date, time, createdAt, status, events, REASONS[random.nextInt(REASONS.size)], random.nextDouble() < 0.4)

    /** Hours until the clinic answers: lognormal around each clinic's median. */
    private fun confirmationDelay(clinicName: String, createdAt: Instant, now: Instant): Duration {
        val median = when {
            clinicName == SchedulingCatalog.boaVista.name && now - createdAt < SLOW_RECEPTION -> 16.0
            clinicName == SchedulingCatalog.boaVista.name -> 3.0
            clinicName == SchedulingCatalog.manaus.name -> 30.0
            else -> 6.0
        }
        val hours = median * exp(gaussian() * 0.7)
        return (hours * 60).toLong().coerceAtLeast(5).minutes
    }

    private fun coverage(clinicName: String): Double = when (clinicName) {
        SchedulingCatalog.boaVista.name -> 0.93
        SchedulingCatalog.manaus.name -> 0.72
        else -> 0.86
    }

    private fun noShowChance(date: LocalDate, time: kotlinx.datetime.LocalTime): Double = when {
        date.dayOfWeek == DayOfWeek.MONDAY && time.hour < 12 -> 0.27
        date.dayOfWeek == DayOfWeek.FRIDAY && time.hour >= 12 -> 0.15
        else -> 0.08
    }

    private fun gaussian(): Double {
        val u = 1.0 - random.nextDouble()
        val v = random.nextDouble()
        return kotlin.math.sqrt(-2.0 * kotlin.math.ln(u)) * kotlin.math.cos(2.0 * Math.PI * v)
    }

    private fun insert(planned: List<Planned>) {
        planned.chunked(BATCH).forEach { chunk ->
            transaction(database) {
                val ids = AppointmentsTable.batchInsert(chunk) { a ->
                    this[AppointmentsTable.patient] = a.patientId
                    this[AppointmentsTable.professional] = a.prof.id
                    this[AppointmentsTable.date] = a.date
                    this[AppointmentsTable.time] = a.time
                    this[AppointmentsTable.status] = a.status.wireName()
                    this[AppointmentsTable.reason] = a.reason
                    this[AppointmentsTable.notes] = ""
                    this[AppointmentsTable.createdAt] = a.createdAt
                    this[AppointmentsTable.slotKey] = if (a.status.holdsSlot) AppointmentsTable.slotKeyOf(a.prof.id, a.date, a.time) else null
                }.map { it[AppointmentsTable.id].value }
                val events = chunk.zip(ids).flatMap { (a, id) -> a.events.map { Triple(a, id, it) } }
                AppointmentEventsTable.batchInsert(events) { (a, id, event) ->
                    this[AppointmentEventsTable.appointment] = id
                    this[AppointmentEventsTable.clinic] = a.prof.clinicId
                    this[AppointmentEventsTable.type] = event.first.wireName()
                    this[AppointmentEventsTable.occurredAt] = event.second
                    this[AppointmentEventsTable.origin] =
                        (if (event.first == AppointmentEventType.Created || event.first == AppointmentEventType.Cancelled) EventOrigin.PatientApp else EventOrigin.DemoSeed).wireName()
                    this[AppointmentEventsTable.detail] = event.third
                }
                val withPreTriage = chunk.zip(ids).filter { it.first.preTriage }
                PreTriagesTable.batchInsert(withPreTriage) { (a, id) ->
                    this[PreTriagesTable.patient] = a.patientId
                    this[PreTriagesTable.appointment] = id
                    this[PreTriagesTable.createdAt] = a.createdAt - 5.minutes
                    this[PreTriagesTable.form] = json.encodeToString(
                        PreTriageForm(
                            fullName = a.patientName,
                            primaryPhone = Phone.EMPTY,
                            reason = a.reason,
                            currentSymptoms = "Descrição fictícia gerada para demonstração.",
                            intensity = 1 + random.nextInt(10),
                        ),
                    )
                }
            }
        }
    }

    private fun name(): String = "${FIRST_NAMES[random.nextInt(FIRST_NAMES.size)]} ${LAST_NAMES[random.nextInt(LAST_NAMES.size)]} ${LAST_NAMES[random.nextInt(LAST_NAMES.size)]}"

    private companion object {
        const val SEED = 20261201
        const val DERBY = "Policlínica Derby"
        const val PATIENTS = 420
        const val SIGN_UP_DAYS = 180
        const val HISTORY_DAYS = 90
        const val BATCH = 2000
        val SLOW_RECEPTION = (21 * 24).hours

        val FIRST_NAMES = listOf(
            "Ana", "Bruno", "Carla", "Diego", "Eduarda", "Felipe", "Gabriela", "Heitor", "Isabela", "João",
            "Larissa", "Marcelo", "Natália", "Otávio", "Paula", "Rafael", "Sofia", "Tiago", "Vitória", "Yuri",
            "Beatriz", "Caio", "Débora", "Enzo", "Fernanda", "Gustavo", "Helena", "Igor", "Júlia", "Lucas",
        )
        val LAST_NAMES = listOf(
            "Silva", "Souza", "Oliveira", "Lima", "Pereira", "Costa", "Ferreira", "Almeida", "Nascimento", "Araújo",
            "Barbosa", "Cavalcanti", "Monteiro", "Rocha", "Teixeira", "Moura", "Freitas", "Correia", "Melo", "Ribeiro",
        )
        val REASONS = listOf(
            "Consulta de rotina", "Dor nas costas", "Retorno", "Exames de rotina", "Dor no joelho",
            "Acompanhamento", "Febre há dois dias", "Renovação de receita", "Manchas na pele", "Check-up anual",
        )
    }
}
