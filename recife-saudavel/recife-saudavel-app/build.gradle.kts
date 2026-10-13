plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvmToolchain(21)

    js {
        browser {
            commonWebpackConfig {
                outputFileName = "recife-saudavel-app.js"
            }
        }
        binaries.executable()
        useCommonJs()
    }

    sourceSets {
        jsMain.dependencies {
            implementation(projects.recifeSaudavelAppCore)
            implementation(kotlinWrappers.js)
            implementation(kotlinWrappers.browser)
            implementation(kotlinWrappers.react)
            implementation(libs.grupotp.kotlin.react.native)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

val clientDirectory = rootProject.layout.projectDirectory.dir("recife-saudavel-client")

// The Expo shell serves `public/` as-is on the web, next to `sw.js` and the manifest.
val syncServiceWorkerBundle = tasks.register<Copy>("syncServiceWorkerBundle") {
    dependsOn(":recife-saudavel-worker:jsBrowserProductionWebpack")
    from(project(":recife-saudavel-worker").layout.buildDirectory.dir("kotlin-webpack/js/productionExecutable"))
    include("recife-saudavel-worker.js")
    into(clientDirectory.dir("public"))
}

// Metro reads the Kotlin bundle from here on both Android and the web.
tasks.register("buildClientBundle") {
    group = "recife saudavel"
    description = "Builds the Kotlin UI and service worker bundles consumed by recife-saudavel-client."
    dependsOn("jsBrowserProductionWebpack", syncServiceWorkerBundle)
}
