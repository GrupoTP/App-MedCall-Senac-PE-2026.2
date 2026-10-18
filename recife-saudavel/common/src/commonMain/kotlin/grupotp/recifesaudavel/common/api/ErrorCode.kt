package grupotp.recifesaudavel.common.api

import kotlinx.serialization.Serializable

/**
 * Why the server refused a request. The client may branch on it; the text shown to the person
 * comes with it in [ApiError.message].
 */
@Serializable
enum class ErrorCode {
    SESSION_EXPIRED,
    WRONG_CREDENTIALS,
    EMAIL_TAKEN,
    FULL_NAME_REQUIRED,
    EMAIL_INVALID,
    PASSWORD_TOO_SHORT,
    CURRENT_PASSWORD_WRONG,
    PASSWORD_UNCHANGED,
    PRIMARY_PHONE_REQUIRED,
    PHONE_INVALID,
    PRIVACY_POLICY_REQUIRED,
    TREATMENT_CONSENT_REQUIRED,
    BIRTH_DATE_REQUIRED,
    BIRTH_DATE_INVALID,
    REASON_REQUIRED,
    INTENSITY_OUT_OF_RANGE,
    TEXT_TOO_LONG,
    INVALID_CHOICE,
    CLINIC_REQUIRED,
    DATE_UNAVAILABLE,
    TIME_UNAVAILABLE,
    SLOT_TAKEN,
    PRE_TRIAGE_NOT_FOUND,
    PRE_TRIAGE_ALREADY_ATTACHED,
    APPOINTMENT_NOT_FOUND,
    APPOINTMENT_NOT_CANCELLABLE,
}
