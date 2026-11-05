package grupotp.medcall.server.di

import grupotp.medcall.server.service.HealthService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
    singleOf(::HealthService)
}
