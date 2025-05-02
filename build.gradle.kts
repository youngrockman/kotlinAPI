
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "com.example"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
        implementation("io.ktor:ktor-server-core-jvm:2.3.7")
        implementation("io.ktor:ktor-server-netty-jvm:2.3.7")
        implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
        implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
        implementation("io.ktor:ktor-server-auth:2.3.7")
        implementation("io.ktor:ktor-server-auth-jwt:2.3.7")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
        implementation(libs.ktor.server.content.negotiation)
        implementation(libs.ktor.server.core)
        implementation(libs.ktor.serialization.kotlinx.json)
        implementation(libs.ktor.server.resources)
        implementation(libs.ktor.server.auth)
        implementation(libs.ktor.server.auth.jwt)
        implementation(libs.ktor.server.netty)
        implementation(libs.logback.classic)
        implementation(libs.ktor.server.config.yaml)
        testImplementation(libs.ktor.server.test.host)
        testImplementation(libs.kotlin.test.junit)
}
