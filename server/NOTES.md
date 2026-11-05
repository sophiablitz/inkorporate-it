# inklings-server — stroke relay

A minimal Ktor server that relays drawing strokes between Inklings apps.
Whatever one connected client sends to `/relay`, every *other* connected client receives.
The server never reads the message, so the JSON strokes pass through unchanged.

This folder is its **own Gradle build** (own wrapper, own versions: Ktor 3.6.1, Kotlin from
`gradle/libs.versions.toml`). It is *not* part of the Android app's build, so the two can use different versions.

## Quick start
1. **Server:** open this `server/` folder in IntelliJ IDEA (File → Open → `server`), let Gradle sync,
   then run `main` in `src/main/kotlin/main.kt`. It listens on **port 8080**.
   Check: http://localhost:8080 shows "Hello, World!".
2. **App:** open the repo root in Android Studio, run the app on an emulator or a USB-connected phone.
3. **For each device**, after it connects and before opening the app, tunnel its port 8080 to your PC:
   ```
   adb reverse tcp:8080 tcp:8080
   ```
   With several devices: `adb devices` for the ids, then `adb -s <id> reverse tcp:8080 tcp:8080`.
4. Draw on one device → it appears on the other(s).

## ⚠️ The app only reaches the server through adb
- The app connects to `ws://localhost:8080/relay` (`app/.../net/StrokeRelay.kt`).
  `localhost` on a device means *that device*, so the `adb reverse` tunnel above is required —
  on emulators and USB phones alike.
- The tunnel disappears when the device disconnects, the emulator restarts, or adb restarts → run it again.
  Symptom when missing: Logcat `StrokeRelay: connection to ws://localhost:8080/relay failed`.
- The app connects once, when the screen opens. Started the server late? Restart the app.
- Depends on the server being on port **8080** (`src/main/resources/application.yaml`).
  Different port → change the URL in `StrokeRelay.kt`, or map it: `adb reverse tcp:8080 tcp:<port>`.
- Tools on your PC (browser, Relay Pad, scripts) use `localhost:8080` directly — no adb needed.

## Testing without a second phone
- **Relay Pad** — open `tools/relay-pad.html` in a browser while the server runs. It acts as a second
  phone (1080×2170 canvas, same pixel coordinates as a 1080×2400 phone): strokes drawn in the app show up there,
  and strokes drawn there show up in the app. It draws plain lines, not ink, so brushes look simpler.
- **Browser console** — at http://localhost:8080, press F12 → Console:
  ```js
  let last; const ws = new WebSocket("ws://localhost:8080/relay"); ws.onmessage = e => { last = e.data; console.log(e.data) }
  ```
  Draw in the app → its JSON prints. Send a red, shifted copy back:
  ```js
  const s = JSON.parse(last); s.color = 0xFFE53935 | 0; s.points.forEach(p => p.x += 200); ws.send(JSON.stringify(s))
  ```
- Two browser tabs running the first line can also chat: `ws.send("hello")` in one → appears in the other.

## How it works
- `src/main/kotlin/Websockets.kt` — installs the WebSockets plugin (pings every 15 s, 15 s timeout).
- `src/main/kotlin/Routing.kt`
  - `GET /` — "Hello, World!" health check.
  - `/ws` — echo: replies to the sender only (learning step).
  - `/relay` — broadcast. `sessions` is a thread-safe set (`ConcurrentHashMap.newKeySet()`) of everyone
    connected, shared by all connections. On connect: add self. Each text frame: send to every session except self.
    `finally`: remove self (runs on errors too).
- Stroke JSON (built in the app's `StrokeRelay.kt`):
  `{"brush":"Marker","color":<ARGB int>,"size":5.0,"epsilon":0.1,"points":[{"x":..,"y":..,"t":<ms>,"p":<pressure, stylus only>}]}`.
  `brush` is the toolbar label (`stockBrushes` in `DrawingToolbar.kt`).

## Tested (2026-11-04)
- Two Node clients on `/relay`: messages reach the other client, not the sender.
- Emulator ↔ Node client: app's stroke arrives as JSON; JSON stroke sent in appears in the app.
- Emulator ↔ Relay Pad: both directions.
- Galaxy S24 over USB ↔ emulator: strokes cross both ways, rebuilt by ink, no dropped or failed strokes.

## Known limits
- Coordinates are raw screen pixels → different screen sizes put strokes in the wrong place
  (same root cause as the app's rotation bug).
- JSON only carries x, y, time, pressure (no tilt/orientation), so real-finger strokes can look different on the other device.
- Dense vertical streaks seen on the S24 with Pressure Pen are drawn locally by the phone, not by the server — to investigate.
- Phone in dark mode → dark canvas, black ink hard to see.
- No reconnect; "Clear" only clears your own screen; plain `ws://` (cleartext allowed in the app manifest) — switch to `wss://` when deployed.

## Next (from the Ktor learning path)
Rooms (`/room/{id}`), typed messages (`sealed class` + kotlinx.serialization), first game rule (only the drawer may draw),
deploy (fat JAR / Docker, then `wss://`).

## Gotchas
- **Terminal Gradle needs a full JDK 21**, not a JRE. If `./gradlew run` complains, point `JAVA_HOME` at a JDK
  (IntelliJ's downloaded JDKs live in `~/.jdks`), or just run from IntelliJ.
- Run on another port: `./gradlew run --args="-port=8081"`.
- Emulator won't boot or freezes on a black screen? Use software graphics:
  Device Manager → Edit → Show Advanced Settings → Graphics: Software,
  or `emulator -avd <name> -gpu swiftshader_indirect`.
