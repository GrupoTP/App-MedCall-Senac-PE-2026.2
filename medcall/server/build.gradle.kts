plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    id("medcall.ktor-dev-mode")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "grupotp.medcall.server.ApplicationKt"
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
    implementation(libs.exposed.dao)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.h2)
    // Webhook events go through RabbitMQ when RABBITMQ_URL is set (integration/EventBus.kt).
    implementation(libs.amqp.client)
    // MySQL in production (DATABASE_DRIVER=com.mysql.cj.jdbc.Driver). H2 stays the default for development and tests.
    runtimeOnly(libs.mysql.connector.j)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit5)
}

// Local runs and tests sign session tokens with a public development key unless JWT_SECRET is set.
// A deployed server starts from the distribution, where JWT_SECRET is required.
val jwtSecret = providers.environmentVariable("JWT_SECRET").orElse("medcall-development-only-jwt-secret")

// The first MedCall panel administrator of a local run. A deployed server creates one only when
// MEDCALL_ADMIN_EMAIL and MEDCALL_ADMIN_PASSWORD are set.
val adminEmail = providers.environmentVariable("MEDCALL_ADMIN_EMAIL").orElse("admin@medcall.dev")
val adminPassword = providers.environmentVariable("MEDCALL_ADMIN_PASSWORD").orElse("medcall-dev-admin")

tasks.withType<JavaExec>().configureEach {
    environment("JWT_SECRET", jwtSecret.get())
    environment("MEDCALL_ADMIN_EMAIL", adminEmail.get())
    environment("MEDCALL_ADMIN_PASSWORD", adminPassword.get())
    // Local runs start with the fictitious data of the dashboards (docs/base-sintetica.md).
    environment("DEMO_SEED", providers.environmentVariable("DEMO_SEED").orElse("true").get())
    environment("DEMO_STAFF_PASSWORD", providers.environmentVariable("DEMO_STAFF_PASSWORD").orElse("medcall-demo").get())
    // Local runs may post webhooks to a receiver on this machine (http://localhost:...).
    environment("WEBHOOKS_ALLOW_PRIVATE", providers.environmentVariable("WEBHOOKS_ALLOW_PRIVATE").orElse("true").get())
}

tasks.test {
    useJUnitPlatform()
    environment("JWT_SECRET", jwtSecret.get())
    environment("MEDCALL_ADMIN_EMAIL", "admin@medcall.test")
    environment("MEDCALL_ADMIN_PASSWORD", "medcall-test-admin")
    // Route tests load application.yaml; an in-memory database keeps them off the development data.
    environment("DATABASE_JDBC_URL", "jdbc:h2:mem:route-tests;DB_CLOSE_DELAY=-1")
}
