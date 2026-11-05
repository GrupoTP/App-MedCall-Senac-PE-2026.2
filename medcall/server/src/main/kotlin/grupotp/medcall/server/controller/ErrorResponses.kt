package grupotp.medcall.server.controller

import grupotp.medcall.common.api.ApiError
import grupotp.medcall.common.api.ErrorCode
import grupotp.medcall.server.i18n.ErrorMessages
import grupotp.medcall.server.domain.ServiceError
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
