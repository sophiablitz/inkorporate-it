#!/usr/bin/env bash
# Requires the [emulator-tested] flag on commits that change the Android app.
# Commits that only touch other folders, or only Markdown files, don't need it.
#
#   scripts/check-emulator-flag.sh --message-file <file>   check a commit being made
#   scripts/check-emulator-flag.sh --range <base>..<head>  check existing commits

set -euo pipefail

flag="[emulator-tested]"

# Reads file paths on stdin and succeeds if any of them are Android app changes.
touches_app() {
  grep '^front-end/' | grep -v '\.md$' > /dev/null
}

has_flag() {
  grep -F "$flag" > /dev/null
}

case "${1:-}" in
  --message-file)
    # Merge commits only combine work that was already checked.
    if [ -f "$(git rev-parse --git-path MERGE_HEAD)" ]; then
      exit 0
    fi
    if git diff --cached --name-only | touches_app && ! has_flag < "$2"; then
      echo "Commit blocked: this commit changes the Android app." >&2
      echo "Run it on the emulator, then add $flag to your commit message:" >&2
      echo "  git commit -m \"Add login screen $flag\"" >&2
      exit 1
    fi
    ;;
  --range)
    missing=0
    for commit in $(git rev-list --no-merges "$2"); do
      if git diff-tree --no-commit-id --name-only -r "$commit" | touches_app \
        && ! git log -1 --format=%B "$commit" | has_flag; then
        echo "Missing $flag: $(git log -1 --format='%h %s' "$commit")" >&2
        missing=1
      fi
    done
    if [ "$missing" -eq 1 ]; then
      echo "To fix this, see \"Fixing a missing flag\" in CONTRIBUTING.md." >&2
    fi
    exit "$missing"
    ;;
  *)
    echo "Usage: $0 --message-file <file> | --range <base>..<head>" >&2
    exit 2
    ;;
esac
