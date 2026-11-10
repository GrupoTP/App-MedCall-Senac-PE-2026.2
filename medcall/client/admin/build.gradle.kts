plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

// Portal da Clínica and Painel de Controle MedCall: one web app in Kotlin/JS with kotlin-react
// (react-dom, not React Native) and the AdminLTE 4 template. Served under /admin/ next to the PWA.
kotlin {
    jvmToolchain(21)

    js {
        browser {
            commonWebpackConfig {
                outputFileName = "admin.js"
                cssSupport { enabled.set(true) }
            }
            testTask { enabled = false }
        }
        binaries.executable()
        useCommonJs()
    }

    compilerOptions {
        optIn.add("kotlin.js.ExperimentalWasmJsInterop")
    }

    sourceSets {
        jsMain.dependencies {
            implementation(projects.common)
            implementation(kotlinWrappers.js)
            implementation(kotlinWrappers.browser)
            implementation(kotlinWrappers.react)
            implementation(kotlinWrappers.reactDom)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.js)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(npm("admin-lte", "4.10.0"))
            implementation(npm("bootstrap", "5.3.8"))
            implementation(npm("bootstrap-icons", "1.13.1"))
            implementation(npm("chart.js", "4.5.1"))
            // Loaders of the AdminLTE and icon stylesheets imported from Kotlin (ui/Theme.kt).
            implementation(devNpm("css-loader", "7.1.2"))
            implementation(devNpm("style-loader", "4.0.0"))
        }
    }
}
