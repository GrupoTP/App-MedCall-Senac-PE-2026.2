package grupotp.recifesaudavel.buildlogic

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logging
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import kotlin.concurrent.thread

/**
 * Recompiles the server while `:server:runDev` runs: a second Gradle build in continuous mode
 * (`gradlew -t :server:classes`), so the server's own build stays free to run it. Gradle closes
 * the service when the build ends, also on Ctrl+C, and that stops the watcher with it.
 */
abstract class ServerWatcher : BuildService<ServerWatcher.Params>, AutoCloseable {
    interface Params : BuildServiceParameters {
        val rootDir: DirectoryProperty
    }

    private val logger = Logging.getLogger(ServerWatcher::class.java)
    private var process: Process? = null

    fun start() {
        val rootDir = parameters.rootDir.get().asFile
        val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
        val gradlew = if (isWindows) {
            listOf("cmd", "/c", rootDir.resolve("gradlew.bat").absolutePath)
        } else {
            listOf(rootDir.resolve("gradlew").absolutePath)
        }
        val started = ProcessBuilder(gradlew + listOf("-t", "-q", "--console=plain", ":server:classes"))
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
        process = started
        // The daemon's own stdout never reaches the terminal; the logger does.
        thread(isDaemon = true, name = "server-watcher-output") {
            started.inputStream.bufferedReader().forEachLine { logger.lifecycle("[watch] $it") }
        }
    }

    override fun close() {
        val running = process ?: return
        running.toHandle().descendants().forEach { it.destroyForcibly() }
        running.destroyForcibly()
    }
}
