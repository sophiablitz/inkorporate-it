#!/usr/bin/env bash
# Build, install, tunnel, and launch the Inklings app on a connected device.
#
# Use this if you'd rather not click Run in Android Studio. It always installs a
# fresh build, which avoids the "stale APK runs old code" trap.
#
# Prereqs: ./scripts/setup.sh has been run, an emulator/phone is connected, and the
# server is running (./scripts/run-server.sh).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

APP_ID="com.blitz.inklings"

# Resolve the SDK so Gradle (and adb) can find it even if ANDROID_HOME isn't set.
SDK=""
if [ -f "$ROOT/local.properties" ]; then
  SDK="$(grep -E '^sdk\.dir=' "$ROOT/local.properties" | head -1 | cut -d= -f2-)"
fi
[ -z "$SDK" ] && SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
if [ ! -d "$SDK" ]; then
  echo "Android SDK not found. Run ./scripts/setup.sh first." >&2
  exit 1
fi
export ANDROID_HOME="$SDK"
ADB="$SDK/platform-tools/adb"

echo "Building and installing a fresh debug APK…"
./gradlew :app:installDebug

echo "Setting up the adb tunnel…"
"$ROOT/scripts/tunnel.sh"

echo "Launching $APP_ID ..."
"$ADB" shell monkey -p "$APP_ID" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || \
  "$ADB" shell am start -n "$APP_ID/.MainActivity" >/dev/null 2>&1

echo "✓ App launched. If it was already open, it reconnects on the drawing screen."
echo "  Watch the relay connection:  $ADB logcat -s StrokeRelay"
