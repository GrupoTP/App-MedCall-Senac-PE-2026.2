package grupotp.recifesaudavel.server.plugin

import grupotp.recifesaudavel.server.database.DatabaseConnector
import grupotp.recifesaudavel.server.database.DatabaseSettings
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.koin.dsl.module
import org.koin.ktor.ext.getKoin

fun Application.configureDatabase() {
    val settings = DatabaseSettings.from(environment.config)
    getKoin().loadModules(
        listOf(
            module {
                single { settings }
                single { DatabaseConnector().connect(get()) }
            },
        ),
    )
    val database = getKoin().get<Database>()
    monitor.subscribe(ApplicationStopped) {
        TransactionManager.closeAndUnregister(database)
    }
}
