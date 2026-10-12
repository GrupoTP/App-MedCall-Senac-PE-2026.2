package br.senac.recifesaudavel.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LoginRequestValidationTest {
    @Test
    fun blankEmailReturnsMessage() {
        // Arrange
        val request = LoginRequest(email = "  ", password = "saudavel")

        // Act
        val message = request.validationError()

        // Assert
        assertEquals("Informe o e-mail.", message)
    }

    @Test
    fun blankPasswordReturnsMessage() {
        // Arrange
        val request = LoginRequest(email = "maria@recife.saude", password = "")

        // Act
        val message = request.validationError()

        // Assert
        assertEquals("Informe a senha.", message)
    }

    @Test
    fun filledCredentialsHaveNoValidationError() {
        // Arrange
        val request = LoginRequest(email = "maria@recife.saude", password = "saudavel")

        // Act
        val message = request.validationError()

        // Assert
        assertNull(message)
    }
}
