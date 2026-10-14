package grupotp.recifesaudavel.common

import kotlinx.serialization.Serializable

/**
 * Answers of the four-step pre-triage from the prototype. Every field is free text the
 * patient types; the app never interprets it (no diagnosis).
 *
 * The personal data here is a copy for this pre-triage only: editing it never changes the profile.
 *
 * @property birthDate ISO date, such as `1980-05-31`. Empty until the patient fills it in.
 * @property intensity `1` to `10`, or empty when the patient prefers not to say.
 */
@Serializable
data class PreTriageForm(
    val fullName: String = "",
    val socialName: String = "",
    val birthDate: String = "",
    val primaryPhone: String = "",
    val primaryPhoneWhatsapp: Boolean = false,
    val secondaryPhone: String = "",
    val secondaryPhoneWhatsapp: Boolean = false,
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val reason: String = "",
    val currentSymptoms: String = "",
    val symptomsStart: String = "",
    val symptomsPattern: String = "",
    val intensity: String = "",
    val complaintNotes: String = "",
    val previousConditions: String = "",
    val surgeries: String = "",
    val allergies: String = "",
    val currentTreatment: String = "",
    val historyNotes: String = "",
    val regularMedications: String = "",
    val continuousMedications: String = "",
    val supplements: String = "",
    val otherMedications: String = "",
    val medicationNotes: String = "",
)

/**
 * A pre-triage the server stored.
 *
 * @property createdAt epoch milliseconds.
 * @property appointmentId appointment it was attached to when booking, if any.
 */
@Serializable
data class PreTriage(
    val id: String,
    val createdAt: Long,
    val form: PreTriageForm,
    val appointmentId: String? = null,
)

/**
 * The latest pre-triage that the next booking may attach, or null when there is none:
 * the patient never sent one, or the latest is already attached to an active appointment.
 */
@Serializable
data class PendingPreTriage(
    val preTriage: PreTriage? = null,
)

/** Options of "Quando começaram os sintomas?", same as the prototype. */
val SYMPTOMS_START_OPTIONS = listOf(
    "Hoje",
    "Há 2 a 3 dias",
    "Há uma semana",
    "Há 2 semanas",
    "Há 1 mês",
    "Há mais de 1 mês",
    "Há mais de 6 meses",
    "Crônico (mais de 1 ano)",
)
