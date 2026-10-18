package grupotp.recifesaudavel.worker

import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.scheduling.Appointment
import grupotp.recifesaudavel.worker.cache.ShellCache
import grupotp.recifesaudavel.worker.routing.WorkerRequestRouter
import grupotp.recifesaudavel.worker.routing.WorkerRoute
import grupotp.recifesaudavel.worker.routing.queryParameter
import grupotp.recifesaudavel.worker.storage.LocalAccountDatabase
import js.promise.Promise
import js.promise.await
import js.reflect.unsafeCast
import js.void.Void
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.promise
import kotlinx.serialization.json.Json
import web.events.EventHandler
import web.http.Request
import web.http.Response
import web.serviceworker.ExtendableEvent
import web.serviceworker.ExtendableMessageEvent
import web.serviceworker.FetchEvent
import web.serviceworker.ServiceWorkerGlobalScope
import web.serviceworker.ServiceWorkerModule

/** Sent by client/app BrowserOfflineShell; keep both in sync. */
private const val CLAIM_MESSAGE = "recife-saudavel:claim"

private val json = Json { ignoreUnknownKeys = true }
private val background = CoroutineScope(SupervisorJob())

fun main() {
    val shell = ShellCache()
    val accounts = LocalAccountDatabase()

    ServiceWorkerModule { self ->
        val router = WorkerRequestRouter(origin = self.location.origin)
        val onInstall: (ExtendableEvent) -> Unit = { event ->
            event.waitUntil(
                workerPromise {
                    shell.install()
                    self.skipWaitingAsync().await<Void>()
                },
            )
        }
        val onActivate: (ExtendableEvent) -> Unit = { event ->
            event.waitUntil(
                workerPromise {
                    shell.deleteRetiredCaches()
                    self.clients.claimAsync().await<Void>()
                },
            )
        }
        // A hard reload (Shift+F5) opens the page without a controller, and an active worker only
        // claims pages when it activates. The page asks for it again with this message.
        val onMessage: (ExtendableMessageEvent) -> Unit = { event ->
            if (event.data?.toString() == CLAIM_MESSAGE) {
                event.waitUntil(self.clients.claimAsync())
            }
        }
        val onFetch: (FetchEvent) -> Unit = { event ->
            when (router.route(event.request.url)) {
                WorkerRoute.Server, WorkerRoute.Ignore -> Unit
                else -> event.respondWith(
                    responsePromise {
                        respond(shell, accounts, router, event.request)
                    },
                )
            }
        }
        self.oninstall = unsafeCast<EventHandler<ExtendableEvent, ServiceWorkerGlobalScope, ServiceWorkerGlobalScope>>(onInstall)
        self.onactivate = unsafeCast<EventHandler<ExtendableEvent, ServiceWorkerGlobalScope, ServiceWorkerGlobalScope>>(onActivate)
        self.onfetch = unsafeCast<EventHandler<FetchEvent, ServiceWorkerGlobalScope, ServiceWorkerGlobalScope>>(onFetch)
        self.onmessage = unsafeCast<EventHandler<ExtendableMessageEvent, ServiceWorkerGlobalScope, ServiceWorkerGlobalScope>>(onMessage)
    }
}

private suspend fun respond(
    shell: ShellCache,
    accounts: LocalAccountDatabase,
    router: WorkerRequestRouter,
    request: Request,
): Response = when (router.route(request.url)) {
    WorkerRoute.LocalAccount -> localAccount(accounts, request)
    WorkerRoute.LocalAppointments -> localAppointments(accounts, request)
    WorkerRoute.LocalPendingSignOuts -> localPendingSignOuts(accounts, request)
    WorkerRoute.Document -> document(shell, request)
    WorkerRoute.Shell -> shellAsset(shell, router, request)
    WorkerRoute.Server, WorkerRoute.Ignore -> error("O navegador deveria atender este pedido.")
}

