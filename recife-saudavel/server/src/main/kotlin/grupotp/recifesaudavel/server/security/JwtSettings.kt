package grupotp.recifesaudavel.server.security

import grupotp.recifesaudavel.common.security.SecretString
import io.ktor.server.config.ApplicationConfig

/**
 * How session tokens are signed: HS256 with [secret], naming [issuer] and [audience].
 *
 * @property realm sent in `WWW-Authenticate` when a token is refused.
 */
data class JwtSettings(
    val secret: SecretString,
    val issuer: String,
    val audience: String,
    val realm: String,
) {
    init {
        require(secret.value.toByteArray().size >= MIN_SECRET_BYTES) {
            "JWT_SECRET needs at least $MIN_SECRET_BYTES bytes, as HS256 signs with a 256-bit key."
        }
    }

    companion object {
        /** HS256 keys shorter than its 256-bit hash make the signature easier to guess. */
        const val MIN_SECRET_BYTES = 32

        /**
         * Reads the `jwt` group. The server does not start without `JWT_SECRET`; the Gradle tasks
         * that run it locally (`run`, `runDev`, `test`) supply a development one.
         */
        fun from(config: ApplicationConfig): JwtSettings {
            val secret = config.property("jwt.secret").getString()
            check(secret.isNotBlank()) { "Set JWT_SECRET to a random secret of at least $MIN_SECRET_BYTES bytes." }
            return JwtSettings(
                secret = SecretString(secret),
                issuer = config.property("jwt.issuer").getString(),
                audience = config.property("jwt.audience").getString(),
                realm = config.property("jwt.realm").getString(),
            )
        }
    }
}
