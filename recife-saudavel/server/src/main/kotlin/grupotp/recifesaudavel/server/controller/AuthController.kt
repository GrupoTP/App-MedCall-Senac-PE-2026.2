package grupotp.recifesaudavel.server.controller

import grupotp.recifesaudavel.common.ApiError
import grupotp.recifesaudavel.common.RegisterAccountRequest
import grupotp.recifesaudavel.common.SignInRequest
import grupotp.recifesaudavel.server.service.AuthService
import grupotp.recifesaudavel.server.service.RegistrationResult
import grupotp.recifesaudavel.server.service.SignInResult
import grupotp.recifesaudavel.server.service.SessionService
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

fun Application.configureAuthController() {
    val authService by inject<AuthService>()
    val sessions by inject<SessionService>()
    routing {
        route("/api/v1/auth") {
            post("/register") {
                val request = call.receive<RegisterAccountRequest>()
                when (val result = authService.register(request)) {
                    is RegistrationResult.Created -> call.respond(HttpStatusCode.Created, result.account)
                    is RegistrationResult.Rejected -> call.respond(HttpStatusCode.BadRequest, ApiError(result.message))
                }
            }
            get("/me") {
                val header = call.request.headers[HttpHeaders.Authorization].orEmpty()
                val userId = header.removePrefix("Bearer ").trim().takeIf { header.startsWith("Bearer ") }
                    ?.let { sessions.userIdFor(it) }
                val account = userId?.let { authService.profileOf(it) }
                if (account == null) {
                    call.respond(HttpStatusCode.Unauthorized, ApiError("Sua sessão expirou. Entre de novo para continuar."))
                } else {
                    call.respond(account)
                }
            }
            post("/login") {
                val request = call.receive<SignInRequest>()
                when (val result = authService.signIn(request)) {
                    is SignInResult.Authenticated -> call.respond(result.account)
                    is SignInResult.Rejected -> call.respond(HttpStatusCode.Unauthorized, ApiError(result.message))
                }
            }
        }
    }
}
