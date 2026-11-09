# Inklings — setup & run

Inklings is a live collaborative drawing demo: an Android app streams strokes, as
they're drawn, to a small relay server, and every other connected screen draws them in
real time. New screens that join mid‑session get caught up with the current canvas.

This is a **monorepo** with two parts that run together:

| Part | Path | What it is | Needs |
|------|------|------------|-------|
| App | `app/` | Android app (Kotlin + Compose + AndroidX Ink) | Android Studio / Android SDK 36 |
| Server | `server/` | Ktor WebSocket relay on port **8080** | JDK 21 (auto-downloaded on first run) |

There's also a browser-based **Relay Pad** (`server/tools/relay-pad.html`) that acts as a
second screen, so you can test without a second phone.

---

## TL;DR (macOS / Linux)

```bash
git clone <THIS_REPO_URL> inklings
cd inklings
./scripts/setup.sh            # one-time: finds your SDK, writes local.properties
./scripts/run-server.sh       # terminal 1: starts the relay on :8080 (leave running)
./scripts/run-app.sh          # terminal 2: builds, installs, tunnels, launches the app
open server/tools/relay-pad.html   # a second "screen" in your browser
```

Draw on the phone → it appears on the Relay Pad, and vice versa.

> First `run-server.sh` and first app build each download tooling (Gradle, a JDK, SDK
> bits). That's slow once, then fast. It is not frozen.

---

## Prerequisites

1. **Android Studio** (latest stable). Installing it gives you the Android SDK and
   `adb`. In **SDK Manager**, make sure **Android SDK Platform 36** and **Android SDK
   Platform-Tools** are installed.
2. **A device**: an emulator (create one in Device Manager) or a USB phone with
   **USB debugging** enabled.
3. **JDK 21** for the server. If you don't have it, the server's Gradle wrapper will
   download a matching toolchain on first run; installing [Temurin 21](https://adoptium.net/)
   is more reliable.
4. **Git**, and a browser for the Relay Pad.

No Node.js is needed for the app or server. (It's only used by the optional test
scripts in `server/tools/`.)

---

## One-time setup

```bash
./scripts/setup.sh
```

This:
- finds your Android SDK (or respects `ANDROID_HOME`) and writes `local.properties`
  so Gradle can build the app — **this is the #1 thing people miss**;
- locates `adb` and tells you how to add it to your PATH;
- checks for a JDK.

`local.properties` is intentionally **not** committed (it points at *your* SDK). The
script regenerates it on any machine.

**Prefer Android Studio?** Just open the repo root in Android Studio and let it sync —
it writes `local.properties` for you. You still need the server and the tunnel (below).

---

## Running

You need three things alive at once: the **server**, the **app**, and an **adb tunnel**
between them.

### 1. Server (terminal 1)

```bash
./scripts/run-server.sh
```
Leave it running. Health check: open <http://localhost:8080> — it shows `Hello, World!`.

### 2. App (terminal 2)

Either:
```bash
./scripts/run-app.sh      # builds a FRESH apk, installs, tunnels, launches
```
or run it from **Android Studio** (open the repo root, pick your device, press Run).

> If you use Android Studio, still set up the tunnel in step 3.

### 3. The tunnel (why the app needs it)

The app connects to `ws://localhost:8080/relay`. On a device, `localhost` means the
*device itself*, so each device needs a reverse tunnel to your computer:

```bash
./scripts/tunnel.sh              # all connected devices
./scripts/tunnel.sh <serial>     # one device (see: adb devices)
```

`run-app.sh` does this automatically. Re-run `tunnel.sh` whenever a device reconnects,
the emulator restarts, or adb restarts — the tunnel is not permanent.

The **Relay Pad in your browser does NOT need the tunnel** — it talks to `localhost:8080`
directly.

### 4. Test it

- Open `server/tools/relay-pad.html` in a browser (it shows **connected**).
- Draw on the phone → strokes stream onto the Relay Pad live.
- Draw on the Relay Pad → strokes appear on the phone.
- A screen that joins late is caught up with whatever's already on the canvas.

---

## Multiple screens

The relay fans out to everyone connected, so you can run several at once:
- more emulators/phones (each needs its own `adb reverse` — `tunnel.sh` handles all
  attached devices), and
- multiple browser tabs of the Relay Pad.

Everything shares one global canvas. (Per-room sessions are a planned next step.)

---

## Troubleshooting

**Build fails: `SDK location not found`**
Run `./scripts/setup.sh` (writes `local.properties`), or open the project once in
Android Studio.

**`zsh: command not found: adb`**
`adb` isn't on your PATH. Use the scripts (they find it), or add to your shell profile:
```bash
export PATH="$HOME/Library/Android/sdk/platform-tools:$PATH"
```

**App draws locally but nothing relays / `StrokeRelay: connection to … failed`**
The tunnel is missing or the app started before it existed. Run `./scripts/tunnel.sh`,
then restart the app (it connects once, when the drawing screen opens; there's no
auto-reconnect).

**App runs but behaves like old code**
A stale APK. Reinstall fresh:
```bash
adb uninstall com.blitz.inklings
./scripts/run-app.sh
```

**Server looks frozen on first run**
It's downloading Gradle and a JDK 21 toolchain. Let it finish once; later runs are fast.

**`./gradlew run` complains about the JDK**
Point `JAVA_HOME` at a JDK 21, or install Temurin 21. (The wrapper also tries to fetch a
toolchain automatically.)

**Confirm the app connected**
```bash
adb logcat -s StrokeRelay      # look for: connected to ws://localhost:8080/relay
```

**Emulator won't boot / black screen**
Device Manager → Edit → Show Advanced Settings → Graphics: **Software**.

---

## Developer notes

- Relay protocol, server internals, and testing without a phone are documented in
  [`server/NOTES.md`](server/NOTES.md).
- Optional protocol tests (need Node.js), run from `server/`:
  ```bash
  node tools/relay-test.js      # verifies live streaming + late-joiner snapshot
  node tools/relay-listen.js    # prints frames crossing the relay while you draw
  ```
- Server port is set in `server/src/main/resources/application.yaml`. If you change it,
  update the tunnel: `adb reverse tcp:8080 tcp:<port>`, and the URL in
  `app/.../net/StrokeRelay.kt`.
