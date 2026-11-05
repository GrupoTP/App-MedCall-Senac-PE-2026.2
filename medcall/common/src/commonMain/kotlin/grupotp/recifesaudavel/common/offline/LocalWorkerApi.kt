package grupotp.recifesaudavel.common.offline

/**
 * What the web page and its service worker (client/worker) agree on. The page reaches the
 * device's local database only through these same-origin paths, which the worker answers.
 */
object LocalWorkerApi {
    /** Signed-in registration profile. GET, PUT, and DELETE, which wipes the device's local data. */
    const val ACCOUNT_PATH = "/local/account"

    /** Appointments of the account named by [ACCOUNT_PARAMETER]. GET and PUT. */
    const val APPOINTMENTS_PATH = "/local/appointments"
    const val ACCOUNT_PARAMETER = "account"

    /** Session tokens waiting to be revoked on the server after an offline sign-out. GET and PUT. */
    const val PENDING_SIGN_OUTS_PATH = "/local/pending-sign-outs"

    /**
     * Posted by the page when it opens without a controller, as after a hard reload (Shift+F5):
     * an active worker only claims pages when it activates, so the page asks again.
     */
    const val CLAIM_MESSAGE = "recife-saudavel:claim"
}
