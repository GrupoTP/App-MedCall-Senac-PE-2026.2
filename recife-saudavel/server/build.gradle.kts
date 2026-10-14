import kotlin.concurrent.thread

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "grupotp.recifesaudavel.server.ApplicationKt"
}

dependencies {
    implementation(projects.common)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.cors)
    implementation(libs.logback.classic)
    implementation(libs.ktor.health.check)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.h2)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit5)
}

tasks.test {
    useJUnitPlatform()
}

// Recompiles the server while :server:runDev runs: a second Gradle build in continuous mode
// (`gradlew -t :server:classes`), so the server's own build stays free to run it. Gradle closes
// the service when the build ends, also on Ctrl+C, and that stops the watcher with it.
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

val serverWatcher = gradle.sharedServices.registerIfAbsent("serverWatcher", ServerWatcher::class) {
    parameters.rootDir = rootProject.layout.projectDirectory
}

tasks.register<JavaExec>("runDev") {
    group = "recife saudavel"
    description = "Runs the server in Ktor development mode, recompiling and reloading it on change."
    usesService(serverWatcher)
    mainClass = application.mainClass
    classpath = sourceSets.main.get().runtimeClasspath
    workingDir = projectDir
    // Ktor reloads the modules from the folders listed in ktor.deployment.watch (application.yaml).
    jvmArgs("-Dio.ktor.development=true")
    val watcher = serverWatcher
    doFirst { watcher.get().start() }
}
