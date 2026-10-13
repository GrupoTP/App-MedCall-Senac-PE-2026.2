package grupotp.recifesaudavel.server.plugin

import grupotp.recifesaudavel.server.database.DatabaseConnector
import grupotp.recifesaudavel.server.database.DatabaseSettings
import grupotp.recifesaudavel.server.database.UsersTable
import grupotp.recifesaudavel.server.repository.UserRepository
import grupotp.recifesaudavel.server.security.PasswordHasher
import grupotp.recifesaudavel.server.service.AuthService
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.ktor.ext.getKoin

fun Application.configureDatabase() {
    val settings = DatabaseSettings.from(environment.config)
    getKoin().loadModules(
        listOf(
            module {
                single { settings }
                single { DatabaseConnector().connect(get()) }
                singleOf(::PasswordHasher)
                singleOf(::UserRepository)
                singleOf(::AuthService)
            },
        ),
    )
    val database = getKoin().get<Database>()
    transaction(database) {
        SchemaUtils.create(UsersTable)
    }
    monitor.subscribe(ApplicationStopped) {
        TransactionManager.closeAndUnregister(database)
    }
}
