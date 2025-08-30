#!/usr/bin/env bash
set -euo pipefail

# Legacy entrypoint
MAIN_FQCN="${1:-il.ac.tau.sc.software1.examples.Main}"

echo "Cleaning old build..."
rm -rf out
mkdir -p out

echo "Compiling Java sources..."
find src -name "*.java" -print0 | xargs -0 javac -encoding UTF-8 -d out

echo "Running $MAIN_FQCN"
java -cp out "$MAIN_FQCN"
