package grupotp.recifesaudavel.server.service

import grupotp.recifesaudavel.common.api.ErrorCode
import grupotp.recifesaudavel.common.pretriage.PendingPreTriage
import grupotp.recifesaudavel.common.pretriage.PreTriageForm
import grupotp.recifesaudavel.common.scheduling.AppointmentStatus
import grupotp.recifesaudavel.server.repository.PreTriageRepository
import grupotp.recifesaudavel.server.repository.SchedulingRepository
import grupotp.recifesaudavel.server.repository.UserRepository
import grupotp.recifesaudavel.server.service.result.PreTriageResult
import grupotp.recifesaudavel.server.domain.ServiceError
import kotlinx.datetime.toKotlinLocalDate
import java.time.Clock
import java.time.LocalDate
import kotlin.time.toKotlinInstant

/**
 * Pre-triage rules. The personal data typed here belongs to the pre-triage only: the profile
 * is never written from it.
 */
class PreTriageService(
    private val preTriages: PreTriageRepository,
    private val users: UserRepository,
    private val appointments: SchedulingRepository,
    private val clock: Clock,
) {
    /**
     * Form to start from. Personal data comes from the current profile; the birth date, the
     * medical history and the medications come from the last pre-triage, since they rarely
     * change. The complaint always starts empty. Null when the user does not exist.
     */
    fun draft(patientId: Int): PreTriageForm? {
        val profile = users.findProfile(patientId) ?: return null
        val last = preTriages.latestOf(patientId)?.form ?: PreTriageForm()
        return PreTriageForm(
            fullName = profile.fullName,
            socialName = profile.socialName,
            birthDate = last.birthDate,
            primaryPhone = profile.primaryPhone,
            primaryPhoneWhatsapp = profile.primaryPhoneWhatsapp,
            secondaryPhone = profile.secondaryPhone,
            secondaryPhoneWhatsapp = profile.secondaryPhoneWhatsapp,
            emergencyContactName = profile.emergencyContactName,
            emergencyContactPhone = profile.emergencyContactPhone,
            previousConditions = last.previousConditions,
            surgeries = last.surgeries,
            allergies = last.allergies,
            currentTreatment = last.currentTreatment,
            historyNotes = last.historyNotes,
            regularMedications = last.regularMedications,
            continuousMedications = last.continuousMedications,
            supplements = last.supplements,
            otherMedications = last.otherMedications,
            medicationNotes = last.medicationNotes,
        )
    }

    fun submit(patientId: Int, form: PreTriageForm): PreTriageResult {
        val clean = form.trimmed()
        validate(clean)?.let { return PreTriageResult.Rejected(it) }
        val created = preTriages.insert(patientId = patientId, form = clean, createdAt = clock.instant().toKotlinInstant())
        return PreTriageResult.Created(created)
    }

    /**
     * The latest pre-triage, if the next booking may attach it: it is not attached yet, or the
     * appointment it was attached to has been cancelled.
     */
    fun pending(patientId: Int): PendingPreTriage {
        val latest = preTriages.latestOf(patientId) ?: return PendingPreTriage()
        val appointmentId = latest.appointmentId ?: return PendingPreTriage(latest)
        val status = appointments.findAppointment(appointmentId)?.status
        return if (status == null || status == AppointmentStatus.Cancelled) {
            PendingPreTriage(latest.copy(appointmentId = null))
        } else {
            PendingPreTriage()
        }
    }

    private fun validate(form: PreTriageForm): ServiceError? {
        val birthDate = form.birthDate
        val texts = listOf(
            form.reason, form.currentSymptoms, form.symptomsPattern, form.complaintNotes,
            form.previousConditions, form.surgeries, form.allergies, form.currentTreatment, form.historyNotes,
            form.regularMedications, form.continuousMedications, form.supplements, form.otherMedications,
            form.medicationNotes,
        )
        val shortTexts = listOf(form.fullName, form.socialName, form.emergencyContactName)
        val phones = listOf(form.primaryPhone, form.secondaryPhone, form.emergencyContactPhone)
        return when {
            form.fullName.isBlank() -> ServiceError(ErrorCode.FULL_NAME_REQUIRED)
            birthDate == null -> ServiceError(ErrorCode.BIRTH_DATE_REQUIRED)
            birthDate > LocalDate.now(clock).toKotlinLocalDate() || birthDate.year < MIN_BIRTH_YEAR ->
                ServiceError(ErrorCode.BIRTH_DATE_INVALID)
            form.primaryPhone.isEmpty() -> ServiceError(ErrorCode.PRIMARY_PHONE_REQUIRED)
            form.reason.isBlank() -> ServiceError(ErrorCode.REASON_REQUIRED)
            form.intensity != null && form.intensity !in INTENSITY ->
                ServiceError(ErrorCode.INTENSITY_OUT_OF_RANGE, INTENSITY.first, INTENSITY.last)
            texts.any { it.length > MAX_TEXT } -> ServiceError(ErrorCode.TEXT_TOO_LONG, MAX_TEXT)
            shortTexts.any { it.length > MAX_SHORT_TEXT } -> ServiceError(
                ErrorCode.TEXT_TOO_LONG,
                MAX_SHORT_TEXT
            )
            phones.any { !it.hasValidLength } -> ServiceError(ErrorCode.PHONE_INVALID)
            else -> null
        }
    }

    private fun PreTriageForm.trimmed(): PreTriageForm = copy(
        fullName = fullName.trim(),
        socialName = socialName.trim(),
        emergencyContactName = emergencyContactName.trim(),
        reason = reason.trim(),
        currentSymptoms = currentSymptoms.trim(),
        symptomsPattern = symptomsPattern.trim(),
        complaintNotes = complaintNotes.trim(),
        previousConditions = previousConditions.trim(),
        surgeries = surgeries.trim(),
        allergies = allergies.trim(),
        currentTreatment = currentTreatment.trim(),
        historyNotes = historyNotes.trim(),
        regularMedications = regularMedications.trim(),
        continuousMedications = continuousMedications.trim(),
        supplements = supplements.trim(),
        otherMedications = otherMedications.trim(),
        medicationNotes = medicationNotes.trim(),
    )

    private companion object {
        const val MAX_TEXT = 1000
        const val MAX_SHORT_TEXT = 200
        const val MIN_BIRTH_YEAR = 1900
        val INTENSITY = 1..10
    }
}
