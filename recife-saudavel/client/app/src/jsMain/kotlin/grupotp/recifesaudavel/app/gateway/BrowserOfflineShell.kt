package grupotp.recifesaudavel.app.gateway

import grupotp.recifesaudavel.app.usecase.AppShellGateway
import grupotp.recifesaudavel.app.usecase.LocalAccountGateway
import grupotp.recifesaudavel.common.AuthenticatedAccount
import js.promise.await
import js.reflect.unsafeCast
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import web.events.EventHandler
import web.events.ProgressEvent
import web.http.RequestMethod
import web.navigator.navigator
import web.serviceworker.ServiceWorkerContainer
import web.xhr.XMLHttpRequest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.seconds

private const val LOCAL_ACCOUNT_URL = "/local/account"
private const val SERVICE_WORKER_URL = "/sw.js"

/** Handled by client/worker Main.kt; keep both in sync. */
private const val CLAIM_MESSAGE = "recife-saudavel:claim"
private val CONTROLLER_TIMEOUT = 10.seconds

private val getMethod: RequestMethod = unsafeCast("GET")
private val putMethod: RequestMethod = unsafeCast("PUT")

/**
 * Registers `sw.js` and talks to the Kotlin worker through `/local/account`.
 * The worker is the only component that writes the local database.
 */
class BrowserOfflineShell(
    private val json: Json = Json { ignoreUnknownKeys = true },
) : AppShellGateway, LocalAccountGateway {
    private var ready: Deferred<Unit>? = null

    override suspend fun prepare() {
        ensureReady()
    }

    override suspend fun save(account: AuthenticatedAccount) {
        ensureReady()
        val (status, text) = request(putMethod, json.encodeToString(account))
        if (status !in 200..299) {
            throw IllegalStateException(text.ifBlank { "Não foi possível guardar o cadastro neste aparelho." })
        }
    }

    override suspend fun load(): AuthenticatedAccount? {
        ensureReady()
        val (status, text) = request(getMethod, null)
        if (status == 204.toShort() || text.isBlank()) {
            return null
        }
        if (status !in 200..299) {
            return null
        }
        return json.decodeFromString(text)
    }

    private suspend fun ensureReady() {
        val current = ready
        if (current != null) {
            current.await()
            return
        }
        val created = CompletableDeferred<Unit>()
        ready = created
        try {
            registerAndClaim()
            created.complete(Unit)
        } catch (error: Throwable) {
            ready = null
            created.completeExceptionally(error)
            throw error
        }
    }

    /**
     * On the first visit the worker claims the page when it activates. After a hard reload
     * (Shift+F5) the page opens without a controller while the worker is already active, so it
     * would never claim it on its own: we ask for it.
     */
    private suspend fun registerAndClaim() {
        val container = navigator.serviceWorker
        container.registerAsync(SERVICE_WORKER_URL).await()
        if (container.controller != null) {
            return
        }
        val controlled = CompletableDeferred<Unit>()
        val onChange: () -> Unit = { controlled.complete(Unit) }
        container.oncontrollerchange = unsafeCast<EventHandler<web.events.Event, ServiceWorkerContainer, ServiceWorkerContainer>>(onChange)
        if (container.controller != null) {
            return
        }
        container.ready.await().active?.postMessage(CLAIM_MESSAGE)
        withTimeoutOrNull(CONTROLLER_TIMEOUT) { controlled.await() }
            ?: throw IllegalStateException("O service worker não assumiu a página.")
    }

    private suspend fun request(method: RequestMethod, body: String?): Pair<Short, String> =
        suspendCancellableCoroutine { continuation ->
            val call = XMLHttpRequest()
            call.open(method, LOCAL_ACCOUNT_URL, true)
            call.setRequestHeader("Accept", "application/json")
            if (body != null) {
                call.setRequestHeader("Content-Type", "application/json")
            }
            val onLoad: () -> Unit = {
                continuation.resume(call.status to call.responseText)
            }
            val onError: () -> Unit = {
                continuation.resumeWithException(IllegalStateException("O service worker não respondeu."))
            }
            call.onload = unsafeCast<EventHandler<ProgressEvent, XMLHttpRequest, XMLHttpRequest>>(onLoad)
            call.onerror = unsafeCast<EventHandler<ProgressEvent, XMLHttpRequest, XMLHttpRequest>>(onError)
            if (body == null) {
                call.send()
            } else {
                call.send(body)
            }
        }
}
