package grupotp.medcall.server.database

import org.jetbrains.exposed.v1.jdbc.Database
import java.io.File

/**
 * Opens the Exposed database described by [DatabaseSettings].
 * File URLs get their parent folder created before the first connection.
 */
class DatabaseConnector {
    fun connect(settings: DatabaseSettings): Database {
        prepareFileDatabase(settings.jdbcUrl)
        return Database.connect(
            url = keepFileDatabaseOpen(settings.jdbcUrl),
            driver = settings.driver,
            user = settings.user,
            password = settings.password,
        )
    }

    private fun prepareFileDatabase(jdbcUrl: String) {
        if (!jdbcUrl.startsWith(FILE_URL_PREFIX)) {
            return
        }
        val path = jdbcUrl.removePrefix(FILE_URL_PREFIX).substringBefore(';')
        File(path).absoluteFile.parentFile?.mkdirs()
    }

    /**
     * Each transaction opens its own connection, and H2 closes a file database when its last
     * connection closes: every request would reopen the file with an empty cache (the dashboards
     * took seconds). DB_CLOSE_DELAY=-1 keeps it open while the server runs.
     */
    internal fun keepFileDatabaseOpen(jdbcUrl: String): String =
        if (jdbcUrl.startsWith(FILE_URL_PREFIX) && !jdbcUrl.contains("DB_CLOSE_DELAY", ignoreCase = true)) {
            "$jdbcUrl;DB_CLOSE_DELAY=-1"
        } else {
            jdbcUrl
        }

    private companion object {
        const val FILE_URL_PREFIX = "jdbc:h2:file:"
    }
}
