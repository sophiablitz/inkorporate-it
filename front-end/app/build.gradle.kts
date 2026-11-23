import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization) // type-safe Navigation Compose routes (AppRoute in MainActivity.kt)
}

// Which game server the app connects to (#151).
// Our server on Render, unless you put server.local=true in your local.properties:
// then debug builds use the server on your own computer (start it with ./gradlew :server:run).
// local.properties is gitignored, so this only changes your own builds. Release builds always use Render.
val renderServerUrl = "wss://inkorporate-it.onrender.com/ws"
val localServerUrl = "ws://10.0.2.2:8080/ws" // 10.0.2.2 is the computer running the emulator
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val useLocalServer = localProperties.getProperty("server.local") == "true"

android {
    namespace = "com.example.inkorporate_it"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.inkorporate_it"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            val url = if (useLocalServer) localServerUrl else renderServerUrl
            buildConfigField("String", "SERVER_URL", "\"$url\"")
        }
        release {
            buildConfigField("String", "SERVER_URL", "\"$renderServerUrl\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        // Generates BuildConfig, which holds SERVER_URL (see buildTypes above).
        buildConfig = true
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Ktor's WebSocket client, from the same Ktor catalog the server uses.
    implementation(ktorLibs.client.okhttp)
    implementation(ktorLibs.client.websockets)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}