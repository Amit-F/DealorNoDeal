#!/usr/bin/env bash
set -euo pipefail

# Simple runner for the renovated v2 CLI.
# Example:
#   ./run-v2.sh --cases=10 --seed=42
#
# If no args are provided, we default to a small, deterministic game.
ARGS=${*:-"--cases=10 --seed=42"}

# Build (fast if already built), install the distribution, and run the app.
./gradlew --no-daemon :cli:installDist >/dev/null

exec ./cli/build/install/cli/bin/cli $ARGS

