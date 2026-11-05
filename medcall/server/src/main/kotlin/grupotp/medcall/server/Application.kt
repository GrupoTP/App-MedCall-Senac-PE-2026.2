package grupotp.medcall.server

import grupotp.medcall.server.controller.configureAccountController
import grupotp.medcall.server.controller.configureAuthController
import grupotp.medcall.server.controller.configureHealthController
import grupotp.medcall.server.controller.configurePreTriageController
import grupotp.medcall.server.controller.configureSchedulingController
import grupotp.medcall.server.plugin.configureAuthentication
import grupotp.medcall.server.plugin.configureCors
import grupotp.medcall.server.plugin.configureDatabase
import grupotp.medcall.server.plugin.configureDependencyInjection
import grupotp.medcall.server.plugin.configureSerialization
import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    // Plugins
    configureDependencyInjection()
    configureDatabase()
    configureSerialization()
    configureCors()
    configureAuthentication()

    // Controllers
    configureHealthController()
    configureAuthController()
    configureAccountController()
    configureSchedulingController()
    configurePreTriageController()
}
