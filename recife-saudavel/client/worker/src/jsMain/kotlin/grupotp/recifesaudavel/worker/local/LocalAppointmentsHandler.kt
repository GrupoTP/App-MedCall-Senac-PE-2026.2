package grupotp.recifesaudavel.worker.local

import grupotp.recifesaudavel.common.offline.LocalWorkerApi
import grupotp.recifesaudavel.common.scheduling.Appointment
import grupotp.recifesaudavel.worker.http.RequestHandler
import grupotp.recifesaudavel.worker.http.WorkerResponses
import grupotp.recifesaudavel.worker.storage.LocalAccountDatabase
import kotlinx.serialization.json.Json
import web.http.GET
import web.http.PUT
import web.http.Request
import web.http.RequestMethod
import web.http.Response
import web.http.text
import web.url.URL

/** [LocalWorkerApi.APPOINTMENTS_PATH]: appointments of the account in the query, readable offline. */
class LocalAppointmentsHandler(
    private val database: LocalAccountDatabase,
    private val json: Json,
) : RequestHandler {
    // RequestMethod is a JS string union, so the compiler takes the `when` as exhaustive; other methods still arrive.
    @Suppress("REDUNDANT_ELSE_IN_WHEN")
    override suspend fun handle(request: Request): Response {
        val accountId = URL(request.url).searchParams.get(LocalWorkerApi.ACCOUNT_PARAMETER.toJsString())
            ?: return WorkerResponses.badRequest()
        return when (request.method) {
            RequestMethod.GET -> load(accountId)
            RequestMethod.PUT -> save(accountId, request)
            else -> WorkerResponses.methodNotAllowed(RequestMethod.GET, RequestMethod.PUT)
        }
    }

    private suspend fun load(accountId: String): Response {
        val appointments = database.loadAppointments(accountId) ?: return WorkerResponses.noContent()
        return WorkerResponses.json(json.encodeToString(appointments))
    }

    private suspend fun save(accountId: String, request: Request): Response {
        database.saveAppointments(accountId, json.decodeFromString<List<Appointment>>(request.text()))
        return WorkerResponses.noContent()
    }
}
