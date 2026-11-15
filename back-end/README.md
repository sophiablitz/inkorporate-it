# back-end

The Ktor game server lives in `server/`.

## Run it

```bash
cd server
./gradlew run
```

On Windows without Git Bash, use `gradlew.bat run`.

The server listens on port 8080. To check that it's up, open http://localhost:8080/health. It should say `ok`.

From the Android emulator, use http://10.0.2.2:8080/health instead. Inside the emulator, `localhost` is the emulator itself.

## Test it

```bash
cd server
./gradlew test
```
