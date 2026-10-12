plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(21)

    js {
        browser {
            commonWebpackConfig {
                outputFileName = "recife-saudavel-ui.js"
            }
        }
        binaries.executable()
        useCommonJs()
    }

    sourceSets {
        jsMain.dependencies {
            implementation(projects.common)
            implementation(kotlinWrappers.react)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ojaynico.kotlin.react.native)
        }
    }
}
