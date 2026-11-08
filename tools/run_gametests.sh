#!/usr/bin/env bash
# Runs the GameTest server and fails unless the explicit "All N required tests passed" line appears.
# (runGameTestServer exits 0 even when the server fails to start, so the Gradle result alone is not trustworthy.)
set -u
cd "$(dirname "$0")/.."
: "${JAVA_HOME:=$HOME/.gradle/jdks/eclipse_adoptium-17-amd64-windows/jdk-17.0.20.1+1}"; export JAVA_HOME
LOG="${1:-run-gametest/gametest-output.log}"
mkdir -p run-gametest
# Fresh world every run: mobs saved from earlier runs (a wandering witch) made AI target tests flaky.
rm -rf run-gametest/world
./gradlew runGameTestServer --console=plain > "$LOG" 2>&1
grep -E "tests are now running|failed!|required tests|    - |Failed to start" "$LOG" | grep -v DEBUG
if grep -qE "All [0-9]+ required tests passed" "$LOG"; then echo "GAMETESTS: PASS"; exit 0; fi
echo "GAMETESTS: FAIL (see $LOG)"; exit 1
