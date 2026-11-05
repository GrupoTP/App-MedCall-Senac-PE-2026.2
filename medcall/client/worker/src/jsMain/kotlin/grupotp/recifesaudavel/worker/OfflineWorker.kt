package grupotp.recifesaudavel.worker

import grupotp.recifesaudavel.common.offline.LocalWorkerApi
import grupotp.recifesaudavel.worker.http.RequestHandler
import grupotp.recifesaudavel.worker.routing.WorkerRequestRouter
import grupotp.recifesaudavel.worker.routing.WorkerRoute
import grupotp.recifesaudavel.worker.shell.ShellCache
import js.coroutines.promise
import js.promise.Promise
import js.reflect.unsafeCast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import web.events.Event
import web.events.EventHandler
import web.serviceworker.ExtendableEvent
import web.serviceworker.ExtendableMessageEvent
import web.serviceworker.FetchEvent
import web.serviceworker.ServiceWorkerGlobalScope
import web.serviceworker.claim
import web.serviceworker.skipWaiting

/**
 * Service worker of the web version: keeps the app shell for offline use and serves the local
 * database to the page. A request whose route has no handler is left to the browser.
 */
class OfflineWorker(
    private val self: ServiceWorkerGlobalScope,
    private val shell: ShellCache,
    private val router: WorkerRequestRouter,
    private val handlers: Map<WorkerRoute, RequestHandler>,
) {
    private val scope = CoroutineScope(SupervisorJob())

    fun start() {
        self.oninstall = handler(::onInstall)
        self.onactivate = handler(::onActivate)
        self.onmessage = handler(::onMessage)
        self.onfetch = handler(::onFetch)
    }

    private fun onInstall(event: ExtendableEvent) {
        event.waitUntil(
            launch {
                shell.install()
                self.skipWaiting()
            },
        )
    }

    private fun onActivate(event: ExtendableEvent) {
        event.waitUntil(
            launch {
                shell.deleteRetiredCaches()
                self.clients.claim()
            },
        )
    }

    private fun onMessage(event: ExtendableMessageEvent) {
        if (event.data?.toString() == LocalWorkerApi.CLAIM_MESSAGE) {
            event.waitUntil(self.clients.claimAsync())
        }
    }

    private fun onFetch(event: FetchEvent) {
        val handler = handlers[router.route(event.request.url)] ?: return
        event.respondWith(scope.promise { handler.handle(event.request) })
    }

    private fun launch(block: suspend () -> Unit): Promise<*> = scope.promise {
        block()
        null
    }

    /** The scope's `on...` properties take a typed handler; a plain function is what the browser calls. */
    private fun <E : Event> handler(
        block: (E) -> Unit,
    ): EventHandler<E, ServiceWorkerGlobalScope, ServiceWorkerGlobalScope> = unsafeCast(block)
}
