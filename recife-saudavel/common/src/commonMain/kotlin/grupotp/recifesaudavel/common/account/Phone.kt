package grupotp.recifesaudavel.common.account

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * A phone number as digits only, DDD included, and empty when not given. It is shown with a mask.
 * A leading 0 (trunk prefix) is kept as typed: nothing is added or removed.
 */
@Serializable
@JvmInline
value class Phone(val digits: String) {
    init {
        require(digits.all { it in '0'..'9' }) { "A phone holds digits only." }
    }

    fun isEmpty(): Boolean = digits.isEmpty()

    /** False when it has more digits than [maxDigits] allows. */
    val hasValidLength: Boolean get() = digits.length <= maxDigits(digits)

    /** `(XX) XXXX-XXXX` or `(XX) XXXXX-XXXX`; see [format]. */
    fun formatted(): String = format(digits)

    override fun toString(): String = formatted()

    companion object {
        val EMPTY = Phone("")

        /** DDD + 9 digits. A number typed with a leading 0 (trunk prefix) may carry one extra digit. */
        private const val MAX_DIGITS = 11

        /** Keeps only the digits of [raw], such as text typed with a mask. */
        fun parse(raw: String): Phone = Phone(digitsOf(raw))

        fun digitsOf(raw: String): String = raw.filter { it in '0'..'9' }

        /** Largest number of digits [raw] may hold, given its first digit. */
        fun maxDigits(raw: String): Int = if (digitsOf(raw).startsWith("0")) MAX_DIGITS + 1 else MAX_DIGITS

        /** Digits of [raw] cut to [maxDigits]. */
        fun limited(raw: String): String = digitsOf(raw).let { it.take(maxDigits(it)) }

        /**
         * Masks digits as `(XX) XXXX-XXXX` (10 digits) or `(XX) XXXXX-XXXX` (11 digits). Partial input is masked
         * progressively, so it can be used while typing. Digits beyond 11 are appended untouched.
         */
        fun format(raw: String): String {
            val d = digitsOf(raw)
            return when {
                d.isEmpty() -> ""
                d.length <= 2 -> "($d"
                d.length <= 6 -> "(${d.take(2)}) ${d.drop(2)}"
                d.length <= 10 -> "(${d.take(2)}) ${d.substring(2, 6)}-${d.drop(6)}"
                else -> "(${d.take(2)}) ${d.substring(2, 7)}-${d.substring(7)}"
            }
        }
    }
}
