package grupotp.recifesaudavel.server.controller

import grupotp.recifesaudavel.common.api.ApiError
import grupotp.recifesaudavel.common.scheduling.CreateAppointmentRequest
import grupotp.recifesaudavel.server.plugin.authenticatedRoute
import grupotp.recifesaudavel.server.service.BookingResult
import grupotp.recifesaudavel.server.service.CancelResult
import grupotp.recifesaudavel.server.service.SchedulingService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

fun Application.configureSchedulingController() {
    val scheduling by inject<SchedulingService>()

    routing {
        authenticatedRoute("/api/v1") {
            get("/specialties") {
                call.respond(scheduling.specialties())
            }
            get("/professionals") {
                call.respond(scheduling.professionals(call.request.queryParameters["specialtyId"]))
            }
            get("/clinics") {
                call.respond(scheduling.clinics())
            }
            get("/availability") {
                val specialtyId = call.request.queryParameters["specialtyId"].orEmpty()
                val professionalId = call.request.queryParameters["professionalId"]?.takeIf { it.isNotBlank() }
                val days = scheduling.availability(specialtyId, professionalId)
                if (days == null) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("Especialidade ou profissional inválido."))
                } else {
                    call.respond(days)
                }
            }
            route("/appointments") {
                get {
                    call.respond(scheduling.appointmentsOf(call.signedIn.userId))
                }
                post("/{id}/cancel") {
                    val id = call.parameters["id"].orEmpty()
                    when (val result = scheduling.cancel(call.signedIn.userId, id)) {
                        is CancelResult.Cancelled -> call.respond(result.appointment)
                        CancelResult.NotFound -> call.respond(HttpStatusCode.NotFound, ApiError("Consulta não encontrada."))
                        is CancelResult.NotCancellable -> call.respond(HttpStatusCode.Conflict, ApiError(result.message))
                    }
                }
                post {
                    val request = call.receive<CreateAppointmentRequest>()
                    when (val result = scheduling.book(call.signedIn.userId, request)) {
                        is BookingResult.Created -> call.respond(HttpStatusCode.Created, result.appointment)
                        is BookingResult.Rejected -> call.respond(HttpStatusCode.BadRequest, ApiError(result.message))
                        is BookingResult.Conflict -> call.respond(HttpStatusCode.Conflict, ApiError(result.message))
                    }
                }
            }
        }
    }
}
