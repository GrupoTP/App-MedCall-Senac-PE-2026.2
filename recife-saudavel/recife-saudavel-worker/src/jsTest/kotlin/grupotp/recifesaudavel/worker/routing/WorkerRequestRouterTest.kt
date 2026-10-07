package grupotp.recifesaudavel.worker.routing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkerRequestRouterTest {
    @Test
    fun essentialFilesGoToTheShellCache() {
        // Arrange
        val router = WorkerRequestRouter()

        // Act
        val routes = listOf(
            "http://localhost:8081/",
            "http://localhost:8081/index.html",
            "http://localhost:8081/styles.css",
            "http://localhost:8081/recife-saudavel.js",
            "http://localhost:8081/manifest.json",
            "http://localhost:8081/icons/icon-192.png",
            "http://localhost:8081/icons/icon-512.png?v=1",
        ).map(router::route)

        // Assert
        assertTrue(routes.all { it == WorkerRoute.Shell })
    }

    @Test
    fun serverAndHealthResponsesAreNeverCached() {
        // Arrange
        val router = WorkerRequestRouter()

        // Act
        val login = router.route("http://localhost:8080/api/v1/auth/login")
        val health = router.route("http://localhost:8080/healthz")
        val ready = router.route("http://localhost:8080/readyz")

        // Assert
        assertEquals(WorkerRoute.Server, login)
        assertEquals(WorkerRoute.Server, health)
        assertEquals(WorkerRoute.Server, ready)
        assertFalse(WorkerRequestRouter.ESSENTIAL_PATHS.any { it.startsWith("/api/") || it == "/healthz" })
    }

    @Test
    fun signedInProfileUsesTheLocalDatabase() {
        // Arrange
        val router = WorkerRequestRouter()

        // Act
        val route = router.route("http://localhost:8081/local/account")

        // Assert
        assertEquals(WorkerRoute.LocalAccount, route)
    }
}
