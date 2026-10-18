plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    js {
        browser {
            commonWebpackConfig {
                outputFileName = "worker.js"
            }
        }
        nodejs()
        binaries.executable()
    }

    compilerOptions {
        // JsString and toJsString(), which the browser wrappers use for IndexedDB and URL values.
        optIn.add("kotlin.js.ExperimentalWasmJsInterop")
    }

    sourceSets {
        jsMain.dependencies {
            implementation(projects.common)
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
