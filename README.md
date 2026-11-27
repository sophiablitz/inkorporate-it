# inkorporate-it

A multiplayer drawing and guessing game: an Android app (`front-end/`), a Ktor server (`back-end/`), and code they share (`shared/`).

It's all one Gradle build. Open this top-level folder in Android Studio, and run every command from here. See [CONTRIBUTING.md](CONTRIBUTING.md) for setup and how to make a change.

| Module | Folder | What it is |
| --- | --- | --- |
| `:app` | `front-end/app` | The Android app |
| `:server` | `back-end/server` | The game server |
| `:shared` | `shared` | Plain Kotlin code both sides use, like the protocol messages |

## Run the app

The app connects to our server on Render (`wss://inkorporate-it.onrender.com/ws`), so for front-end work you don't need to run the server yourself.

1. Open this top-level folder in Android Studio and let Gradle sync.
2. Start the emulator and press **Run** in Android Studio, with the `app` configuration selected.

## Run the server on your computer

Do this when you want to try server changes before they're merged and deployed.

1. Start the server from a **separate terminal**, in this folder:

   ```bash
   ./gradlew :server:run
   ```

   Wait for `Responding at http://127.0.0.1:8080`. To check it, open http://localhost:8080/health in a browser. It should say `ok`.

   Don't start the server from inside Android Studio: it keeps Android Studio's Gradle busy for as long as it runs, so building the app waits behind it.
2. Point your app at your computer: add this line to `local.properties` in this folder (Android Studio made that file for you), then click **Sync Now**:

   ```properties
   server.local=true
   ```

   The app then connects to `ws://10.0.2.2:8080/ws`. `10.0.2.2` is how the emulator reaches your computer (`localhost` inside the emulator is the emulator itself). `local.properties` is gitignored, so this only changes your own builds. Delete the line and sync again to go back to Render.
3. Run the app as above.
4. To stop the server, press Ctrl+C in its terminal.

## Common commands

| To | Run |
| --- | --- |
| Run the server | `./gradlew :server:run` |
| Install the app on the running emulator | `./gradlew :app:installDebug` (or press Run in Android Studio) |
| Run all the checks, like GitHub does on a pull request | `scripts/test.sh` |
| Run only one part's checks | `scripts/test.sh front-end`, `scripts/test.sh back-end` or `scripts/test.sh shared` |
| Build and test everything | `./gradlew build` |

In PowerShell or cmd, use `.\gradlew` or `gradlew.bat` instead of `./gradlew`.
