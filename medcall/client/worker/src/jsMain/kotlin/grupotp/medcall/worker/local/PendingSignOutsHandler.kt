package grupotp.medcall.worker.local

import grupotp.medcall.common.offline.LocalWorkerApi
import grupotp.medcall.common.security.SecretString
import grupotp.medcall.worker.http.RequestHandler
import grupotp.medcall.worker.http.WorkerResponses
import grupotp.medcall.worker.storage.LocalAccountDatabase
import kotlinx.serialization.json.Json
import web.http.GET
import web.http.PUT
import web.http.Request
import web.http.RequestMethod
import web.http.Response
import web.http.text

/** [LocalWorkerApi.PENDING_SIGN_OUTS_PATH]: tokens to revoke once the device is online again. */
class PendingSignOutsHandler(
    private val database: LocalAccountDatabase,
    private val json: Json,
) : RequestHandler {
    // RequestMethod is a JS string union, so the compiler takes the `when` as exhaustive; other methods still arrive.
    @Suppress("REDUNDANT_ELSE_IN_WHEN")
    override suspend fun handle(request: Request): Response = when (request.method) {
        RequestMethod.GET -> load()
        RequestMethod.PUT -> save(request)
        else -> WorkerResponses.methodNotAllowed(RequestMethod.GET, RequestMethod.PUT)
    }

    private suspend fun load(): Response {
        val tokens = database.loadPendingSignOuts() ?: return WorkerResponses.noContent()
        return WorkerResponses.json(json.encodeToString(tokens))
    }

    private suspend fun save(request: Request): Response {
        database.savePendingSignOuts(json.decodeFromString<List<SecretString>>(request.text()))
        return WorkerResponses.noContent()
    }
}
