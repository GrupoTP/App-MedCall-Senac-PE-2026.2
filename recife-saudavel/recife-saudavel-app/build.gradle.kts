plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvmToolchain(21)

    js {
        browser {
            commonWebpackConfig {
                outputFileName = "recife-saudavel.js"
                devServer = (devServer ?: org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.DevServer()).apply {
                    port = 8081
                }
            }
        }
        nodejs()
        binaries.executable()
    }

    sourceSets {
        jsMain.dependencies {
            implementation(projects.recifeSaudavelCommon)
            implementation(libs.kotlin.react)
            implementation(libs.kotlin.react.dom)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }
        jsTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

val syncServiceWorkerBundle = tasks.register<Copy>("syncServiceWorkerBundle") {
    dependsOn(":recife-saudavel-worker:jsBrowserProductionWebpack")
    from(project(":recife-saudavel-worker").layout.buildDirectory.dir("kotlin-webpack/js/productionExecutable"))
    include("recife-saudavel-worker.js")
    into(layout.buildDirectory.dir("processedResources/js/main"))
    doLast {
        copy {
            from(project(":recife-saudavel-worker").layout.buildDirectory.dir("kotlin-webpack/js/productionExecutable"))
            include("recife-saudavel-worker.js")
            into(rootProject.layout.buildDirectory.dir("js/packages/recife-saudavel-recife-saudavel-app/kotlin"))
        }
    }
}

tasks.named("jsBrowserDevelopmentRun") {
    dependsOn(syncServiceWorkerBundle)
}

syncServiceWorkerBundle.configure {
    mustRunAfter("jsProcessResources")
}
