package grupotp.recifesaudavel.app.usecase

import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.pretriage.PreTriage
import grupotp.recifesaudavel.common.pretriage.PreTriageForm
import grupotp.recifesaudavel.common.security.SecretString
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

/**
 * Pre-triage API of the server. Every call needs the session token.
 */
interface PreTriageGateway {
    suspend fun draft(token: SecretString): SchedulingResult<PreTriageForm>

    suspend fun submit(token: SecretString, form: PreTriageForm): SchedulingResult<PreTriage>

    suspend fun pending(token: SecretString): SchedulingResult<PreTriage?>
}

/**
 * Form to start from: personal data from the current profile, medical history and medications
 * from the last pre-triage. Loading it also tells whether the server can be reached.
 */
class LoadPreTriageDraft(
    private val gateway: PreTriageGateway,
) {
    suspend operator fun invoke(account: AuthenticatedAccount): SchedulingResult<PreTriageForm> =
        gateway.draft(account.sessionToken)
}

/**
 * Sends the pre-triage. The profile is never changed by it, even when the personal data differ.
 */
class SubmitPreTriage(
    private val gateway: PreTriageGateway,
) {
    suspend operator fun invoke(account: AuthenticatedAccount, form: PreTriageForm): SchedulingResult<PreTriage> =
        gateway.submit(account.sessionToken, form)
}

/**
 * Latest pre-triage the next booking may attach, or null. A failure counts as none, so the
 * booking never depends on it.
 */
class LoadPendingPreTriage(
    private val gateway: PreTriageGateway,
) {
    suspend operator fun invoke(account: AuthenticatedAccount): PreTriage? =
        (gateway.pending(account.sessionToken) as? SchedulingResult.Success)?.value
}

const val PRE_TRIAGE_OFFLINE_MESSAGE =
    "A pré-triagem precisa de internet. Conecte-se e tente de novo. Nada foi enviado."

fun SchedulingError.toPreTriageMessage(): String = when (this) {
    SchedulingError.Offline -> PRE_TRIAGE_OFFLINE_MESSAGE
    SchedulingError.SessionExpired -> SESSION_EXPIRED_MESSAGE
    is SchedulingError.Rejected -> message
}

/** `31/05/1980` → the date, or null when the text is not a whole, real date (such as `31/02/1980`). */
fun parseBrazilianDate(text: String): LocalDate? {
    val parts = text.split("/")
    if (parts.size != 3 || parts[0].length != 2 || parts[1].length != 2 || parts[2].length != 4) {
        return null
    }
    val (day, month, year) = parts.map { it.toIntOrNull() ?: return null }
    return try {
        LocalDate(year, month, day)
    } catch (_: IllegalArgumentException) {
        null
    }
}

/** The date as `31/05/1980`, or empty for null. */
fun formatBrazilianDate(date: LocalDate?): String {
    if (date == null) {
        return ""
    }
    val day = date.day.toString().padStart(2, '0')
    val month = date.month.number.toString().padStart(2, '0')
    return "$day/$month/${date.year}"
}

/** Keeps the digits typed and puts the slashes of `dd/MM/aaaa` back in. */
fun maskBrazilianDate(text: String): String {
    val digits = text.filter(Char::isDigit).take(8)
    return buildString {
        digits.forEachIndexed { index, digit ->
            if (index == 2 || index == 4) append('/')
            append(digit)
        }
    }
}
