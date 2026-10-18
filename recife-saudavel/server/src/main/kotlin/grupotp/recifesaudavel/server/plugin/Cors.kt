package grupotp.recifesaudavel.server.plugin

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import java.net.URI

/**
 * Lets the web client call the API from its own origin. The origins come from `cors.allowedOrigins`
 * (`CORS_ALLOWED_ORIGINS`), a comma-separated list such as `https://app.example.com,http://localhost:8081`.
 * The Android app does not send an origin, so it needs no entry.
 */
fun Application.configureCors() {
    val origins = environment.config.property("cors.allowedOrigins").getString()
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map(::URI)
    install(CORS) {
        for (origin in origins) {
            val port = if (origin.port == -1) "" else ":${origin.port}"
            allowHost("${origin.host}$port", schemes = listOf(origin.scheme))
        }
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowHeader(HttpHeaders.Accept)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
    }
}
