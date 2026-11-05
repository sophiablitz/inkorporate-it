#!/usr/bin/env bash
# Start the stroke-relay server (Ktor, port 8080).
#
# First run downloads Gradle + a JDK 21 toolchain, so it can take a minute and
# look like it's hanging — that's normal. Leave it running; it serves until you
# press Ctrl+C.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT/server"

echo "Starting Inklings relay server on http://localhost:8080 (Ctrl+C to stop)…"
exec ./gradlew run
