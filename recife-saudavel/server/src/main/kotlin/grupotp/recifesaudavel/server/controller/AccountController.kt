package grupotp.recifesaudavel.server.controller

import grupotp.recifesaudavel.common.ApiError
import grupotp.recifesaudavel.common.ChangePasswordRequest
import grupotp.recifesaudavel.common.UpdateConsentsRequest
import grupotp.recifesaudavel.common.UpdateProfileRequest
import grupotp.recifesaudavel.server.service.AccountResult
import grupotp.recifesaudavel.server.service.AccountService
import grupotp.recifesaudavel.server.service.SessionService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

/**
 * Settings of the signed-in person. Every route needs the session token.
 */
fun Application.configureAccountController() {
    val accounts by inject<AccountService>()
    val sessions by inject<SessionService>()

    routing {
        route("/api/v1/account") {
            get {
                val userId = call.authenticatedUserId(sessions) ?: return@get
                val profile = accounts.profile(userId)
                if (profile == null) {
                    call.respondUnknownUser()
                } else {
                    call.respond(profile)
                }
            }
            put {
                val userId = call.authenticatedUserId(sessions) ?: return@put
                call.respondResult(accounts.update(userId, call.receive<UpdateProfileRequest>()))
            }
            put("/consents") {
                val userId = call.authenticatedUserId(sessions) ?: return@put
                val request = call.receive<UpdateConsentsRequest>()
                call.respondResult(accounts.updateNotifications(userId, request.wantsNotifications))
            }
            post("/password") {
                val userId = call.authenticatedUserId(sessions) ?: return@post
                call.respondResult(accounts.changePassword(userId, call.receive<ChangePasswordRequest>()))
            }
            post("/deletion-request") {
                val userId = call.authenticatedUserId(sessions) ?: return@post
                call.respondResult(accounts.requestDeletion(userId))
            }
        }
    }
}

private suspend fun ApplicationCall.respondResult(result: AccountResult) {
    when (result) {
        is AccountResult.Done -> respond(result.profile)
        is AccountResult.Rejected -> respond(HttpStatusCode.BadRequest, ApiError(result.message))
        AccountResult.UnknownUser -> respondUnknownUser()
    }
}

private suspend fun ApplicationCall.respondUnknownUser() {
    respond(HttpStatusCode.Unauthorized, ApiError("Sua sessão expirou. Entre de novo para continuar."))
}
