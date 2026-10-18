plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    id("recifesaudavel.ktor-dev-mode")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "grupotp.recifesaudavel.server.ApplicationKt"
}

dependencies {
    implementation(projects.common)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.logback.classic)
    implementation(libs.ktor.health.check)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.h2)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit5)
}

// Local runs and tests sign session tokens with a public development key unless JWT_SECRET is set.
// A deployed server starts from the distribution, where JWT_SECRET is required.
val jwtSecret = providers.environmentVariable("JWT_SECRET").orElse("recife-saudavel-development-only-jwt-secret")

tasks.withType<JavaExec>().configureEach {
    environment("JWT_SECRET", jwtSecret.get())
}

tasks.test {
    useJUnitPlatform()
    environment("JWT_SECRET", jwtSecret.get())
}
