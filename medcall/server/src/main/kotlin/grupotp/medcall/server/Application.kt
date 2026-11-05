package grupotp.recifesaudavel.server

import grupotp.recifesaudavel.server.controller.configureAccountController
import grupotp.recifesaudavel.server.controller.configureAuthController
import grupotp.recifesaudavel.server.controller.configureHealthController
import grupotp.recifesaudavel.server.controller.configurePreTriageController
import grupotp.recifesaudavel.server.controller.configureSchedulingController
import grupotp.recifesaudavel.server.plugin.configureAuthentication
import grupotp.recifesaudavel.server.plugin.configureCors
import grupotp.recifesaudavel.server.plugin.configureDatabase
import grupotp.recifesaudavel.server.plugin.configureDependencyInjection
import grupotp.recifesaudavel.server.plugin.configureSerialization
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
