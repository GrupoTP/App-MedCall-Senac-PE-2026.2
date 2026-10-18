package grupotp.recifesaudavel.server.plugin

import grupotp.recifesaudavel.server.database.DatabaseConnector
import grupotp.recifesaudavel.server.database.DatabaseSettings
import grupotp.recifesaudavel.server.database.AppointmentsTable
import grupotp.recifesaudavel.server.database.PreTriagesTable
import grupotp.recifesaudavel.server.database.ProfessionalsTable
import grupotp.recifesaudavel.server.database.SchemaMigrations
import grupotp.recifesaudavel.server.database.SessionsTable
import grupotp.recifesaudavel.server.database.SpecialtiesTable
import grupotp.recifesaudavel.server.database.UsersTable
import grupotp.recifesaudavel.server.repository.PreTriageRepository
import grupotp.recifesaudavel.server.repository.SchedulingRepository
import grupotp.recifesaudavel.server.repository.SessionRepository
import grupotp.recifesaudavel.server.repository.UserRepository
import grupotp.recifesaudavel.server.security.PasswordHasher
import grupotp.recifesaudavel.server.service.AccountService
import grupotp.recifesaudavel.server.service.AuthService
import grupotp.recifesaudavel.server.service.PreTriageService
import grupotp.recifesaudavel.server.service.SchedulingService
import grupotp.recifesaudavel.server.service.SessionService
import grupotp.recifesaudavel.server.service.SessionSettings
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
import java.time.ZoneId

/** Clinics work in Recife time, whatever the server's own time zone. */
private val CLINIC_ZONE: ZoneId = ZoneId.of("America/Recife")

fun Application.configureDatabase() {
    val settings = DatabaseSettings.from(environment.config)
    getKoin().loadModules(
        listOf(
            module {
                single { settings }
                single { SessionSettings.from(environment.config) }
                single { DatabaseConnector().connect(get()) }
                singleOf(::PasswordHasher)
                single { Clock.system(CLINIC_ZONE) }
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
    transaction(database) {
        SchemaUtils.create(
            UsersTable,
            SessionsTable,
            SpecialtiesTable,
            ProfessionalsTable,
            AppointmentsTable,
            PreTriagesTable
        )
    }
    SchemaMigrations.run(database)
    getKoin().get<SchedulingRepository>().seedCatalogIfEmpty()
    monitor.subscribe(ApplicationStopped) {
        TransactionManager.closeAndUnregister(database)
    }
}
