package grupotp.recifesaudavel.server.database

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
            url = settings.jdbcUrl,
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

    private companion object {
        const val FILE_URL_PREFIX = "jdbc:h2:file:"
    }
}