private suspend fun localAccount(accounts: LocalAccountDatabase, request: Request): Response = when (request.method.toString()) {
    "GET" -> {
        val account = accounts.load()
        if (account == null) {
            emptyResponse(204)
        } else {
            jsonResponse(200, json.encodeToString(account))
        }
    }
    "PUT" -> {
        val account = json.decodeFromString<AuthenticatedAccount>(request.textAsync().await())
        accounts.save(account)
        emptyResponse(204)
    }
    "DELETE" -> {
        accounts.clear()
        emptyResponse(204)
    }
    else -> jsonResponse(405, """{"message":"Método não permitido."}""")
}

private suspend fun localPendingSignOuts(accounts: LocalAccountDatabase, request: Request): Response =
    when (request.method.toString()) {
        "GET" -> {
            val tokens = accounts.loadPendingSignOuts()
            if (tokens == null) emptyResponse(204) else jsonResponse(200, tokens)
        }
        "PUT" -> {
            accounts.savePendingSignOuts(request.textAsync().await())
            emptyResponse(204)
        }
        else -> jsonResponse(405, """{"message":"Método não permitido."}""")
    }

private suspend fun localAppointments(accounts: LocalAccountDatabase, request: Request): Response {
    val accountId = queryParameter(request.url, "account")
        ?: return jsonResponse(400, """{"message":"Conta não informada."}""")
    return when (request.method.toString()) {
        "GET" -> {
            val appointments = accounts.loadAppointments(accountId)
            if (appointments == null) {
                emptyResponse(204)
            } else {
                jsonResponse(200, json.encodeToString(appointments))
            }
        }
        "PUT" -> {
            val appointments = json.decodeFromString<List<Appointment>>(request.textAsync().await())
            accounts.saveAppointments(accountId, appointments)
            emptyResponse(204)
        }
        else -> jsonResponse(405, """{"message":"Método não permitido."}""")
    }
}

private suspend fun document(shell: ShellCache, request: Request): Response = try {
    val fresh = fetch(request).await()
    if (fresh.ok) {
        shell.store(WorkerRequestRouter.ROOT_DOCUMENT, fresh.clone())
    }
    fresh
} catch (_: Throwable) {
    shell.match(WorkerRequestRouter.ROOT_DOCUMENT) ?: offlineDocument()
}

private suspend fun shellAsset(shell: ShellCache, router: WorkerRequestRouter, request: Request): Response {
    val key = router.cacheKey(request.url)
    shell.match(key)?.let { return it }
    return try {
        val fresh = fetch(request).await()
        if (fresh.ok) {
            shell.store(key, fresh.clone())
        }
        fresh
    } catch (_: Throwable) {
        shell.match(WorkerRequestRouter.ROOT_DOCUMENT) ?: offlineDocument()
    }
}

private fun jsonResponse(status: Int, body: String): Response = response(status, body, "application/json")

// 204 and friends must not carry a body, not even an empty string, or the Response constructor throws.
private fun emptyResponse(status: Int): Response = response(status, null, "text/plain")

private fun offlineDocument(): Response = response(
    status = 503,
    body = "Sem conexão. Abra o Recife Saudável uma vez com internet para guardar a interface neste aparelho.",
    contentType = "text/plain; charset=utf-8",
)

private fun response(status: Int, body: String?, contentType: String): Response =
    createResponse(status, body, contentType)

private val responseFactory: Any = js(
    """
    function (status, body, contentType) {
      return new Response(body, { status: status, headers: { "Content-Type": contentType } });
    }
    """,
)

private val createResponse: (Int, String?, String) -> Response = unsafeCast(responseFactory)

private fun workerPromise(block: suspend () -> Unit): Promise<Void> =
    unsafeCast(background.promise { block() })

private fun responsePromise(block: suspend () -> Response): Promise<Response> =
    unsafeCast(background.promise { block() })

private external fun fetch(request: Request): Promise<Response>
