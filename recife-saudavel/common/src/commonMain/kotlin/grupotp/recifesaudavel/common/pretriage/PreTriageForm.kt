package grupotp.recifesaudavel.common.pretriage

import kotlinx.serialization.Serializable

/**
 * Answers of the four-step pre-triage from the prototype. Every field is free text the
 * patient types; the app never interprets it (no diagnosis).
 *
 * The personal data here is a copy for this pre-triage only: editing it never changes the profile.
 *
 * @property birthDate ISO date, such as `1980-05-31`. Empty until the patient fills it in.
 * @property symptomsStart null until the patient picks an answer.
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
    val symptomsStart: SymptomsStart? = null,
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
