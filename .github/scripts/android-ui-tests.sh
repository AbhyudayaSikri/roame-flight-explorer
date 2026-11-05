#!/usr/bin/env bash
# Android emulator smoke tests (android-ui.yml). Runs once the emulator has booted, with the local
# API serving on port 8080 with `--allow-control`, and its PID in "$RUNNER_TEMP/server.pid". To run
# it on your own emulator, start the API the same way and set RUNNER_TEMP (and ANDROID_SERIAL if
# more than one device is attached).
#
# Every run uses one install, so the app keeps its database: the last run relaunches onto the cache
# and the saved flight that the demo walkthrough left, after the API has been stopped.
set -euo pipefail

out="$RUNNER_TEMP/android-ui"
mkdir -p "$out"
runner=travel.roame.takehome.android.test/androidx.test.runner.AndroidJUnitRunner
tests=travel.roame.takehome.android

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
# Every class; DemoWalkthrough and OfflineRelaunchTest skip themselves without their arguments.
instrument online -e package "$tests" || status=1

# android-demo.mp4: the walkthrough, then the offline relaunch once the API has stopped.
adb shell screenrecord --time-limit 180 --size 720x1600 --bit-rate 3000000 /sdcard/android-demo.mp4 &
recorder=$!
sleep 1
instrument demo -e class "$tests.DemoWalkthrough" -e demo true || status=1

kill "$(cat "$RUNNER_TEMP/server.pid")"
for _ in $(seq 1 20); do
  curl -fsS http://127.0.0.1:8080/health > /dev/null 2>&1 || break
  sleep 0.5
done

instrument offline -e class "$tests.OfflineRelaunchTest" -e offline true || status=1

# SIGINT makes screenrecord finish the file; then give it a moment to close it.
adb shell pkill -INT screenrecord || true
wait "$recorder" || true
sleep 2
adb pull /sdcard/android-demo.mp4 "$out/android-demo.mp4" || true

adb pull /sdcard/Android/data/travel.roame.takehome.android/files/ui-test-failures "$out/" > /dev/null 2>&1 || true
adb exec-out screencap -p > "$out/last-screen.png" || true
adb logcat -d > "$out/logcat.txt" || true
exit "$status"
