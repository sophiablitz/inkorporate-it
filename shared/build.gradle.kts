// shared/: plain Kotlin code that both the app and the server use (#16), like the
// protocol messages. No Android or server code here, only kotlinx.* libraries.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        // The app compiles for Java 11, so shared/ has to as well, or the app can't use it.
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    // api, not implementation: the app and server need kotlinx.serialization too,
    // to encode and decode the @Serializable classes that live here.
    api(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test"))
}
