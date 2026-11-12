plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(ktorLibs.plugins.ktor)
}

group = "it.inkorporate"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
    // The message protocol lives in ../shared so the app and the server
    // compile the exact same files.
    sourceSets["main"].kotlin.srcDir("../shared/src/main/kotlin")
}

dependencies {
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.websockets)
    implementation(libs.logback.classic)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}
