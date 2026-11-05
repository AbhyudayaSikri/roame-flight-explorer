#!/usr/bin/env bash
# Android emulator smoke tests (android-ui.yml). Runs once the emulator has booted, with the local
# API serving on port 8080 with `--allow-control`, and its PID in "$RUNNER_TEMP/server.pid". To run it on your own
# emulator, start the API the same way and set RUNNER_TEMP (and ANDROID_SERIAL if more than one
# device is attached).
#
# Every run uses one install, so the app keeps its database: the last run relaunches onto the cache
# and saved flight the one before it left, after the API has been stopped.
set -euo pipefail

out="$RUNNER_TEMP/android-ui"
mkdir -p "$out"
runner=travel.roame.takehome.android.test/androidx.test.runner.AndroidJUnitRunner

# A clean install, so nothing from an earlier run on this device (a saved flight, a debug Server
# URL) carries over.
adb uninstall travel.roame.takehome.android > /dev/null 2>&1 || true
adb uninstall travel.roame.takehome.android.test > /dev/null 2>&1 || true
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb install -r androidApp/build/outputs/apk/androidTest/debug/androidApp-debug-androidTest.apk
adb logcat -c

# `am instrument` exits 0 even when tests fail; success is its closing "OK (n tests)" line.
instrument() {
  local name=$1
  shift
  adb shell am instrument -w "$@" "$runner" | tee "$out/$name.txt"
  grep -q '^OK (' "$out/$name.txt"
}

status=0
offline=travel.roame.takehome.android.OfflineRelaunchTest
# Every class; OfflineRelaunchTest skips itself without `-e offline`.
instrument online -e package travel.roame.takehome.android || status=1
instrument offline-before -e class "$offline" -e offline before || status=1

kill "$(cat "$RUNNER_TEMP/server.pid")"
for _ in $(seq 1 20); do
  curl -fsS http://127.0.0.1:8080/health > /dev/null 2>&1 || break
  sleep 0.5
done

instrument offline-after -e class "$offline" -e offline after || status=1

adb pull /sdcard/Android/data/travel.roame.takehome.android/files/ui-test-failures "$out/" > /dev/null 2>&1 || true
adb exec-out screencap -p > "$out/last-screen.png" || true
adb logcat -d > "$out/logcat.txt" || true
exit "$status"
