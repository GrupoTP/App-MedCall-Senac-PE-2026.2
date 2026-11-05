package grupotp.medcall.server.plugin

import grupotp.medcall.server.database.table.AppointmentsTable
import grupotp.medcall.server.database.table.ClinicsTable
import grupotp.medcall.server.database.DatabaseConnector
import grupotp.medcall.server.database.DatabaseSettings
import grupotp.medcall.server.database.LegacySchemaCheck
import grupotp.medcall.server.database.table.PreTriagesTable
import grupotp.medcall.server.database.table.ProfessionalsTable
import grupotp.medcall.server.database.table.SessionsTable
import grupotp.medcall.server.database.table.SpecialtiesTable
import grupotp.medcall.server.database.table.UsersTable
import grupotp.medcall.server.repository.PreTriageRepository
import grupotp.medcall.server.repository.SchedulingRepository
import grupotp.medcall.server.repository.SessionRepository
import grupotp.medcall.server.repository.UserRepository
import grupotp.medcall.server.security.JwtSettings
import grupotp.medcall.server.security.PasswordHasher
import grupotp.medcall.server.security.SessionTokens
import grupotp.medcall.server.service.AccountService
import grupotp.medcall.server.service.AuthService
import grupotp.medcall.server.service.PreTriageService
import grupotp.medcall.server.service.SchedulingService
import grupotp.medcall.server.service.SessionService
import grupotp.medcall.server.security.SessionSettings
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
