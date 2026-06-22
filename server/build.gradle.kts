import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.application)
}

group = "com.example.proyectofinal"
version = "1.0.0"

application {
    mainClass.set("com.example.proyectofinal.MainKt")
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(project(":shared"))
    // Ktor
    val ktorVersion = libs.versions.ktor.get()
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")

    // Database
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.postgresql)

    // JWT
    implementation("com.auth0:java-jwt:4.4.0")

    // Password hashing
    implementation("at.favre.lib:bcrypt:0.10.2")

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.16")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")

    // Testing
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.serialization.kotlinx.json)
    testImplementation(libs.kotlin.test)
    // Flyway 10.x publishes a PostgreSQL database module but no matching flyway-database-h2 artifact.
    // H2 migration coverage stays explicit through the in-memory driver below and the server test suite.
    testImplementation(libs.h2)
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

tasks.withType<Tar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Zip> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
tasks.withType<Copy> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
tasks.withType<AbstractCopyTask> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
