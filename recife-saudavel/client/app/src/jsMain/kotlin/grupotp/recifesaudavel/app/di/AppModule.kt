package grupotp.recifesaudavel.app.di

import grupotp.recifesaudavel.app.domain.account.AccountGateway
import grupotp.recifesaudavel.app.domain.auth.AuthGateway
import grupotp.recifesaudavel.app.domain.offline.AppShellGateway
import grupotp.recifesaudavel.app.domain.pretriage.PreTriageGateway
import grupotp.recifesaudavel.app.domain.scheduling.AppointmentGateway
import grupotp.recifesaudavel.app.domain.scheduling.LocalAppointmentGateway
import grupotp.recifesaudavel.app.domain.session.LocalAccountGateway
import grupotp.recifesaudavel.app.domain.session.PendingSignOutGateway
import grupotp.recifesaudavel.app.domain.session.PendingSignOutQueue
import grupotp.recifesaudavel.app.gateway.BrowserOfflineShell
import grupotp.recifesaudavel.app.gateway.RestAccountGateway
import grupotp.recifesaudavel.app.gateway.RestAppointmentGateway
import grupotp.recifesaudavel.app.gateway.RestAuthGateway
import grupotp.recifesaudavel.app.gateway.RestPreTriageGateway
import grupotp.recifesaudavel.app.gateway.SqliteLocalAccounts
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
    single { RestAuthGateway(apiBaseUrl(platform)) } bind AuthGateway::class
    single { RestAppointmentGateway(apiBaseUrl(platform)) } bind AppointmentGateway::class
    single { RestPreTriageGateway(apiBaseUrl(platform)) } bind PreTriageGateway::class
    single { RestAccountGateway(apiBaseUrl(platform)) } bind AccountGateway::class
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
