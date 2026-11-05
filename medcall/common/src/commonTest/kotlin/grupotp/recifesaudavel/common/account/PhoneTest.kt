package grupotp.recifesaudavel.common.account

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhoneTest {
    @Test
    fun parsingKeepsDigitsAndJsonIsThePlainString() {
        // Act
        val phone = Phone.parse("(81) 99999-0000")

        // Assert
        assertEquals("81999990000", phone.digits)
        assertEquals("(81) 99999-0000", phone.formatted())
        assertEquals("\"81999990000\"", Json.encodeToString(phone))
        assertEquals(phone, Json.decodeFromString<Phone>("\"81999990000\""))
    }

    @Test
    fun onlyDigitsAreAccepted() {
        // Act & Assert
        assertFailsWith<IllegalArgumentException> { Phone("(81) 9999") }
        assertFailsWith<IllegalArgumentException> { Json.decodeFromString<Phone>("\"81-9999\"") }
    }

    @Test
    fun lengthDependsOnTheTrunkPrefix() {
        // Assert
        assertTrue(Phone("81999990000").hasValidLength)
        assertFalse(Phone("819999900001").hasValidLength)
        assertTrue(Phone("081999990000").hasValidLength)
    }
}
