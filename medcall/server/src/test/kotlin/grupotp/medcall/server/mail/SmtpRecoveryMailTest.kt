package grupotp.medcall.server.mail

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SmtpRecoveryMailTest {
    @Test
    fun blankHostOrFromAddressIsNotConfigured() {
        assertFalse(SmtpRecoveryMail(host = "", port = 587, username = "", password = "", from = "medcall@example.com").configured)
        assertFalse(SmtpRecoveryMail(host = "smtp.example.com", port = 587, username = "", password = "", from = "").configured)
        assertTrue(SmtpRecoveryMail(host = "smtp.example.com", port = 587, username = "", password = "", from = "medcall@example.com").configured)
    }
}
