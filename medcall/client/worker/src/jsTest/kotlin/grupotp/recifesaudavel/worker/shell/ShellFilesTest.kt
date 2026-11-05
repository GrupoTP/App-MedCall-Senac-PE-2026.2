package grupotp.recifesaudavel.worker.shell

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ShellFilesTest {
    @Test
    fun buildOutputKeepsTheQueryInTheCacheKey() {
        // Act
        val icon = ShellFiles.cacheKey("http://localhost:8081/icons/icon-512.png?v=1")
        val first = ShellFiles.cacheKey("http://localhost:8081/assets/?unstable_path=a.ttf")
        val second = ShellFiles.cacheKey("http://localhost:8081/assets/?unstable_path=b.ttf#top")

        // Assert
        assertEquals("/icons/icon-512.png", icon)
        assertEquals("http://localhost:8081/assets/?unstable_path=a.ttf", first)
        assertEquals("http://localhost:8081/assets/?unstable_path=b.ttf", second)
    }

    @Test
    fun serverAndHealthDataAreNeverInstalled() {
        // Act
        val serverPaths = ShellFiles.ESSENTIAL_PATHS.filter { it.startsWith("/api/") || it == "/healthz" }

        // Assert
        assertEquals(emptyList(), serverPaths)
        assertFalse(ShellFiles.isShellFile("/api/v1/auth/login"))
    }
}
