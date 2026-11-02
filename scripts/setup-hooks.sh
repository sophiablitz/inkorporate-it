#!/usr/bin/env bash
# Run this once after cloning. It tells Git to use the hooks in .githooks/.

set -euo pipefail

git config core.hooksPath .githooks
echo "Git hooks are on for this copy of the repo."
