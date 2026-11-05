package grupotp.medcall.worker

import grupotp.medcall.worker.local.LocalAccountHandler
import grupotp.medcall.worker.local.LocalAppointmentsHandler
import grupotp.medcall.worker.local.PendingSignOutsHandler
import grupotp.medcall.worker.routing.WorkerRequestRouter
import grupotp.medcall.worker.routing.WorkerRoute
import grupotp.medcall.worker.shell.DocumentHandler
import grupotp.medcall.worker.shell.ShellAssetHandler
import grupotp.medcall.worker.shell.ShellCache
import grupotp.medcall.worker.storage.LocalAccountDatabase
import kotlinx.serialization.json.Json
import web.serviceworker.ServiceWorkerModule

/** Builds the worker's parts and starts listening to the browser's events. */
fun main() {
    ServiceWorkerModule { self ->
        val json = Json { ignoreUnknownKeys = true }
        val shell = ShellCache()
        val database = LocalAccountDatabase(json)
        OfflineWorker(
            self = self,
            shell = shell,
            router = WorkerRequestRouter(origin = self.location.origin),
            handlers = mapOf(
                WorkerRoute.Document to DocumentHandler(shell),
                WorkerRoute.Shell to ShellAssetHandler(shell),
                WorkerRoute.LocalAccount to LocalAccountHandler(database, json),
                WorkerRoute.LocalAppointments to LocalAppointmentsHandler(database, json),
                WorkerRoute.LocalPendingSignOuts to PendingSignOutsHandler(database, json),
            ),
        ).start()
    }
}
