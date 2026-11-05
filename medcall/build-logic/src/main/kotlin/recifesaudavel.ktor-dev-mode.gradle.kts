// Project plugin `recifesaudavel.ktor-dev-mode`, applied by server/build.gradle.kts.
// Adds `runDev`: the server in Ktor development mode, recompiled by ServerWatcher and reloaded on change.
import grupotp.recifesaudavel.buildlogic.ServerWatcher

plugins {
    application
}

val serverWatcher = gradle.sharedServices.registerIfAbsent("serverWatcher", ServerWatcher::class) {
    parameters.rootDir = rootProject.layout.projectDirectory
}

tasks.register<JavaExec>("runDev") {
    group = "recife saudavel"
    description = "Runs the server in Ktor development mode, recompiling and reloading it on change."
    usesService(serverWatcher)
    mainClass = application.mainClass
    // Ktor only reloads classes from folders, so :common comes in as its compiled classes
    // instead of common-jvm.jar; ktor.deployment.watch matches both build/classes folders.
    classpath = sourceSets.main.get().runtimeClasspath.filter { it.name != "common-jvm.jar" } +
        files(project(":common").layout.buildDirectory.dir("classes/kotlin/jvm/main")) {
            builtBy(":common:compileKotlinJvm")
        }
    workingDir = projectDir
    // Ktor reloads the modules from the folders listed in ktor.deployment.watch (application.yaml).
    jvmArgs("-Dio.ktor.development=true")
    val watcher = serverWatcher
    doFirst { watcher.get().start() }
}
