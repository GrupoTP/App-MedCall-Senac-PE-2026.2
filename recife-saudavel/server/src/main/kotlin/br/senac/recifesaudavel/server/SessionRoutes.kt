package br.senac.recifesaudavel.server

import br.senac.recifesaudavel.common.ErrorResponse
import br.senac.recifesaudavel.common.LoginRequest
import br.senac.recifesaudavel.common.validationError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.sessionRoutes(authService: AuthService) {
    route("/api/sessions") {
        post {
            val request = call.receive<LoginRequest>()
            val validationMessage = request.validationError()
            if (validationMessage != null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(validationMessage))
                return@post
            }

            val session = authService.login(request)
            if (session == null) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("E-mail ou senha inválidos."),
                )
                return@post
            }

            call.respond(session)
        }

        get("/current") {
            val session = authService.current(call.bearerToken())
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Sessão inválida."))
                return@get
            }
            call.respond(session)
        }

        delete("/current") {
            val removed = authService.logout(call.bearerToken())
            if (!removed) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Sessão inválida."))
                return@delete
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun ApplicationCall.bearerToken(): String? {
    val header = request.headers[HttpHeaders.Authorization] ?: return null
    if (!header.startsWith("Bearer ")) {
        return null
    }
    return header.removePrefix("Bearer ").trim().takeIf { it.isNotEmpty() }
}
