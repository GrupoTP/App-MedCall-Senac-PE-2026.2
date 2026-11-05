package grupotp.recifesaudavel.server.database

import io.ktor.server.config.ApplicationConfig

/**
 * JDBC settings read from application configuration.
 * Each value can be replaced with an environment variable.
 */
data class DatabaseSettings(
    val jdbcUrl: String,
    val driver: String,
    val user: String,
    val password: String,
) {
    companion object {
        fun from(config: ApplicationConfig): DatabaseSettings = DatabaseSettings(
            jdbcUrl = config.property("database.jdbcUrl").getString(),
            driver = config.property("database.driver").getString(),
            user = config.property("database.user").getString(),
            password = config.property("database.password").getString(),
        )
    }
}
