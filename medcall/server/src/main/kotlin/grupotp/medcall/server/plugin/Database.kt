package grupotp.medcall.server.plugin

import grupotp.medcall.server.database.DemoSeed
import grupotp.medcall.server.database.MedcallSchema
import grupotp.medcall.server.integration.EventBus
import grupotp.medcall.server.integration.InMemoryEventBus
import grupotp.medcall.server.integration.IntegrationRepository
import grupotp.medcall.server.integration.IntegrationService
import grupotp.medcall.server.integration.RabbitMqEventBus
import grupotp.medcall.server.integration.WebhookSender
import grupotp.medcall.server.repository.AdminRepository
import grupotp.medcall.server.repository.AuditRepository
import grupotp.medcall.server.service.AdminService
import grupotp.medcall.server.repository.ClinicAgendaRepository
import grupotp.medcall.server.repository.KpiRepository
import grupotp.medcall.server.service.ClinicAgendaService
import grupotp.medcall.server.service.KpiService
import grupotp.medcall.server.repository.StaffRepository
import grupotp.medcall.server.repository.StaffSessionRepository
import grupotp.medcall.server.security.StaffTokens
import grupotp.medcall.server.service.StaffService
import grupotp.medcall.server.service.StaffSessionService
import grupotp.medcall.common.security.SecretString
import grupotp.medcall.server.database.DatabaseConnector
import grupotp.medcall.server.database.DatabaseSettings
import grupotp.medcall.server.database.LegacySchemaCheck
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
    val rabbitUrl = environment.config.property("integration.rabbitmqUrl").getString().trim()
    val allowPrivateWebhooks = environment.config.property("integration.allowPrivateWebhooks").getString().trim().toBoolean()
    getKoin().loadModules(
        listOf(
            module {
                single { settings }
                single { SessionSettings.from(environment.config) }
                single { JwtSettings.from(environment.config) }
                single { SessionTokens(get()) }
                single { StaffTokens(get()) }
                single { DatabaseConnector().connect(get()) }
                singleOf(::PasswordHasher)
                // UTC: each clinic has its own zone, and the services convert to it where it matters.
                single { Clock.systemUTC() }
                singleOf(::UserRepository)
                singleOf(::SessionRepository)
                singleOf(::SchedulingRepository)
                singleOf(::PreTriageRepository)
                single { SessionService(get<SessionRepository>(), get(), get()) }
                singleOf(::StaffRepository)
                singleOf(::StaffSessionRepository)
                singleOf(::AuditRepository)
                single { StaffSessionService(get(), get(), get()) }
                singleOf(::StaffService)
                singleOf(::ClinicAgendaRepository)
                singleOf(::ClinicAgendaService)
                singleOf(::KpiRepository)
                singleOf(::KpiService)
                singleOf(::DemoSeed)
                singleOf(::AdminRepository)
                singleOf(::AdminService)
                singleOf(::IntegrationRepository)
                single { WebhookSender(allowPrivateTargets = allowPrivateWebhooks) }
                single<EventBus> { if (rabbitUrl.isBlank()) InMemoryEventBus() else RabbitMqEventBus(rabbitUrl) }
                singleOf(::IntegrationService)
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
        SchemaUtils.create(*MedcallSchema.tables)
    }
    getKoin().get<SchedulingRepository>().seedCatalogIfEmpty()
    getKoin().get<StaffService>().bootstrap(
        email = environment.config.property("staff.bootstrapEmail").getString(),
        password = SecretString(environment.config.property("staff.bootstrapPassword").getString()),
    )
    if (environment.config.property("demo.seed").getString().trim().toBoolean()) {
        getKoin().get<DemoSeed>().runIfEmpty(SecretString(environment.config.property("demo.staffPassword").getString()))
    }
    // Outbox → bus → webhooks; runs while the application runs.
    getKoin().get<IntegrationService>().start(this)
    monitor.subscribe(ApplicationStopped) {
        getKoin().get<EventBus>().close()
        TransactionManager.closeAndUnregister(database)
    }
}
