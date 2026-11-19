// The one Gradle build for the whole repo (#16): the Android app, the server and shared/.
// Run everything from this folder, for example ./gradlew :server:run or ./gradlew :app:installDebug.
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        // Ktor's own catalog, used by the server (ktorLibs.server.core and so on).
        create("ktorLibs").from("io.ktor:ktor-version-catalog:3.6.1")
    }
}

rootProject.name = "inkorporate-it"

// The modules keep their folders. Only their names are new.
include(":app")
project(":app").projectDir = file("front-end/app")

include(":server")
project(":server").projectDir = file("back-end/server")

include(":shared")
