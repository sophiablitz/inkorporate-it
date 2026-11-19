
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(ktorLibs.plugins.ktor)
}

group = "it.inkorporate"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "it.inkorporate.MainKt"
}

kotlin {
    jvmToolchain(21)
}
dependencies {
    implementation(project(":shared"))
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.websockets)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.logback.classic)

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
    testImplementation(ktorLibs.client.websockets)
}
