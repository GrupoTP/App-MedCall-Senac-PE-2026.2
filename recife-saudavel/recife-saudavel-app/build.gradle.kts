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
        binaries.executable()
    }

    sourceSets {
        jsMain.dependencies {
            implementation(projects.recifeSaudavelCommon)
            implementation(libs.kotlin.react)
            implementation(libs.kotlin.react.dom)
        }
    }
}
