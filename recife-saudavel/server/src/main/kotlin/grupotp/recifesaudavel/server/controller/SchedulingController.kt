package grupotp.recifesaudavel.server.controller

import grupotp.recifesaudavel.common.api.ErrorCode
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
                val specialtyId = call.request.queryParameters["specialtyId"]
                val id = specialtyId?.toIntOrNull()
                if (specialtyId != null && id == null) {
                    call.respondError(HttpStatusCode.BadRequest, ErrorCode.INVALID_CHOICE)
                } else {
                    call.respond(scheduling.professionals(id))
                }
            }
            get("/clinics") {
                call.respond(scheduling.clinics())
            }
            get("/availability") {
                val specialtyId = call.request.queryParameters["specialtyId"]?.toIntOrNull()
                val professional = call.request.queryParameters["professionalId"]?.takeIf { it.isNotBlank() }
                val professionalId = professional?.toIntOrNull()
                val days = if (specialtyId == null || (professional != null && professionalId == null)) {
                    null
                } else {
                    scheduling.availability(specialtyId, professionalId)
                }
                if (days == null) {
                    call.respondError(HttpStatusCode.BadRequest, ErrorCode.INVALID_CHOICE)
                } else {
                    call.respond(days)
                }
            }
            route("/appointments") {
                get {
                    call.respond(scheduling.appointmentsOf(call.signedIn.userId))
                }
                post("/{id}/cancel") {
                    val id = call.parameters["id"]?.toIntOrNull()
                    val result = id?.let { scheduling.cancel(call.signedIn.userId, it) } ?: CancelResult.NotFound
                    when (result) {
                        is CancelResult.Cancelled -> call.respond(result.appointment)
                        CancelResult.NotFound -> call.respondError(HttpStatusCode.NotFound, ErrorCode.APPOINTMENT_NOT_FOUND)
                        CancelResult.NotCancellable ->
                            call.respondError(HttpStatusCode.Conflict, ErrorCode.APPOINTMENT_NOT_CANCELLABLE)
                    }
                }
                post {
                    val request = call.receive<CreateAppointmentRequest>()
                    when (val result = scheduling.book(call.signedIn.userId, request)) {
                        is BookingResult.Created -> call.respond(HttpStatusCode.Created, result.appointment)
                        is BookingResult.Rejected -> call.respondError(HttpStatusCode.BadRequest, result.error)
                        BookingResult.Conflict -> call.respondError(HttpStatusCode.Conflict, ErrorCode.SLOT_TAKEN)
                    }
                }
            }
        }
    }
}
