package grupotp.medcall.server.mail

/** Sends the one-time recovery code. The code is not logged. */
interface RecoveryMail {
    /** False when this server has no way to deliver the code. */
    val configured: Boolean

    fun send(to: String, code: String)
}
