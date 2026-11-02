#!/usr/bin/env bash
# The project's test script. GitHub runs it on every pull request, and the
# pre-push hook runs it on your machine before you push.
#
#   scripts/test.sh              run everything
#   scripts/test.sh front-end    run only the Android checks
#   scripts/test.sh back-end     run only the back-end checks

set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
target="${1:-all}"

test_front_end() {
  echo "==> front-end: unit tests and lint"
  (cd "$repo_root/front-end" && ./gradlew testDebugUnitTest lintDebug --console=plain)
}

test_back_end() {
  # Replace this with the real test command once the back-end project exists.
  echo "==> back-end: no tests yet"
}

case "$target" in
  all)
    test_front_end
    test_back_end
    ;;
  front-end) test_front_end ;;
  back-end) test_back_end ;;
  *)
    echo "Unknown target: $target (use front-end, back-end, or nothing)" >&2
    exit 2
    ;;
esac

echo "All checks passed."
