package grupotp.medcall.worker.local

import grupotp.medcall.common.auth.AuthenticatedAccount
import grupotp.medcall.common.offline.LocalWorkerApi
import grupotp.medcall.worker.http.RequestHandler
import grupotp.medcall.worker.http.WorkerResponses
import grupotp.medcall.worker.storage.LocalAccountDatabase
import kotlinx.serialization.json.Json
import web.http.DELETE
import web.http.GET
import web.http.PUT
import web.http.Request
import web.http.RequestMethod
import web.http.Response
import web.http.text

/** [LocalWorkerApi.ACCOUNT_PATH]: the signed-in profile. DELETE wipes the device's local data. */
class LocalAccountHandler(
    private val database: LocalAccountDatabase,
    private val json: Json,
) : RequestHandler {
    // RequestMethod is a JS string union, so the compiler takes the `when` as exhaustive; other methods still arrive.
    @Suppress("REDUNDANT_ELSE_IN_WHEN")
    override suspend fun handle(request: Request): Response = when (request.method) {
        RequestMethod.GET -> load()
        RequestMethod.PUT -> save(request)
        RequestMethod.DELETE -> clear()
        else -> WorkerResponses.methodNotAllowed(RequestMethod.GET, RequestMethod.PUT, RequestMethod.DELETE)
    }

    private suspend fun load(): Response {
        val account = database.loadAccount() ?: return WorkerResponses.noContent()
        return WorkerResponses.json(json.encodeToString(account))
    }

    private suspend fun save(request: Request): Response {
        database.saveAccount(json.decodeFromString<AuthenticatedAccount>(request.text()))
        return WorkerResponses.noContent()
    }

    private suspend fun clear(): Response {
        database.clear()
        return WorkerResponses.noContent()
    }
}
