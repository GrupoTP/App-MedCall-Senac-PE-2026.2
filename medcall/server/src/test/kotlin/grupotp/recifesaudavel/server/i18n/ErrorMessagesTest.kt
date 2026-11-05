package grupotp.recifesaudavel.server.i18n

import grupotp.recifesaudavel.common.api.ErrorCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ErrorMessagesTest {
    @Test
    fun everyCodeHasAText() {
        // Act
        val texts = ErrorCode.entries.associateWith { ErrorMessages.of(it, listOf(1, 10)) }

        // Assert
        texts.forEach { (code, text) -> assertTrue(text.isNotBlank() && !text.contains('{'), "$code: $text") }
    }

    @Test
    fun argumentsAreFormattedForBrazil() {
        // Act
        val tooLong = ErrorMessages.of(ErrorCode.TEXT_TOO_LONG, listOf(1000))
        val expired = ErrorMessages.of(ErrorCode.SESSION_EXPIRED)

        // Assert
        assertEquals("Texto muito longo. Use até 1.000 caracteres.", tooLong)
        assertEquals("Sua sessão expirou. Entre de novo para continuar.", expired)
    }
}
