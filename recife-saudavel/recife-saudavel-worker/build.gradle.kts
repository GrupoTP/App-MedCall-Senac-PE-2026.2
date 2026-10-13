plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    js {
        browser {
            commonWebpackConfig {
                outputFileName = "recife-saudavel-worker.js"
            }
        }
        nodejs()
        binaries.executable()
    }

    sourceSets {
        jsMain.dependencies {
            implementation(projects.recifeSaudavelCommon)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(kotlinWrappers.js)
            implementation(kotlinWrappers.browser)
        }
        jsTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
