package grupotp.recifesaudavel.worker.routing

sealed interface WorkerRoute {
    /** The HTML page. Network first, stored copy when offline. */
    data object Document : WorkerRoute

    /** Static files required to open the interface. */
    data object Shell : WorkerRoute

    /** Signed-in registration profile kept in the local database. */
    data object LocalAccount : WorkerRoute

    /** Appointments of the signed-in account, readable offline. */
    data object LocalAppointments : WorkerRoute

    /** Session tokens waiting to be revoked on the server after an offline sign-out. */
    data object LocalPendingSignOuts : WorkerRoute

    /** API and health responses on the page's origin. Must not be written to the cache. */
    data object Server : WorkerRoute

    /** Leave the request to the browser: other origins and files the worker does not keep. */
    data object Ignore : WorkerRoute
}
