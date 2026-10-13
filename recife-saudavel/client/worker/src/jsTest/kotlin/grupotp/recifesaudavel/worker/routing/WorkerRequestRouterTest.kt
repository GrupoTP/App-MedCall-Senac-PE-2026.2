package grupotp.recifesaudavel.worker.routing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkerRequestRouterTest {
    @Test
    fun thePageComesFromTheNetworkFirst() {
        // Arrange
        val router = WorkerRequestRouter()

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
        val router = WorkerRequestRouter()

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
        val router = WorkerRequestRouter()

        // Act
        val route = router.route("http://localhost:8081/index.bundle?platform=web&dev=true")

        // Assert
        assertEquals(WorkerRoute.Ignore, route)
    }

    @Test
    fun buildOutputKeepsTheQueryInTheCacheKey() {
        // Arrange
        val router = WorkerRequestRouter()

        // Act
        val icon = router.cacheKey("http://localhost:8081/icons/icon-512.png?v=1")
        val first = router.cacheKey("http://localhost:8081/assets/?unstable_path=a.ttf")
        val second = router.cacheKey("http://localhost:8081/assets/?unstable_path=b.ttf")

        // Assert
        assertEquals("/icons/icon-512.png", icon)
        assertEquals("http://localhost:8081/assets/?unstable_path=a.ttf", first)
        assertEquals("http://localhost:8081/assets/?unstable_path=b.ttf", second)
    }

    @Test
    fun installFindsTheBundlesTheExportedPageLoads() {
        // Arrange
        val document = """
            <link rel="preload" href="/_expo/static/css/web-4b1e.css" as="style">
            <script src="/_expo/static/js/web/index-3f2a9c.js" defer></script>
            <script src="/_expo/static/js/web/index-3f2a9c.js" defer></script>
            <link rel="manifest" href="/manifest.json">
        """.trimIndent()

        // Act
        val paths = WorkerRequestRouter.buildOutputIn(document)

        // Assert
        assertEquals(
            listOf("/_expo/static/css/web-4b1e.css", "/_expo/static/js/web/index-3f2a9c.js"),
            paths,
        )
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
