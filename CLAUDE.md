# Roame Flight Explorer — take-home

Spec: `docs/BRIEF.md` (text of the PDF). It is the source of truth.
Plan: `docs/PLAN.md`. QA checklist: `docs/QA_CHECKLIST.md`. Assumptions/notes: `docs/NOTES.md`.
Priority tiers: every step and QA item is MUST, SHOULD or COULD. Build every MUST on both apps, and pass every MUST QA item on both, before starting any SHOULD/COULD work (PLAN §0, §13).

## Machines
- **Work Mac** — writes all code, owns the repo, pushes. Must NOT open Xcode, run `xcodebuild`, or run iOS Gradle tasks (list under Commands). Xcode.app is installed; leave it alone.
- **Windows laptop** — Android Studio + emulator. QA only: pulls, runs, files GitHub issues (labels `qa` + tier). Use `gradlew.bat` and PowerShell syntax there.
- **GitHub Actions** — the only iOS build/test machine: `macos-26`, Xcode 26.4.1, simulator "iPhone 17" on iOS 26.4. Also Linux CI for the server, shared module and Android build.

## Commands (verified 2026-11-03, PLAN §3.2)
- Run local Gradle as `DEVELOPER_DIR=/var/empty ./gradlew …`. An accidental Xcode-tool call then fails instead of running. Android builds, with SKIE applied, need no Xcode tools.
- Pre-push check: `./gradlew :server:test :shared:testAndroidHostTest :androidApp:assembleDebug`
- Shared tests on JVM (commonTest + androidHostTest): `./gradlew :shared:testAndroidHostTest`
  - One class: `--tests "travel.roame.takehome.SearchTest"`.
  - The task exists only with `withHostTest {}` inside `kotlin { android { } }`.
  - `allTests` doesn't cover it.
- Shared tests on the iOS simulator (CI only): `./gradlew :shared:iosSimulatorArm64Test --device "$UDID"`. `iosX64Test` exists but is disabled on Apple Silicon.
- Never run on the Mac: `build`, `check`, `allTests`, `assemble`, any `*Ios*` task (`compileKotlinIos*`, `link*Ios*`, `iosSimulatorArm64Test`), or `embedAndSignAppleFrameworkForXcode`. They call `xcrun`/`xcodebuild`.
- API:
  - Run: `./gradlew :server:run --args="--mode=normal|empty|error|updated|slow --port=8080"`. `slow` serves the normal fixture 3 s late, to show loading states. In CI, add `--allow-control`, which enables `POST /control/mode?value=…`.
  - Tests: `./gradlew :server:test`.
  - CI binary: `./gradlew :server:installDist`, then `server/build/install/server/bin/server`.
- Python fallback API (Python 3.9+, standard library only): `python3 server/fallback.py` (Windows: `py server\fallback.py`), with the same flags. Tests: `python3 -m unittest discover -s server -p "test_*.py"`.
- Server as one jar: `./gradlew :server:fatJar` → `server/build/libs/roame-flights-server.jar` (fixture embedded; `java -jar`, same flags).
- Releases: `.github/workflows/release.yml` on a `v*` tag publishes the debug APK, the zipped simulator `.app`, the jar, `fallback.py`, `flights.json` and `QUICKSTART.md`. Pull requests that change the workflow and manual runs only build.
- Android: `./gradlew :androidApp:assembleDebug`. Override the base URL with `-PapiBaseUrl=http://…`.
- Compose stability reports: `./gradlew :androidApp:compileDebugKotlin --rerun -PcomposeReports` → `androidApp/build/compose-reports`. New shared types the UI takes as parameters go in `androidApp/compose-stability.conf`.
- iOS project:
  - App sources live in `iosApp/RoameStarter/`; UI tests in `iosApp/RoameStarterUITests/`.
  - After editing `iosApp/project.yml`, or adding, removing or renaming any file under `iosApp/`, run `xcodegen generate --spec iosApp/project.yml` (XcodeGen 2.46.0) and commit everything.
  - CI fails if the committed `.xcodeproj` differs.
- iOS build and tests: CI only (`.github/workflows/ios.yml`).
  - Pull requests: the build, `iosSimulatorArm64Test` and 5 smoke XCUITests (about 15 min).
  - Pushes to `main` and workflow_dispatch: the full suite, the demo and the offline relaunch (about 30 min). `main` must stay green on the full suite.
  - Loop: push → `gh run watch <run-id> --exit-status` → `gh run view --log-failed` → fix → push.
  - Screenshots and raw logs: `gh run download <run-id> -n ios-ui-tests`.
  - `print()` output from tests appears only in the raw `*.log` files; the console shows xcbeautify's summary.

## Base URLs
- Android emulator: `http://10.0.2.2:8080` (or `adb reverse tcp:8080 tcp:8080` + `http://localhost:8080`)
- iOS simulator: `http://localhost:8080`
- Configured via BuildConfig `API_BASE_URL` on Android (`-PapiBaseUrl`), and via the Info.plist key `API_BASE_URL` on iOS (launch argument `-apiBaseUrl`). Never hard-code it in shared.

