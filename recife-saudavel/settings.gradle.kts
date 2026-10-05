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
}

rootProject.name = "recife-saudavel"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(
    "recife-saudavel-common",
    "recife-saudavel-server",
    "recife-saudavel-app",
)
