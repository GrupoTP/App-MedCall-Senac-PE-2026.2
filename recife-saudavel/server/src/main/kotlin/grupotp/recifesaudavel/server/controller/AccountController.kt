package grupotp.recifesaudavel.server.controller

import grupotp.recifesaudavel.common.account.ChangePasswordRequest
import grupotp.recifesaudavel.common.account.UpdateConsentsRequest
import grupotp.recifesaudavel.common.account.UpdateProfileRequest
import grupotp.recifesaudavel.common.api.ApiError
import grupotp.recifesaudavel.server.plugin.authenticatedRoute
import grupotp.recifesaudavel.server.service.AccountResult
import grupotp.recifesaudavel.server.service.AccountService
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

    routing {
        authenticatedRoute("/api/v1/account") {
            get {
                val profile = accounts.profile(call.signedIn.userId)
                if (profile == null) {
                    call.respondSessionExpired()
                } else {
                    call.respond(profile)
                }
            }
            put {
                call.respondResult(accounts.update(call.signedIn.userId, call.receive<UpdateProfileRequest>()))
            }
            put("/consents") {
                val request = call.receive<UpdateConsentsRequest>()
                call.respondResult(accounts.updateNotifications(call.signedIn.userId, request.wantsNotifications))
            }
            post("/password") {
                val session = call.signedIn
                val request = call.receive<ChangePasswordRequest>()
                call.respondResult(accounts.changePassword(session.userId, request, keepSessionId = session.sessionId))
            }
            post("/deletion-request") {
                call.respondResult(accounts.requestDeletion(call.signedIn.userId))
            }
        }
    }
}

private suspend fun ApplicationCall.respondResult(result: AccountResult) {
    when (result) {
        is AccountResult.Done -> respond(result.profile)
        is AccountResult.Rejected -> respond(HttpStatusCode.BadRequest, ApiError(result.message))
        AccountResult.UnknownUser -> respondSessionExpired()
    }
}
