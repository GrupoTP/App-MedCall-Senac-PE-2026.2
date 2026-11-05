package grupotp.medcall.app.di

import grupotp.medcall.app.domain.session.PendingSignOutQueue
import grupotp.medcall.app.gateway.account.AccountGateway
import grupotp.medcall.app.gateway.account.RestAccountGateway
import grupotp.medcall.app.gateway.auth.AuthGateway
import grupotp.medcall.app.gateway.auth.RestAuthGateway
import grupotp.medcall.app.gateway.http.apiHttpClient
import grupotp.medcall.app.gateway.local.AppShellGateway
import grupotp.medcall.app.gateway.local.BrowserOfflineShell
import grupotp.medcall.app.gateway.local.LocalAccountGateway
import grupotp.medcall.app.gateway.local.LocalAppointmentGateway
import grupotp.medcall.app.gateway.local.PendingSignOutGateway
import grupotp.medcall.app.gateway.local.SqliteLocalAccounts
import grupotp.medcall.app.gateway.pretriage.PreTriageGateway
import grupotp.medcall.app.gateway.pretriage.RestPreTriageGateway
import grupotp.medcall.app.gateway.scheduling.AppointmentGateway
import grupotp.medcall.app.gateway.scheduling.RestAppointmentGateway
import grupotp.medcall.app.platform.apiBaseUrl
import grupotp.medcall.app.usecase.account.ChangePasswordUseCase
import grupotp.medcall.app.usecase.account.LoadAccountProfileUseCase
import grupotp.medcall.app.usecase.account.RequestAccountDeletionUseCase
import grupotp.medcall.app.usecase.account.SaveAccountProfileUseCase
import grupotp.medcall.app.usecase.account.UpdateNotificationConsentUseCase
import grupotp.medcall.app.usecase.auth.RegisterAccountUseCase
import grupotp.medcall.app.usecase.auth.RequestPasswordRecoveryUseCase
import grupotp.medcall.app.usecase.auth.SignInUseCase
import grupotp.medcall.app.usecase.offline.PrepareOfflineShellUseCase
import grupotp.medcall.app.usecase.pretriage.LoadPendingPreTriageUseCase
import grupotp.medcall.app.usecase.pretriage.LoadPreTriageDraftUseCase
import grupotp.medcall.app.usecase.pretriage.SubmitPreTriageUseCase
import grupotp.medcall.app.usecase.scheduling.BookAppointmentUseCase
import grupotp.medcall.app.usecase.scheduling.CancelAppointmentUseCase
import grupotp.medcall.app.usecase.scheduling.ListAppointmentsUseCase
import grupotp.medcall.app.usecase.scheduling.LoadAvailabilityUseCase
import grupotp.medcall.app.usecase.scheduling.LoadSchedulingOptionsUseCase
import grupotp.medcall.app.usecase.session.FlushPendingSignOutsUseCase
import grupotp.medcall.app.usecase.session.LoadSignedInAccountUseCase
import grupotp.medcall.app.usecase.session.RefreshAccountUseCase
import grupotp.medcall.app.usecase.session.RememberSignedInAccountUseCase
import grupotp.medcall.app.usecase.session.SignOutUseCase
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
