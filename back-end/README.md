# back-end

The Ktor game server lives in `server/`. It's the `:server` module of the one Gradle build at the top of the repo, so run these commands from the top-level `inkorporate-it` folder, not from here.

## Run it

```bash
./gradlew :server:run
```

In PowerShell or cmd, use `.\gradlew :server:run`.

The server listens on port 8080. To check that it's up, open http://localhost:8080/health. It should say `ok`.

From the Android emulator, use http://10.0.2.2:8080/health instead. Inside the emulator, `localhost` is the emulator itself.

## Test it

```bash
./gradlew :server:check
```

Or `scripts/test.sh back-end`, which is what the pre-push hook and GitHub run.
