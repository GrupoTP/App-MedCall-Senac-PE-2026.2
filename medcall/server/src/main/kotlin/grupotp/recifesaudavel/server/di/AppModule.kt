package grupotp.recifesaudavel.server.di

import grupotp.recifesaudavel.server.service.HealthService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
    singleOf(::HealthService)
}
