package grupotp.recifesaudavel.app.ui.format

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BrazilianDateTest {
    @Test
    fun parseAcceptsOnlyWholeRealDates() {
        // Act
        val valid = BrazilianDate.parse("31/05/1980")
        val shortYear = BrazilianDate.parse("31/05/80")
        val impossible = BrazilianDate.parse("31/02/1980")

        // Assert
        assertEquals(LocalDate(1980, 5, 31), valid)
        assertNull(shortYear)
        assertNull(impossible)
    }

    @Test
    fun formatPadsDayAndMonth() {
        // Act
        val formatted = BrazilianDate.format(LocalDate(1980, 5, 31))
        val empty = BrazilianDate.format(null)

        // Assert
        assertEquals("31/05/1980", formatted)
        assertEquals("", empty)
    }

    @Test
    fun maskPutsTheSlashesBackAndDropsExtraDigits() {
        // Act
        val digits = BrazilianDate.mask("31051980")
        val partial = BrazilianDate.mask("31/05")
        val tooLong = BrazilianDate.mask("31/05/19801")

        // Assert
        assertEquals("31/05/1980", digits)
        assertEquals("31/05", partial)
        assertEquals("31/05/1980", tooLong)
    }
}
