package grupotp.medcall.server.mail

/**
 * Keeps the last code in memory. Used by the test task (`MAIL_MODE=capture`),
 * so the suite can finish a recovery without an SMTP server.
 */
class CapturingRecoveryMail : RecoveryMail {
    @Volatile
    var lastCode: String? = null

    override val configured: Boolean = true

    override fun send(to: String, code: String) {
        lastCode = code
    }

    companion object {
        @Volatile
        var active: CapturingRecoveryMail? = null
    }
}
