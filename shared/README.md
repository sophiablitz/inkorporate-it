All business logic and data models shared across front and back end.

This is the `:shared` module: a plain Kotlin library that both the app (`:app`) and the server (`:server`) depend on. Keep it free of Android and server code, and use only `kotlinx.*` libraries here. kotlinx.serialization is already set up, so `@Serializable` classes work.

Run its tests from the top-level folder with `./gradlew :shared:check` or `scripts/test.sh shared`.
