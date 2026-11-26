# Builds the game server so Render can run it.
#
# Render can't build Kotlin by itself, so it follows these steps instead.
# This file is at the repo root because the server build needs the root Gradle files,
# back-end/ and shared/. Gradle also reads front-end/app's build file, but never builds the app,
# so no Android SDK is needed.

# Step 1: build the server. This uses an image with the Java 21 JDK, which the build asks for.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /repo
COPY . .
RUN ./gradlew :server:buildFatJar --no-daemon --console=plain

# Step 2: the image Render actually runs. Only Java and the server jar, no build tools.
FROM eclipse-temurin:21-jre
COPY --from=build /repo/back-end/server/build/libs/server-all.jar /app/server.jar
# Our Render plan has 512 MB of memory. Let Java use up to 75% of it.
CMD ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/server.jar"]
