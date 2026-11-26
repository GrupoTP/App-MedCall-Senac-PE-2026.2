package grupotp.medcall.server.mail

import io.ktor.server.config.ApplicationConfig

object RecoveryMailFactory {
    fun from(config: ApplicationConfig): RecoveryMail {
        val mode = config.property("mail.mode").getString().trim()
        if (mode == "capture") {
            return CapturingRecoveryMail().also { CapturingRecoveryMail.active = it }
        }
        val port = config.property("mail.port").getString().trim().toInt()
        return SmtpRecoveryMail(
            host = config.property("mail.host").getString().trim(),
            port = port,
            username = config.property("mail.username").getString(),
            password = config.property("mail.password").getString(),
            from = config.property("mail.from").getString().trim(),
        )
    }
}
