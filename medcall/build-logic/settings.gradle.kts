// Gradle plugins of this repository: the root settings and the modules apply them by id.
// An included build instead of buildSrc, because buildSrc only exists after settings.gradle.kts
// has run, too late for the settings plugin.
dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
    }
}

rootProject.name = "build-logic"
