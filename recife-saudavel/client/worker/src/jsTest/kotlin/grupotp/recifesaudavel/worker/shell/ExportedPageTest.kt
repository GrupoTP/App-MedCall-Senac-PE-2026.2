package grupotp.recifesaudavel.worker.shell

import kotlin.test.Test
import kotlin.test.assertEquals

class ExportedPageTest {
    @Test
    fun findsTheBundlesThePageLoadsOnce() {
        // Arrange
        val page = ExportedPage(
            """
            <link rel="preload" href="/_expo/static/css/web-4b1e.css" as="style">
            <script src="/_expo/static/js/web/index-3f2a9c.js" defer></script>
            <script src="/_expo/static/js/web/index-3f2a9c.js" defer></script>
            <link rel="manifest" href="/manifest.json">
            """.trimIndent(),
        )

        // Act
        val paths = page.bundlePaths()

        // Assert
        assertEquals(
            listOf("/_expo/static/css/web-4b1e.css", "/_expo/static/js/web/index-3f2a9c.js"),
            paths,
        )
    }
}
