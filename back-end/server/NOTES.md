# inklings-server — stroke relay

A minimal Ktor server that relays drawing strokes between Inklings apps.
Whatever one connected client sends to `/relay`, every *other* connected client receives.
Strokes now **stream live** (begin → points → end) so screens watch a drawing appear as
it happens, and the server keeps the current canvas so a client that joins late is caught
up with a snapshot. See "Live-streaming protocol" below.

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
    connected, shared by all connections. On connect: add self AND send a snapshot of the
    current canvas. Each text frame: update canvas state, then send to every session except self.
    `finally`: remove self (runs on errors too).
- `src/main/kotlin/CanvasState.kt` — the server's view of the drawing, kept so late joiners
  can be caught up. It reads only `type` and `id` from each frame and otherwise stores the raw
  JSON, reassembling whole strokes for the snapshot. No JSON library (small, flat frames).

### Live-streaming protocol
JSON text frames, one message per frame, each with a `type`:
- begin  — `{"type":"begin","id":"s1","brush":"Marker","color":<ARGB int>,"size":5.0,"epsilon":0.1}`
- points — `{"type":"points","id":"s1","points":[{"x":..,"y":..,"t":<ms>,"p":<pressure>}, ...]}` (sent repeatedly while drawing)
- end    — `{"type":"end","id":"s1"}`
- clear  — `{"type":"clear"}` (wipes everyone's canvas)
- snapshot — server → a newly connected client: `{"type":"snapshot","strokes":[<full stroke>, ...]}`

A snapshot stroke has the begin fields plus the accumulated `points`, i.e. the same shape
as the legacy single-frame stroke, so clients render snapshot and live strokes with one path.
`brush` is the toolbar label (`stockBrushes` in `DrawingToolbar.kt`).

**Back-compat:** a frame with no `type` but with `points` is treated as one complete stroke
(the old single-frame protocol). The app still sends this for stylus/eraser input, which the
finger-only live-streaming path doesn't cover.

### Coordinates are NOT normalized (yet)
Points are raw screen pixels and are rendered as-is. If two screens have different pixel
dimensions, strokes land in the wrong place. The Relay Pad is sized 1080×2170 to match the
emulator's pixels as a workaround. Normalizing to a device-independent space is a planned change.

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

## Next
- **Normalize coordinates** so different screen sizes line up (device-independent space +
  scale on render; also scale brush `size`). This is the next planned change.
- Live-stream **stylus/eraser** input too (currently only finger strokes stream live).
- Rooms (`/room/{id}`), typed messages (`sealed class` + kotlinx.serialization),
  first game rule (only the drawer may draw), deploy (fat JAR / Docker, then `wss://`).

## Gotchas
- **Terminal Gradle needs a full JDK 21**, not a JRE. If `./gradlew run` complains, point `JAVA_HOME` at a JDK
  (IntelliJ's downloaded JDKs live in `~/.jdks`), or just run from IntelliJ.
- Run on another port: `./gradlew run --args="-port=8081"`.
- Emulator won't boot or freezes on a black screen? Use software graphics:
  Device Manager → Edit → Show Advanced Settings → Graphics: Software,
  or `emulator -avd <name> -gpu swiftshader_indirect`.
