package grupotp.recifesaudavel.common.security

import grupotp.recifesaudavel.common.auth.SignInRequest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SecretStringTest {
    @Test
    fun printingNeverShowsTheSecretButJsonCarriesIt() {
        // Arrange
        val request = SignInRequest(email = "maria@email.com", password = SecretString("segredo"))

        // Act
        val printed = request.toString()
        val json = Json.encodeToString(request)
        val decoded = Json.decodeFromString<SignInRequest>(json)

        // Assert
        assertFalse("segredo" in printed)
        assertEquals("SignInRequest(email=maria@email.com, password=<secret>)", printed)
        assertEquals("""{"email":"maria@email.com","password":"segredo"}""", json)
        assertEquals("segredo", decoded.password.value)
    }
}
