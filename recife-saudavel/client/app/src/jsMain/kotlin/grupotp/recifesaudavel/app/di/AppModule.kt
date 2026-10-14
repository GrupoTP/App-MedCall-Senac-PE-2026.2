package grupotp.recifesaudavel.app.di

import grupotp.recifesaudavel.app.gateway.BrowserOfflineShell
import grupotp.recifesaudavel.app.gateway.RestAccountGateway
import grupotp.recifesaudavel.app.gateway.RestAppointmentGateway
import grupotp.recifesaudavel.app.gateway.RestAuthGateway
import grupotp.recifesaudavel.app.gateway.RestPreTriageGateway
import grupotp.recifesaudavel.app.gateway.SqliteLocalAccounts
import grupotp.recifesaudavel.app.platform.apiBaseUrl
import grupotp.recifesaudavel.app.usecase.AccountGateway
import grupotp.recifesaudavel.app.usecase.AppShellGateway
import grupotp.recifesaudavel.app.usecase.AppointmentGateway
import grupotp.recifesaudavel.app.usecase.AuthGateway
import grupotp.recifesaudavel.app.usecase.BookAppointment
import grupotp.recifesaudavel.app.usecase.ChangePassword
import grupotp.recifesaudavel.app.usecase.CancelAppointment
import grupotp.recifesaudavel.app.usecase.ListAppointments
import grupotp.recifesaudavel.app.usecase.LoadAccountProfile
import grupotp.recifesaudavel.app.usecase.LoadAvailability
import grupotp.recifesaudavel.app.usecase.LoadPendingPreTriage
import grupotp.recifesaudavel.app.usecase.LoadPreTriageDraft
import grupotp.recifesaudavel.app.usecase.LoadSchedulingOptions
import grupotp.recifesaudavel.app.usecase.LoadSignedInAccount
import grupotp.recifesaudavel.app.usecase.LocalAccountGateway
import grupotp.recifesaudavel.app.usecase.LocalAppointmentGateway
import grupotp.recifesaudavel.app.usecase.PreTriageGateway
import grupotp.recifesaudavel.app.usecase.PrepareOfflineShell
import grupotp.recifesaudavel.app.usecase.RefreshAccount
import grupotp.recifesaudavel.app.usecase.RegisterAccount
import grupotp.recifesaudavel.app.usecase.RequestAccountDeletion
import grupotp.recifesaudavel.app.usecase.RememberSignedInAccount
import grupotp.recifesaudavel.app.usecase.RequestPasswordRecovery
import grupotp.recifesaudavel.app.usecase.SaveAccountProfile
import grupotp.recifesaudavel.app.usecase.SignIn
import grupotp.recifesaudavel.app.usecase.SignOut
import grupotp.recifesaudavel.app.usecase.SubmitPreTriage
import grupotp.recifesaudavel.app.usecase.UpdateNotificationConsent
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
    val localGateways = arrayOf(AppShellGateway::class, LocalAccountGateway::class, LocalAppointmentGateway::class)
    if (platform == WEB) {
        single { BrowserOfflineShell() } binds localGateways
    } else {
        single { SqliteLocalAccounts() } binds localGateways
    }
    singleOf(::SignIn)
    singleOf(::RegisterAccount)
    singleOf(::RequestPasswordRecovery)
    singleOf(::PrepareOfflineShell)
    singleOf(::RememberSignedInAccount)
    singleOf(::LoadSignedInAccount)
    singleOf(::SignOut)
    singleOf(::LoadSchedulingOptions)
    singleOf(::LoadAvailability)
    singleOf(::RefreshAccount)
    singleOf(::BookAppointment)
    singleOf(::CancelAppointment)
    singleOf(::ListAppointments)
    singleOf(::LoadPreTriageDraft)
    singleOf(::SubmitPreTriage)
    singleOf(::LoadPendingPreTriage)
    singleOf(::LoadAccountProfile)
    singleOf(::SaveAccountProfile)
    singleOf(::UpdateNotificationConsent)
    singleOf(::ChangePassword)
    singleOf(::RequestAccountDeletion)
}
