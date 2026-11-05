package grupotp.recifesaudavel.server.controller

import grupotp.recifesaudavel.common.api.ApiError
import grupotp.recifesaudavel.common.api.ErrorCode
import grupotp.recifesaudavel.server.i18n.ErrorMessages
import grupotp.recifesaudavel.server.domain.ServiceError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

/** Answers with the error's code and its text from [ErrorMessages]. */
internal suspend fun ApplicationCall.respondError(status: HttpStatusCode, error: ServiceError) {
    respond(status, ApiError(error.code, ErrorMessages.of(error.code, error.arguments)))
}

internal suspend fun ApplicationCall.respondError(status: HttpStatusCode, code: ErrorCode) {
    respondError(status, ServiceError(code))
}
