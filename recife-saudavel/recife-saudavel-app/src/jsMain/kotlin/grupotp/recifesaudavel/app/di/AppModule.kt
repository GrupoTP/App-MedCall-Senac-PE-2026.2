package grupotp.recifesaudavel.app.di

import grupotp.recifesaudavel.app.gateway.BrowserOfflineShell
import grupotp.recifesaudavel.app.gateway.RestAuthGateway
import grupotp.recifesaudavel.app.usecase.AppShellGateway
import grupotp.recifesaudavel.app.usecase.AuthGateway
import grupotp.recifesaudavel.app.usecase.ListUpcomingAppointments
import grupotp.recifesaudavel.app.usecase.LoadSignedInAccount
import grupotp.recifesaudavel.app.usecase.LocalAccountGateway
import grupotp.recifesaudavel.app.usecase.PrepareOfflineShell
import grupotp.recifesaudavel.app.usecase.RegisterAccount
import grupotp.recifesaudavel.app.usecase.RememberSignedInAccount
import grupotp.recifesaudavel.app.usecase.RequestPasswordRecovery
import grupotp.recifesaudavel.app.usecase.SignIn
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

val appModule = module {
    single { RestAuthGateway() } bind AuthGateway::class
    single { BrowserOfflineShell() } binds arrayOf(AppShellGateway::class, LocalAccountGateway::class)
    singleOf(::SignIn)
    singleOf(::RegisterAccount)
    singleOf(::RequestPasswordRecovery)
    singleOf(::ListUpcomingAppointments)
    singleOf(::PrepareOfflineShell)
    singleOf(::RememberSignedInAccount)
    singleOf(::LoadSignedInAccount)
}
