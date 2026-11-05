pluginManagement {
    // Plugins of this repository (Dropbox flags, Ktor development mode).
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("recifesaudavel.dropbox-ignore")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("kotlinWrappers") {
            from("org.jetbrains.kotlin-wrappers:kotlin-wrappers-catalog:2026.9.3")
        }
    }
}

rootProject.name = "recife-saudavel"

// The submodule has to exist before Gradle reads the included build, so no task could clone it:
// the first build after `git clone` checks it out here. Later runs only check for the file.
val reactNativeWrappers = settingsDir.resolve("grupotp-kotlin-react-native")
if (!reactNativeWrappers.resolve("settings.gradle.kts").isFile) {
    logger.lifecycle("Submodule grupotp-kotlin-react-native missing, running git submodule update --init")
    val checkout = providers.exec {
        workingDir(settingsDir)
        commandLine("git", "submodule", "update", "--init", reactNativeWrappers.name)
        isIgnoreExitValue = true
    }
    if (checkout.result.get().exitValue != 0) {
        throw GradleException("git submodule update --init failed:\n" + checkout.standardError.asText.get())
    }
}
includeBuild(reactNativeWrappers)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":common", ":server", ":client:app", ":client:worker", ":client:expo")
