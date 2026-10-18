package grupotp.recifesaudavel.app.di

import grupotp.recifesaudavel.app.domain.session.PendingSignOutQueue
import grupotp.recifesaudavel.app.gateway.account.AccountGateway
import grupotp.recifesaudavel.app.gateway.account.RestAccountGateway
import grupotp.recifesaudavel.app.gateway.auth.AuthGateway
import grupotp.recifesaudavel.app.gateway.auth.RestAuthGateway
import grupotp.recifesaudavel.app.gateway.http.apiHttpClient
import grupotp.recifesaudavel.app.gateway.local.AppShellGateway
import grupotp.recifesaudavel.app.gateway.local.BrowserOfflineShell
import grupotp.recifesaudavel.app.gateway.local.LocalAccountGateway
import grupotp.recifesaudavel.app.gateway.local.LocalAppointmentGateway
import grupotp.recifesaudavel.app.gateway.local.PendingSignOutGateway
import grupotp.recifesaudavel.app.gateway.local.SqliteLocalAccounts
import grupotp.recifesaudavel.app.gateway.pretriage.PreTriageGateway
import grupotp.recifesaudavel.app.gateway.pretriage.RestPreTriageGateway
import grupotp.recifesaudavel.app.gateway.scheduling.AppointmentGateway
import grupotp.recifesaudavel.app.gateway.scheduling.RestAppointmentGateway
import grupotp.recifesaudavel.app.platform.apiBaseUrl
import grupotp.recifesaudavel.app.usecase.account.ChangePasswordUseCase
import grupotp.recifesaudavel.app.usecase.account.LoadAccountProfileUseCase
import grupotp.recifesaudavel.app.usecase.account.RequestAccountDeletionUseCase
import grupotp.recifesaudavel.app.usecase.account.SaveAccountProfileUseCase
import grupotp.recifesaudavel.app.usecase.account.UpdateNotificationConsentUseCase
import grupotp.recifesaudavel.app.usecase.auth.RegisterAccountUseCase
import grupotp.recifesaudavel.app.usecase.auth.RequestPasswordRecoveryUseCase
import grupotp.recifesaudavel.app.usecase.auth.SignInUseCase
import grupotp.recifesaudavel.app.usecase.offline.PrepareOfflineShellUseCase
import grupotp.recifesaudavel.app.usecase.pretriage.LoadPendingPreTriageUseCase
import grupotp.recifesaudavel.app.usecase.pretriage.LoadPreTriageDraftUseCase
import grupotp.recifesaudavel.app.usecase.pretriage.SubmitPreTriageUseCase
import grupotp.recifesaudavel.app.usecase.scheduling.BookAppointmentUseCase
import grupotp.recifesaudavel.app.usecase.scheduling.CancelAppointmentUseCase
import grupotp.recifesaudavel.app.usecase.scheduling.ListAppointmentsUseCase
import grupotp.recifesaudavel.app.usecase.scheduling.LoadAvailabilityUseCase
import grupotp.recifesaudavel.app.usecase.scheduling.LoadSchedulingOptionsUseCase
import grupotp.recifesaudavel.app.usecase.session.FlushPendingSignOutsUseCase
import grupotp.recifesaudavel.app.usecase.session.LoadSignedInAccountUseCase
import grupotp.recifesaudavel.app.usecase.session.RefreshAccountUseCase
import grupotp.recifesaudavel.app.usecase.session.RememberSignedInAccountUseCase
import grupotp.recifesaudavel.app.usecase.session.SignOutUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

private const val WEB = "web"

/**
 * @param platform React Native `Platform.OS`. The web build keeps the service worker;
 * Android has none, so the local account and appointments live in SQLite.
 */
fun appModule(platform: String): Module = module {
    single { apiHttpClient() }
    single { RestAuthGateway(apiBaseUrl(platform), get()) } bind AuthGateway::class
    single { RestAppointmentGateway(apiBaseUrl(platform), get()) } bind AppointmentGateway::class
    single { RestPreTriageGateway(apiBaseUrl(platform), get()) } bind PreTriageGateway::class
    single { RestAccountGateway(apiBaseUrl(platform), get()) } bind AccountGateway::class
    val localGateways = arrayOf(
        AppShellGateway::class,
        LocalAccountGateway::class,
        LocalAppointmentGateway::class,
        PendingSignOutGateway::class,
    )
    if (platform == WEB) {
        single { BrowserOfflineShell() } binds localGateways
    } else {
        single { SqliteLocalAccounts() } binds localGateways
    }

    singleOf(::PendingSignOutQueue)

    singleOf(::SignInUseCase)
    singleOf(::RegisterAccountUseCase)
    singleOf(::RequestPasswordRecoveryUseCase)

    singleOf(::LoadSignedInAccountUseCase)
    singleOf(::RememberSignedInAccountUseCase)
    singleOf(::RefreshAccountUseCase)
    singleOf(::SignOutUseCase)
    singleOf(::FlushPendingSignOutsUseCase)

    singleOf(::PrepareOfflineShellUseCase)

    singleOf(::LoadSchedulingOptionsUseCase)
    singleOf(::LoadAvailabilityUseCase)
    singleOf(::BookAppointmentUseCase)
    singleOf(::CancelAppointmentUseCase)
    singleOf(::ListAppointmentsUseCase)

    singleOf(::LoadPreTriageDraftUseCase)
    singleOf(::SubmitPreTriageUseCase)
    singleOf(::LoadPendingPreTriageUseCase)

    singleOf(::LoadAccountProfileUseCase)
    singleOf(::SaveAccountProfileUseCase)
    singleOf(::UpdateNotificationConsentUseCase)
    singleOf(::ChangePasswordUseCase)
    singleOf(::RequestAccountDeletionUseCase)
}
