package grupotp.recifesaudavel.server.controller

import grupotp.recifesaudavel.common.api.ApiError
import grupotp.recifesaudavel.common.pretriage.PreTriageForm
import grupotp.recifesaudavel.server.service.PreTriageResult
import grupotp.recifesaudavel.server.service.PreTriageService
import grupotp.recifesaudavel.server.service.SessionService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

/**
 * Pre-triage of the signed-in patient. Every route needs the session token.
 */
fun Application.configurePreTriageController() {
    val preTriages by inject<PreTriageService>()
    val sessions by inject<SessionService>()

    routing {
        route("/api/v1/pre-triages") {
            get("/draft") {
                val patientId = call.authenticatedUserId(sessions) ?: return@get
                val draft = preTriages.draft(patientId)
                if (draft == null) {
                    call.respond(HttpStatusCode.Unauthorized, ApiError("Sua sessão expirou. Entre de novo para continuar."))
                } else {
                    call.respond(draft)
                }
            }
            get("/pending") {
                val patientId = call.authenticatedUserId(sessions) ?: return@get
                call.respond(preTriages.pending(patientId))
            }
            post {
                val patientId = call.authenticatedUserId(sessions) ?: return@post
                val form = call.receive<PreTriageForm>()
                when (val result = preTriages.submit(patientId, form)) {
                    is PreTriageResult.Created -> call.respond(HttpStatusCode.Created, result.preTriage)
                    is PreTriageResult.Rejected -> call.respond(HttpStatusCode.BadRequest, ApiError(result.message))
                }
            }
        }
    }
}
