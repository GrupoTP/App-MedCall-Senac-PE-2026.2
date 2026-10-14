package grupotp.recifesaudavel.app.usecase

import grupotp.recifesaudavel.common.AuthenticatedAccount
import grupotp.recifesaudavel.common.PreTriage
import grupotp.recifesaudavel.common.PreTriageForm

/**
 * Pre-triage API of the server. Every call needs the session token.
 */
interface PreTriageGateway {
    suspend fun draft(token: String): SchedulingResult<PreTriageForm>

    suspend fun submit(token: String, form: PreTriageForm): SchedulingResult<PreTriage>

    suspend fun pending(token: String): SchedulingResult<PreTriage?>
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

/** `31/05/1980` → `1980-05-31`, or empty when the text is not a whole date. */
fun brazilianDateToIso(text: String): String {
    val parts = text.split("/")
    if (parts.size != 3 || parts[0].length != 2 || parts[1].length != 2 || parts[2].length != 4) {
        return ""
    }
    return "${parts[2]}-${parts[1]}-${parts[0]}"
}

/** `1980-05-31` → `31/05/1980`; anything else becomes empty. */
fun isoDateToBrazilian(iso: String): String {
    val parts = iso.split("-")
    return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else ""
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
