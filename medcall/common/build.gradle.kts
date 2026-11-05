plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(21)

    jvm()

    js {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            // api: LocalDate and LocalTime are part of the shared DTOs. Ktor brings the server the
            // 0.8.0-0.6.x-compat variant, which differs only in Instant: use kotlin.time.Instant, not
            // the kotlinx-datetime one, so the same code runs with either.
            api(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
