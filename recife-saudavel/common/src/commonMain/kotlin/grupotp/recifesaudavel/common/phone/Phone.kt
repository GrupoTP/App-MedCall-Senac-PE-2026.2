package grupotp.recifesaudavel.common.phone

/**
 * Phone helpers shared by client and server. Phones are stored as digits only (DDD included) and shown with a mask.
 */
object Phone {
    /** Keeps only the digits. A leading 0 is kept as typed: nothing is added or removed. */
    fun digits(raw: String): String = raw.filter { it in '0'..'9' }

    /** DDD + 9 digits. A number typed with a leading 0 (trunk prefix) may carry one extra digit. */
    private const val MAX_DIGITS = 11

    /** Largest number of digits [raw] may hold, given its first digit. */
    fun maxDigits(raw: String): Int = if (digits(raw).startsWith("0")) MAX_DIGITS + 1 else MAX_DIGITS

    /** Digits of [raw] cut to [maxDigits]. */
    fun limited(raw: String): String = digits(raw).let { it.take(maxDigits(it)) }

    /**
     * Masks digits as `(XX) XXXX-XXXX` (10 digits) or `(XX) XXXXX-XXXX` (11 digits). Partial input is masked
     * progressively, so it can be used while typing. Digits beyond 11 are appended untouched.
     */
    fun format(raw: String): String {
        val d = digits(raw)
        return when {
            d.isEmpty() -> ""
            d.length <= 2 -> "($d"
            d.length <= 6 -> "(${d.take(2)}) ${d.drop(2)}"
            d.length <= 10 -> "(${d.take(2)}) ${d.substring(2, 6)}-${d.drop(6)}"
            else -> "(${d.take(2)}) ${d.substring(2, 7)}-${d.substring(7)}"
        }
    }
}