## Architecture rules (do not break)
- ALL of the following live in `:shared`: networking, persistence, search, filtering, map aggregation, selection, sync state, route geometry and display formatting. UIs only render `FlightExplorerStore.state` and send intents. No business logic in Swift or Compose.
- Search: trim, lowercase and collapse inner whitespace; origin and destination are separate fields.
  - If the text equals an airport code in the data, match that code exactly.
  - Otherwise, match code-prefix OR city word-prefix.
  - So destination `dub` → Dublin only (not Dubai), and origin `fra` → nothing (not San Francisco).
- Filters:
  - AND across filter types; OR within loyalty programs; an empty filter = all values.
  - The departure date range is inclusive on both ends; max points is inclusive.
  - Cabin and program match exactly: "Economy" ≠ "Premium Economy", "Mileage Plan" ≠ "MileagePlus".
- Default query = the full data date range with no other filters, so all flights are visible on first launch. Reset also clears both search fields.
- Map markers are grouped by destination AIRPORT CODE (Tokyo has both HND and NRT). Marker = count + lowest points.
- Selection survives List↔Map switching, and is cleared automatically when the flight leaves the filtered results.
- Saved flights = full snapshots in their own table, upserted by id, independent of the latest API response.
  - A refresh never touches them.
  - "Update saved" re-saves when the cached flight differs from the snapshot.
- SyncState distinguishes a successful EMPTY response from a FAILED request. A failed refresh keeps the cache and shows Stale.
- Save/remove return `Result` internally. On failure, the store rolls back optimistic changes and emits a `message`, and the UI must surface it.
- Never block the main thread: filtering on `Dispatchers.Default`, DB and network on IO, and the latest query wins (`mapLatest`/`flatMapLatest`).
- Flight IDs are opaque strings (the fixture has `f101`–`f106`). Never parse them.

## Swift-facing API rules (SKIE)
- **Public:**
  - `FlightExplorerStore`, `ExplorerState` and the value types it exposes.
  - `DebugOptions` and `TestIds`.
  - The platform factories `AndroidExplorer.createStore(...)` and `IosExplorer.createStore(...)`.

  Everything else is `internal`: repository, API, DB, DTOs, filter functions.
- **Not in public signatures:** `ClosedRange`, nullable primitives (`Int?` becomes `KotlinInt?`), `Result`, `Instant`, kotlinx `LocalDate`. Use instead:
  - `CalendarDate` and `DateRange`.
  - Epoch-millisecond `Long`s.
  - Paired intents (`setMaxPoints` / `clearMaxPoints`).
- **Naming:** use descriptive parameter names, because they become Swift labels. No members named `description` or `hash`, and no method names starting with `new`, `copy` or `init`.
- **State stream:** one `StateFlow<ExplorerState>`, which must never throw: SKIE turns an exception inside a Flow into a Swift crash. Catch errors in the store and surface them via `sync`/`message`.
- **Display strings** (points, dates, chips, marker labels) come from shared, so both apps match and Swift never does `Int32` arithmetic.
- **Swift side:**
  - `@preconcurrency import Shared`.
  - The app target's default actor isolation is MainActor.
  - Observe the store with `for await` inside `.task`.

## Conventions
- **Pinned toolchain:** Kotlin 2.4.21, AGP 9.3.1, Gradle 9.7.0, compileSdk 37.0, Xcode 26.4 (26.4.1 on CI), Swift 6.
  - This is the top of Kotlin 2.4.21's tested compatibility row. Don't bump any of them, even though AGP 9.5.0 and Xcode 26.6 exist.
  - Library versions live in `gradle/libs.versions.toml` (from PLAN §3.1). Verify a library supports the toolchain before adding it.
- **Gradle plugins:** declare every one in the root `plugins {}` with `apply false`. Otherwise SQLDelight's plugin drags in an older KGP.
- **iOS linking:** the `Shared` framework is static, so the iOS app links `-lsqlite3` (set in `project.yml`). If an iOS link fails on files under `shared/build/skie/**/swift-generated-sources`, delete that directory (SKIE #208).
- **Android:**
  - minSdk 26: kotlinx-datetime uses `java.time`.
  - MapLibre comes from `org.maplibre.gl:android-sdk-opengl`; the plain artifact uses Vulkan.
  - SDK packages: `platforms;android-37.0` and `build-tools;37.0.0`.
- **Testing and priority:**
  - Every MUST behaviour change ships with a test in `shared/src/commonTest`.
  - Finish every MUST (PLAN, QA_CHECKLIST) on both apps before starting any SHOULD/COULD.
- **Commits:**
  - Small and logical, with conventional messages.
  - Every commit must leave both apps building. iOS is only confirmed by CI after the push, so a red iOS run is fixed before anything else.
- Record every assumption in `docs/NOTES.md` when you make it.
- Keep `gradlew` LF + executable. Never commit `.DS_Store`.
- **UI identifiers:**
  - iOS: every interactive control gets an `accessibilityIdentifier` from shared `TestIds`. XCUITest depends on them.
  - Android: every interactive control gets a `Modifier.testTag` from the same `TestIds`, for parity with iOS and for QA tooling. `testTagsAsResourceIds()` shows them to UiAutomator. Each window sets it on its root (the `Scaffold`, every sheet, dialog and menu), because new windows don't inherit it. There are no Android UI tests.
