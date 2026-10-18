package grupotp.recifesaudavel.common.pretriage

import grupotp.recifesaudavel.common.phone.Phone
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Answers of the four-step pre-triage from the prototype. The texts are what the patient
 * types; the app never interprets them (no diagnosis).
 *
 * The personal data here is a copy for this pre-triage only: editing it never changes the profile.
 *
 * @property birthDate null until the patient fills it in.
 * @property symptomsStart null until the patient picks an answer.
 * @property intensity `1` to `10`, or null when the patient prefers not to say.
 */
@Serializable
data class PreTriageForm(
    val fullName: String = "",
    val socialName: String = "",
    val birthDate: LocalDate? = null,
    val primaryPhone: Phone = Phone.EMPTY,
    val primaryPhoneWhatsapp: Boolean = false,
    val secondaryPhone: Phone = Phone.EMPTY,
    val secondaryPhoneWhatsapp: Boolean = false,
    val emergencyContactName: String = "",
    val emergencyContactPhone: Phone = Phone.EMPTY,
    val reason: String = "",
    val currentSymptoms: String = "",
    val symptomsStart: SymptomsStart? = null,
    val symptomsPattern: String = "",
    val intensity: Int? = null,
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
