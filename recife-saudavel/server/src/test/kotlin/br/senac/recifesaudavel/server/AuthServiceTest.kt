package br.senac.recifesaudavel.server

import br.senac.recifesaudavel.common.LoginRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthServiceTest {
    @Test
    fun loginWithMockUserReturnsSession() {
        // Arrange
        val authService = AuthService()
        val request = LoginRequest(email = MockUser.EMAIL, password = MockUser.PASSWORD)

        // Act
        val session = authService.login(request)

        // Assert
        assertEquals(MockUser.NAME, session?.user?.name)
        assertEquals(MockUser.EMAIL, session?.user?.email)
        assertTrue(session!!.token.isNotBlank())
    }

    @Test
    fun loginWithWrongPasswordReturnsNull() {
        // Arrange
        val authService = AuthService()
        val request = LoginRequest(email = MockUser.EMAIL, password = "errada")

        // Act
        val session = authService.login(request)

        // Assert
        assertNull(session)
    }

    @Test
    fun logoutRemovesTheSession() {
        // Arrange
        val authService = AuthService()
        val session = authService.login(LoginRequest(email = MockUser.EMAIL, password = MockUser.PASSWORD))
        val token = session!!.token

        // Act
        val removed = authService.logout(token)
        val current = authService.current(token)

        // Assert
        assertTrue(removed)
        assertNull(current)
        assertFalse(authService.logout(token))
    }
}
