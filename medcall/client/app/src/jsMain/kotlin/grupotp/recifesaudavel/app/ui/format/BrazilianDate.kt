package grupotp.recifesaudavel.app.ui.format

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

/**
 * Dates typed and shown as `dd/MM/aaaa`.
 */
internal object BrazilianDate {
    /** `31/05/1980` → the date, or null when the text is not a whole, real date (such as `31/02/1980`). */
    fun parse(text: String): LocalDate? {
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
    fun format(date: LocalDate?): String {
        if (date == null) {
            return ""
        }
        val day = date.day.toString().padStart(2, '0')
        val month = date.month.number.toString().padStart(2, '0')
        return "$day/$month/${date.year}"
    }

    /** Keeps the digits typed and puts the slashes of `dd/MM/aaaa` back in. */
    fun mask(text: String): String {
        val digits = text.filter(Char::isDigit).take(8)
        return buildString {
            digits.forEachIndexed { index, digit ->
                if (index == 2 || index == 4) append('/')
                append(digit)
            }
        }
    }
}
