package grupotp.recifesaudavel.server

import grupotp.recifesaudavel.server.controller.configureAuthController
import grupotp.recifesaudavel.server.controller.configureHealthController
import grupotp.recifesaudavel.server.plugin.configureCors
import grupotp.recifesaudavel.server.plugin.configureDatabase
import grupotp.recifesaudavel.server.plugin.configureDependencyInjection
import grupotp.recifesaudavel.server.plugin.configureSerialization
import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    configureDependencyInjection()
    configureDatabase()
    configureSerialization()
    configureCors()
    configureHealthController()
    configureAuthController()
}
