package grupotp.recifesaudavel.server.plugin

import grupotp.recifesaudavel.server.database.table.AppointmentsTable
import grupotp.recifesaudavel.server.database.table.ClinicsTable
import grupotp.recifesaudavel.server.database.DatabaseConnector
import grupotp.recifesaudavel.server.database.DatabaseSettings
import grupotp.recifesaudavel.server.database.LegacySchemaCheck
import grupotp.recifesaudavel.server.database.table.PreTriagesTable
import grupotp.recifesaudavel.server.database.table.ProfessionalsTable
import grupotp.recifesaudavel.server.database.table.SessionsTable
import grupotp.recifesaudavel.server.database.table.SpecialtiesTable
import grupotp.recifesaudavel.server.database.table.UsersTable
import grupotp.recifesaudavel.server.repository.PreTriageRepository
import grupotp.recifesaudavel.server.repository.SchedulingRepository
import grupotp.recifesaudavel.server.repository.SessionRepository
import grupotp.recifesaudavel.server.repository.UserRepository
import grupotp.recifesaudavel.server.security.JwtSettings
import grupotp.recifesaudavel.server.security.PasswordHasher
import grupotp.recifesaudavel.server.security.SessionTokens
import grupotp.recifesaudavel.server.service.AccountService
import grupotp.recifesaudavel.server.service.AuthService
import grupotp.recifesaudavel.server.service.PreTriageService
import grupotp.recifesaudavel.server.service.SchedulingService
import grupotp.recifesaudavel.server.service.SessionService
import grupotp.recifesaudavel.server.security.SessionSettings
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.ktor.ext.getKoin
import java.time.Clock


fun Application.configureDatabase() {
    val settings = DatabaseSettings.from(environment.config)
    getKoin().loadModules(
        listOf(
            module {
                single { settings }
                single { SessionSettings.from(environment.config) }
                single { JwtSettings.from(environment.config) }
                singleOf(::SessionTokens)
                single { DatabaseConnector().connect(get()) }
                singleOf(::PasswordHasher)
                // UTC: each clinic has its own zone, and the services convert to it where it matters.
                single { Clock.systemUTC() }
                singleOf(::UserRepository)
                singleOf(::SessionRepository)
                singleOf(::SchedulingRepository)
                singleOf(::PreTriageRepository)
                singleOf(::SessionService)
                singleOf(::AuthService)
                singleOf(::AccountService)
                singleOf(::SchedulingService)
                singleOf(::PreTriageService)
            },
        ),
    )
    val database = getKoin().get<Database>()
    LegacySchemaCheck.run(database)
    transaction(database) {
        SchemaUtils.create(
            UsersTable,
            SessionsTable,
            ClinicsTable,
            SpecialtiesTable,
            ProfessionalsTable,
            AppointmentsTable,
            PreTriagesTable
        )
    }
    getKoin().get<SchedulingRepository>().seedCatalogIfEmpty()
    monitor.subscribe(ApplicationStopped) {
        TransactionManager.closeAndUnregister(database)
    }
}
