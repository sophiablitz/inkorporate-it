#!/usr/bin/env bash
# The project's test script. GitHub runs it on every pull request, and the
# pre-push hook runs it on your machine before you push.
#
#   scripts/test.sh              run everything
#   scripts/test.sh front-end    run only the Android checks
#   scripts/test.sh back-end     run only the back-end checks
#   scripts/test.sh shared       run only the shared/ checks
#
# Since #16 everything is one Gradle build at the repo root, so every target
# runs ./gradlew from there (and needs the Android SDK, see CONTRIBUTING.md).

set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
target="${1:-all}"

test_front_end() {
  echo "==> front-end: unit tests and lint"
  (cd "$repo_root" && ./gradlew :app:testDebugUnitTest :app:lintDebug --console=plain)
}

test_back_end() {
  echo "==> back-end: unit tests"
  (cd "$repo_root" && ./gradlew :server:check --console=plain)
}

test_shared() {
  echo "==> shared: unit tests"
  (cd "$repo_root" && ./gradlew :shared:check --console=plain)
}

test_all() {
  # One Gradle call for everything, so Gradle only starts up once.
  echo "==> everything: app unit tests and lint, server checks, shared checks"
  (cd "$repo_root" && ./gradlew :app:testDebugUnitTest :app:lintDebug :server:check :shared:check --console=plain)
}

case "$target" in
  all) test_all ;;
  front-end) test_front_end ;;
  back-end) test_back_end ;;
  shared) test_shared ;;
  *)
    echo "Unknown target: $target (use front-end, back-end, shared, or nothing)" >&2
    exit 2
    ;;
esac

echo "All checks passed."
