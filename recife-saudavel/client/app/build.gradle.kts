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
        }
        binaries.executable()
        useCommonJs()
    }

    sourceSets {
        jsMain.dependencies {
            implementation(projects.client.appCore)
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
