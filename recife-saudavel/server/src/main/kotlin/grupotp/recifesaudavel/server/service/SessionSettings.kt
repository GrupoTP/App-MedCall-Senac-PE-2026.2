package grupotp.recifesaudavel.server.service

import io.ktor.server.config.ApplicationConfig
import java.time.Duration

/**
 * How long a session survives without being used. Each use starts the count again, so a person
 * who opens the app now and then never has to sign in again.
 *
 * @property idleTimeout null when sessions never expire.
 */
data class SessionSettings(
    val idleTimeout: Duration?,
) {
    companion object {
        /** Reads `sessions.idleDays` (`SESSION_IDLE_DAYS`); 0 turns expiry off. */
        fun from(config: ApplicationConfig): SessionSettings {
            val days = config.property("sessions.idleDays").getString().trim().toLong()
            require(days >= 0) { "sessions.idleDays must be 0 or more, got $days." }
            return SessionSettings(idleTimeout = if (days == 0L) null else Duration.ofDays(days))
        }
    }
}
