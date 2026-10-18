plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvmToolchain(21)

    js {
        browser {
            commonWebpackConfig {
                outputFileName = "app.js"
            }
            // Karma can't run the CommonJS bundle ("module is not defined"). Tests run on Node instead.
            testTask { enabled = false }
        }
        // Node only runs the unit tests. React Native and Expo modules are stubbed, see test-support/.
        nodejs {
            testTask {
                nodeJsArgs += listOf("--require", file("test-support/native-module-stubs.js").absolutePath)
            }
        }
        binaries.executable()
        useCommonJs()
    }

    sourceSets {
        jsMain.dependencies {
            implementation(projects.common)
            implementation(kotlinWrappers.js)
            implementation(kotlinWrappers.browser)
            implementation(kotlinWrappers.react)
            implementation(libs.grupotp.kotlin.react.native)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.js)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            // On JavaScript, kotlinx-datetime relies on @js-joda/core, which only knows UTC and the
            // device's own zone. This package adds the other zones, such as a clinic's "America/Recife"
            // (loaded in platform/TimeZones.kt). Pinned to 2.23.0: later releases require
            // @js-joda/core 5.7+, but kotlinx-datetime brings 3.2.
            implementation(npm("@js-joda/timezone", "2.23.0"))
        }
        jsTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}

val clientDirectory = rootProject.layout.projectDirectory.dir("client/expo")

// The Expo shell serves `public/` as-is on the web, next to `sw.js` and the manifest.
val syncServiceWorkerBundle = tasks.register<Copy>("syncServiceWorkerBundle") {
    dependsOn(":client:worker:jsBrowserProductionWebpack")
    from(project(":client:worker").layout.buildDirectory.dir("kotlin-webpack/js/productionExecutable"))
    include("worker.js")
    into(clientDirectory.dir("public"))
}

// Metro reads the Kotlin bundle from here on both Android and the web.
tasks.register("buildClientBundle") {
    group = "recife saudavel"
    description = "Builds the Kotlin UI and service worker bundles consumed by client/expo."
    dependsOn("jsBrowserProductionWebpack", syncServiceWorkerBundle)
}
