#!/usr/bin/env bash
# Set up the adb reverse tunnel so a device/emulator can reach the server.
#
# The app connects to ws://localhost:8080/relay. On a device, "localhost" means the
# device itself, so each device needs this tunnel to your computer. Re-run it whenever
# a device reconnects, the emulator restarts, or adb restarts.
#
# Usage:
#   ./scripts/tunnel.sh            # tunnel every connected device
#   ./scripts/tunnel.sh <serial>   # tunnel one device (see: adb devices)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Find adb: PATH first, then the SDK from local.properties, then common defaults.
find_adb() {
  if command -v adb >/dev/null 2>&1; then command -v adb; return; fi
  local sdk=""
  if [ -f "$ROOT/local.properties" ]; then
    sdk="$(grep -E '^sdk\.dir=' "$ROOT/local.properties" | head -1 | cut -d= -f2-)"
  fi
  [ -z "$sdk" ] && sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
  if [ -x "$sdk/platform-tools/adb" ]; then echo "$sdk/platform-tools/adb"; return; fi
  echo ""
}

ADB="$(find_adb)"
if [ -z "$ADB" ]; then
  echo "adb not found. Run ./scripts/setup.sh first, or install Platform-Tools." >&2
  exit 1
fi

PORT=8080
reverse_one() {
  local serial="$1"
  if [ -n "$serial" ]; then
    "$ADB" -s "$serial" reverse "tcp:$PORT" "tcp:$PORT"
    echo "✓ tunneled $serial  (tcp:$PORT → computer tcp:$PORT)"
  else
    "$ADB" reverse "tcp:$PORT" "tcp:$PORT"
    echo "✓ tunneled default device (tcp:$PORT → computer tcp:$PORT)"
  fi
}

if [ "${1:-}" != "" ]; then
  reverse_one "$1"
else
  # No serial given: tunnel every attached device.
  SERIALS="$("$ADB" devices | awk 'NR>1 && $2=="device" {print $1}')"
  if [ -z "$SERIALS" ]; then
    echo "No connected devices. Start an emulator or plug in a phone, then re-run." >&2
    exit 1
  fi
  COUNT="$(echo "$SERIALS" | wc -l | tr -d ' ')"
  if [ "$COUNT" = "1" ]; then
    reverse_one ""
  else
    echo "$SERIALS" | while read -r s; do reverse_one "$s"; done
  fi
fi

echo "Active tunnels:"
"$ADB" reverse --list || true
