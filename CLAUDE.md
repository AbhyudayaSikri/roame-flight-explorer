# Roame Flight Explorer — take-home

Spec: `docs/BRIEF.md` (text of the PDF). It is the source of truth.
Plan: `docs/PLAN.md`. QA checklist: `docs/QA_CHECKLIST.md`. Assumptions/notes: `docs/NOTES.md`.

## Machines
- **Work Mac** — writes all code, owns the repo, pushes. Must NOT open Xcode or run `xcodebuild` locally.
- **Windows laptop** — Android Studio + emulator. QA only: pulls, runs, files GitHub issues (label `qa`). Use `gradlew.bat` and PowerShell syntax there.
- **GitHub Actions** — the only iOS build/test machine (macOS runner). Also Linux CI for server + shared + Android build.

## Commands
- API: `./gradlew :server:run` (args: `--args="--mode=normal|empty|error|slow --port=8080"`)
- Python fallback API: `python3 server/fallback.py`
- Shared tests (JVM/Android host): see `docs/PLAN.md` §Commands for the exact task names once verified
- Android: `./gradlew :androidApp:assembleDebug`
- iOS: CI only (`.github/workflows/ios.yml`). Loop: push → `gh run watch` → `gh run view --log-failed` → fix → push.

## Base URLs
- Android emulator: `http://10.0.2.2:8080` (or `adb reverse tcp:8080 tcp:8080` + `http://localhost:8080`)
- iOS simulator: `http://localhost:8080`
- Configured via BuildConfig (Android) and an Info.plist/build setting (iOS). Never hard-code in shared.

## Architecture rules (do not break)
- ALL networking, persistence, search, filtering, map aggregation, selection and sync-state logic live in `:shared`.
  UIs only render `FlightExplorerStore.state` and send intents. No business logic in Swift or Compose.
- Search: trim + case-insensitive; origin and destination are separate fields; each matches airport code OR city name.
- Filters: AND across filter types; OR within loyalty programs; empty filter = all values;
  departure date range inclusive on both ends; max points inclusive.
- Default query = full data date range, no other filters → all flights visible on first launch.
- Map markers grouped by destination AIRPORT CODE (Tokyo has both HND and NRT). Marker = count + lowest points.
- Selection survives List↔Map switching; cleared automatically when the flight leaves the filtered results.
- Saved flights = full snapshots in their own table, upsert by id, independent of the latest API response.
- SyncState distinguishes a successful EMPTY response from a FAILED request. Failed refresh keeps cache and shows Stale.
- Save/remove return `Result`; UI must surface failures and roll back optimistic changes.
- Never block the main thread. Filtering on `Dispatchers.Default`; latest query wins (`mapLatest`/`flatMapLatest`).

## Conventions
- Pinned toolchain: Kotlin 2.4.21, AGP 9.3.1, Gradle 9.7.0, compileSdk 37, Swift 6. Verify a library supports these before adding it.
- Every behaviour change ships with a test in `shared/src/commonTest`.
- Small, logical commits with conventional messages. Every commit leaves both apps building.
- Record every assumption in `docs/NOTES.md` when you make it.
- Keep `gradlew` LF + executable. Never commit `.DS_Store`.
- Every interactive iOS/Android control gets a stable accessibility identifier / testTag (used by UI tests).
