#!/usr/bin/env bash
# One-time setup for the Inklings monorepo (app + stroke-relay server).
#
# What it does, so the team doesn't have to remember it:
#   1. Finds your Android SDK and writes local.properties (the app build needs it).
#   2. Checks for a usable JDK 21 (the server needs it from the terminal).
#   3. Prints the adb path so you can draw/test without it on your PATH.
#
# Safe to run more than once. It never overwrites a local.properties you already have.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

say()  { printf "\033[1;36m%s\033[0m\n" "$*"; }
ok()   { printf "\033[1;32m✓ %s\033[0m\n" "$*"; }
warn() { printf "\033[1;33m! %s\033[0m\n" "$*"; }
err()  { printf "\033[1;31m✗ %s\033[0m\n" "$*" >&2; }

say "Inklings setup — $ROOT"

# --- 1. Android SDK + local.properties -------------------------------------------
find_sdk() {
  # Honor an explicit env var first.
  if [ -n "${ANDROID_HOME:-}" ] && [ -d "$ANDROID_HOME" ]; then echo "$ANDROID_HOME"; return; fi
  if [ -n "${ANDROID_SDK_ROOT:-}" ] && [ -d "$ANDROID_SDK_ROOT" ]; then echo "$ANDROID_SDK_ROOT"; return; fi
  # Common per-OS defaults.
  for p in \
    "$HOME/Library/Android/sdk" \
    "$HOME/Android/Sdk" \
    "$HOME/AppData/Local/Android/Sdk" \
    "/usr/local/share/android-sdk" \
    "/opt/android-sdk"; do
    if [ -d "$p" ]; then echo "$p"; return; fi
  done
  echo ""
}

SDK="$(find_sdk)"
if [ -z "$SDK" ]; then
  err "Android SDK not found."
  err "Install Android Studio (it bundles the SDK), then re-run this script."
  err "Or set ANDROID_HOME to your SDK path and re-run."
  exit 1
fi
ok "Android SDK: $SDK"

write_local_properties() {
  local file="$1"
  if [ -f "$file" ]; then
    ok "local.properties already present: $file (left unchanged)"
  else
    echo "sdk.dir=$SDK" > "$file"
    ok "Wrote $file"
  fi
}
# The app build reads the root local.properties.
write_local_properties "$ROOT/local.properties"

# --- 2. adb -----------------------------------------------------------------------
ADB="$SDK/platform-tools/adb"
if [ -x "$ADB" ]; then
  ok "adb: $ADB"
  if ! command -v adb >/dev/null 2>&1; then
    warn "adb is not on your PATH. Either use the full path above, or add this to your shell profile:"
    warn "  export PATH=\"$SDK/platform-tools:\$PATH\""
  fi
else
  warn "adb not found under the SDK. Install 'Android SDK Platform-Tools' from Android Studio's SDK Manager."
fi

# --- 3. JDK 21 for the server -----------------------------------------------------
JAVA_BIN=""
if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/java" ]; then
  JAVA_BIN="${JAVA_HOME}/bin/java"
elif command -v java >/dev/null 2>&1; then
  JAVA_BIN="$(command -v java)"
fi
if [ -n "$JAVA_BIN" ]; then
  VER="$("$JAVA_BIN" -version 2>&1 | head -1)"
  ok "java: $VER"
  warn "The server targets JDK 21. If './gradlew run' complains about the JDK, the Gradle wrapper"
  warn "will download a matching toolchain on first run (needs internet, and is slow the first time)."
else
  warn "No 'java' found. The server's Gradle wrapper can download a JDK 21 toolchain on first run,"
  warn "but installing a JDK 21 (e.g. Temurin 21) is more reliable."
fi

echo
ok "Setup complete."
echo "Next:"
echo "  1. Start the server:   ./scripts/run-server.sh"
echo "  2. Run the app from Android Studio (open the repo root), or: ./scripts/run-app.sh"
echo "  3. Tunnel each device: ./scripts/tunnel.sh"
echo "  4. Open server/tools/relay-pad.html in a browser to test without a second phone."
