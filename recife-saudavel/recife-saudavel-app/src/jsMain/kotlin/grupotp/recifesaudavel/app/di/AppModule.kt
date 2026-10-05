package grupotp.recifesaudavel.app.di

import grupotp.recifesaudavel.app.gateway.RestAuthGateway
import grupotp.recifesaudavel.app.usecase.AuthGateway
import grupotp.recifesaudavel.app.usecase.ListUpcomingAppointments
import grupotp.recifesaudavel.app.usecase.RegisterAccount
import grupotp.recifesaudavel.app.usecase.RequestPasswordRecovery
import grupotp.recifesaudavel.app.usecase.SignIn
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
    single { RestAuthGateway() } bind AuthGateway::class
    singleOf(::SignIn)
    singleOf(::RegisterAccount)
    singleOf(::RequestPasswordRecovery)
    singleOf(::ListUpcomingAppointments)
}
