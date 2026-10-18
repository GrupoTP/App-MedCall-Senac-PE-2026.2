package grupotp.recifesaudavel.server.service

import grupotp.recifesaudavel.common.pretriage.PendingPreTriage
import grupotp.recifesaudavel.common.pretriage.PreTriage
import grupotp.recifesaudavel.common.pretriage.PreTriageForm
import grupotp.recifesaudavel.common.scheduling.AppointmentStatus
import grupotp.recifesaudavel.server.repository.PreTriageRepository
import grupotp.recifesaudavel.server.repository.SchedulingRepository
import grupotp.recifesaudavel.server.repository.UserRepository
import kotlinx.datetime.toKotlinLocalDate
import java.time.Clock
import java.time.LocalDate
import kotlin.time.toKotlinInstant

sealed interface PreTriageResult {
    data class Created(val preTriage: PreTriage) : PreTriageResult

    data class Rejected(val message: String) : PreTriageResult
}

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

    private fun validate(form: PreTriageForm): String? {
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
            form.fullName.isBlank() -> "Nome completo é obrigatório."
            birthDate == null -> "Informe sua data de nascimento."
            birthDate > LocalDate.now(clock).toKotlinLocalDate() || birthDate.year < MIN_BIRTH_YEAR ->
                "Informe uma data de nascimento válida."
            form.primaryPhone.isEmpty() -> "Telefone principal é obrigatório."
            form.reason.isBlank() -> "Descreva o motivo da consulta."
            form.intensity != null && form.intensity !in 1..10 -> "Intensidade deve ser de 1 a 10."
            texts.any { it.length > MAX_TEXT } -> "Texto muito longo. Use até $MAX_TEXT caracteres."
            shortTexts.any { it.length > MAX_SHORT_TEXT } -> "Texto muito longo. Use até $MAX_SHORT_TEXT caracteres."
            phones.any { !it.hasValidLength } -> "Telefone inválido."
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
    }
}
