package br.senac.recifesaudavel.server

import br.senac.recifesaudavel.common.appJson
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.routing.routing

fun main() {
    println("Recife Saudável API em http://localhost:$SERVER_PORT")
    println("Usuário mock: ${MockUser.EMAIL} / ${MockUser.PASSWORD}")
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module(authService: AuthService = AuthService()) {
    install(ContentNegotiation) {
        json(appJson)
    }
    install(CORS) {
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Accept)
        allowHeader(HttpHeaders.Authorization)
        anyHost()
    }
    routing {
        sessionRoutes(authService)
    }
}

const val SERVER_PORT = 8080
