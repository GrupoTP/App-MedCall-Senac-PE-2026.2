pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
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

includeBuild("grupotp-kotlin-react-native")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(
    "recife-saudavel-common",
    "recife-saudavel-server",
    "recife-saudavel-app-core",
    "recife-saudavel-app",
    "recife-saudavel-worker",
)
