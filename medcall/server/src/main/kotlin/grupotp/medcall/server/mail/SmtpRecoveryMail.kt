package grupotp.medcall.server.mail

import jakarta.mail.Authenticator
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.Properties

/**
 * Sends the recovery code with SMTP. Host, port, user, password and from-address
 * come from the environment. None of them is written in the repository.
 */
class SmtpRecoveryMail(
    private val host: String,
    private val port: Int,
    private val username: String,
    private val password: String,
    private val from: String,
) : RecoveryMail {
    override val configured: Boolean = host.isNotBlank() && from.isNotBlank()

    override fun send(to: String, code: String) {
        val properties = Properties()
        properties["mail.smtp.host"] = host
        properties["mail.smtp.port"] = port.toString()
        properties["mail.smtp.auth"] = username.isNotBlank().toString()
        properties["mail.smtp.starttls.enable"] = "true"
        val session = Session.getInstance(
            properties,
            object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication =
                    PasswordAuthentication(username, password)
            },
        )
        val message = MimeMessage(session)
        message.setFrom(InternetAddress(from))
        message.setRecipients(MimeMessage.RecipientType.TO, InternetAddress.parse(to))
        message.subject = "MedCall, código para trocar a senha"
        message.setText(
            """
            Use este código para escolher uma nova senha no MedCall.
            Ele vale por 30 minutos e só funciona uma vez.

            $code

            Se você não pediu isso, ignore esta mensagem.
            """.trimIndent(),
            "UTF-8",
        )
        Transport.send(message)
    }
}
