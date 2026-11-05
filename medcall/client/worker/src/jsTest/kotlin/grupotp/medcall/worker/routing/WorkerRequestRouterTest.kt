package grupotp.recifesaudavel.worker.routing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val ORIGIN = "http://localhost:8081"

class WorkerRequestRouterTest {
    @Test
    fun thePageComesFromTheNetworkFirst() {
        // Arrange
        val router = WorkerRequestRouter(ORIGIN)

        // Act
        val routes = listOf(
            "http://localhost:8081/",
            "http://localhost:8081/index.html",
        ).map(router::route)

        // Assert
        assertTrue(routes.all { it == WorkerRoute.Document })
    }

    @Test
    fun essentialFilesAndExpoBuildOutputGoToTheShellCache() {
        // Arrange
        val router = WorkerRequestRouter(ORIGIN)

        // Act
        val routes = listOf(
            "http://localhost:8081/manifest.json",
            "http://localhost:8081/icons/icon-192.png",
            "http://localhost:8081/icons/icon-512.png?v=1",
            "http://localhost:8081/_expo/static/js/web/index-3f2a9c.js",
            "http://localhost:8081/assets/node_modules/@expo-google-fonts/nunito/400Regular/Nunito_400Regular.ttf",
        ).map(router::route)

        // Assert
        assertTrue(routes.all { it == WorkerRoute.Shell })
    }

    @Test
    fun metroDevelopmentBundleIsLeftToTheBrowser() {
        // Arrange
        val router = WorkerRequestRouter(ORIGIN)

        // Act
        val route = router.route("http://localhost:8081/index.bundle?platform=web&dev=true")

        // Assert
        assertEquals(WorkerRoute.Ignore, route)
    }

    @Test
    fun serverAndHealthResponsesAreNeverCached() {
        // Arrange
        val router = WorkerRequestRouter(ORIGIN)

        // Act
        val login = router.route("$ORIGIN/api/v1/auth/login")
        val health = router.route("$ORIGIN/healthz")
        val ready = router.route("$ORIGIN/readyz")

        // Assert
        assertEquals(WorkerRoute.Server, login)
        assertEquals(WorkerRoute.Server, health)
        assertEquals(WorkerRoute.Server, ready)
    }

    @Test
    fun otherOriginsAreLeftToTheBrowserWhateverThePath() {
        // Arrange
        val router = WorkerRequestRouter(ORIGIN)

        // Act
        val routes = listOf(
            "http://localhost:8080/api/v1/auth/login",
            "https://api.recifesaudavel.example/api/v1/appointments",
            "https://api.recifesaudavel.example/",
            "https://cdn.example.com/assets/font.ttf",
            "https://other.example/local/account",
            "http://localhost:8082/local/pending-sign-outs",
        ).map(router::route)

        // Assert
        assertTrue(routes.all { it == WorkerRoute.Ignore })
    }

    @Test
    fun signedInProfileUsesTheLocalDatabase() {
        // Arrange
        val router = WorkerRequestRouter(ORIGIN)

        // Act
        val route = router.route("http://localhost:8081/local/account")

        // Assert
        assertEquals(WorkerRoute.LocalAccount, route)
    }

    @Test
    fun appointmentsUseTheLocalDatabaseWhateverTheQuery() {
        // Arrange
        val router = WorkerRequestRouter(ORIGIN)

        // Act
        val route = router.route("http://localhost:8081/local/appointments?account=12&x=1")

        // Assert
        assertEquals(WorkerRoute.LocalAppointments, route)
    }

    @Test
    fun pendingSignOutsHaveTheirOwnLocalRoute() {
        // Arrange
        val router = WorkerRequestRouter(ORIGIN)

        // Act
        val route = router.route("http://localhost:8081/local/pending-sign-outs")

        // Assert
        assertEquals(WorkerRoute.LocalPendingSignOuts, route)
    }
}
