package grupotp.medcall.server.controller

import grupotp.medcall.common.api.ErrorCode
import grupotp.medcall.common.auth.Acknowledged
import grupotp.medcall.common.auth.ConfirmPasswordRecoveryRequest
import grupotp.medcall.common.auth.PasswordRecoveryRequest
import grupotp.medcall.common.auth.RegisterAccountRequest
import grupotp.medcall.common.auth.SignInRequest
import grupotp.medcall.server.plugin.SESSION_AUTH
import grupotp.medcall.server.service.AuthService
import grupotp.medcall.server.service.PasswordRecoveryService
import grupotp.medcall.server.service.result.RecoveryResult
import grupotp.medcall.server.service.result.RegistrationResult
import grupotp.medcall.server.service.SessionService
import grupotp.medcall.server.service.result.SignInResult
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.buildJsonObject
import org.koin.ktor.ext.inject

private suspend fun io.ktor.server.application.ApplicationCall.respondRecovery(result: RecoveryResult) {
    when (result) {
        RecoveryResult.Accepted -> respond(Acknowledged())
        RecoveryResult.Unavailable -> respondError(HttpStatusCode.ServiceUnavailable, ErrorCode.MAIL_UNAVAILABLE)
        is RecoveryResult.Rejected -> respondError(HttpStatusCode.BadRequest, result.error)
    }
}

fun Application.configureAuthController() {
    val authService by inject<AuthService>()
    val recovery by inject<PasswordRecoveryService>()
    val sessions by inject<SessionService>()
    routing {
        route("/api/v1/auth") {
            post("/register") {
                val request = call.receive<RegisterAccountRequest>()
                when (val result = authService.register(request)) {
                    is RegistrationResult.Created -> call.respond(HttpStatusCode.Created, result.account)
                    is RegistrationResult.Rejected -> call.respondError(HttpStatusCode.BadRequest, result.error)
                }
            }
            post("/login") {
                val request = call.receive<SignInRequest>()
                when (val result = authService.signIn(request)) {
                    is SignInResult.Authenticated -> call.respond(result.account)
                    SignInResult.Rejected -> call.respondError(HttpStatusCode.Unauthorized, ErrorCode.WRONG_CREDENTIALS)
                }
            }
            post("/password-recovery") {
                val request = call.receive<PasswordRecoveryRequest>()
                call.respondRecovery(recovery.request(request.email))
            }
            post("/password-recovery/confirm") {
                val request = call.receive<ConfirmPasswordRecoveryRequest>()
                call.respondRecovery(recovery.confirm(request))
            }
            authenticate(SESSION_AUTH) {
                get("/me") {
                    val account = authService.profileOf(call.signedIn.userId)
                    if (account == null) {
                        call.respondSessionExpired()
                    } else {
                        call.respond(account)
                    }
                }
                post("/logout") {
                    sessions.revoke(call.signedIn.sessionId)
                    call.respond(HttpStatusCode.OK, buildJsonObject { })
                }
            }
        }
    }
}
