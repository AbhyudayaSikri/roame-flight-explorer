# Overnight run log (Nov 4, 2026)

Branch `overnight`, cut from `main` at `6f91c42`. Scope: `docs/PLAN.md` steps 4–8, MUST-tier items only, one step at a time. Times are local (UTC-5).

## Status

| Step | State | Last green CI |
|---|---|---|
| 4. Local API (`:server`) | done 00:56 | [ci](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41781065144) · [ios](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41781065124) |
| 5. Shared domain | done 01:10 | [ci](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41782370174) · [ios](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41782370193) |
| 6. Data layer + store | done 01:34 | [ci](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41784197206) · [ios](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41784197457) |
| 7. Android UI | done 01:43 | [ci](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41786516273) (Android-only change, so `ios.yml` didn't run) |
| 8. iOS UI | done 03:26; flaky-test fix green 04:30 | [ci](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41803655191) · [ios](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41803655158) (demo video and screenshots in the `ios-ui-tests` artifact) |

## Log

- **00:40 Setup.** Created `overnight` from `main`. Both workflows now also run on pushes to `overnight`; before this they only ran on `main` and on PRs.
- **00:40–00:56 Step 4 (local API).**
  - `:server` is a Ktor CIO app. `GET /flights` serves the fixture byte-for-byte as `application/json`; `GET /health` returns `ok`.
  - Modes: `--mode=empty` (200 with no flights), `error` (500) and `updated` (every flight +500 points).
  - `POST /control/mode?value=…` exists only with `--allow-control`.
  - Every request is logged with method, path, mode and status.
  - 13 tests: one per mode, a byte-for-byte fixture check, the control endpoint on and off, and argument parsing.
  - Smoke test of `./gradlew :server:run`: `curl` got 200 `application/json` with 1,000 flights, byte-identical to the fixture, and the control endpoint switched modes.
  - `ci.yml` now runs the full pre-push check: `:server:test :shared:testAndroidHostTest :androidApp:assembleDebug`.
- **00:56–01:10 Step 5 (shared domain).**
  - Pure functions in `commonMain`, written test-first:
    - Decoding.
    - `CalendarDate`.
    - Search: exact code first, then code-prefix or city word-prefix.
    - AND/OR filters, `DataBounds`, destination markers.
    - Antimeridian-safe great-circle routes.
    - Shared English formatting and active-filter chips.
  - 46 `commonTest` tests use the real fixture, embedded at build time by `:shared:generateTestFixture`. They passed on the JVM and on the iOS simulator in CI.
  - `ios.yml` now runs `:shared:iosSimulatorArm64Test --device $UDID` and fails if no results are written.
  - minSdk is now 26 in both modules, for kotlinx-datetime.
- **01:10–01:13 Step 6 (data layer + store), local work.**
  - SQLDelight persistence, Ktor `FlightsApi`, the repository, `SyncState`, `FlightExplorerStore`, and `AndroidExplorer`/`IosExplorer`.
  - 87 shared tests are green on the JVM.
  - Two problems surfaced on the first test run:
    - The test harness ran the store inside `backgroundScope`, whose work `advanceUntilIdle()` skips. It now uses a child job with a normal test dispatcher.
    - The state waited for every database read before deriving anything (real bug, fixed; see Review findings).
- **01:34 Step 6 green in CI.**
  - Linux: 87 shared tests plus the server tests.
  - iOS: the same 87 tests on the simulator, using SQLDelight's native driver and Ktor Darwin.
  - The app now links with `-lsqlite3`, and the existing UI test and persistence probe still pass.
- **01:20–01:34 Step 7 (Android UI), local work.** The MUST bullets compiled on the first try.
  - Smoke test on a new local emulator, `roame-smoke` (API 35, arm64; your existing AVDs weren't touched).
  - With no server running, the app launches and shows "Couldn't load flights… Retry". No crash; screenshot checked.
  - Starting the local API for a with-data smoke test was blocked by the tool's auto-review (it misread an old "don't start the server" instruction). I didn't work around it, so no with-data smoke test was run on the Mac. Your Windows QA pass covers that.
- **01:44–01:54 Step 8 (iOS UI), local work.**
  - SwiftUI app over the shared store:
    - `@MainActor @Observable ExplorerModel` fed by `for await` over `store.state`.
    - Explore (search fields, count, chips, Reset, filter sheet, List/Map, card) and Saved tabs.
    - Loading, error, empty, no-matches and stale states, plus a message banner.
  - Seven XCUITest classes, one per QA group.
  - `ios.yml`:
    - Starts the API with `--allow-control` and runs the suite.
    - Seeds a cache and a saved flight, stops the API, then runs `OfflineRelaunchTests`.
  - The step-2b SKIE spike and persistence probe are replaced, as PLAN 8.1 planned.
- **02:23 First iOS run** ([41788560793](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41788560793)). Everything compiled first time, and 19 of 23 UI tests passed. The offline relaunch run also passed (O1–O3). Four failures, from two causes:
  - **Map markers not found (3 tests).** The failure's UI hierarchy showed the markers are accessible buttons with the right ids, but the fixed starting region only covered Europe and Africa, so Tokyo was off-screen. The map now uses `.automatic` (it frames the markers) and fits the selected route.
  - **Slider (1 test).** `adjust(toNormalizedSliderPosition: 0)` stopped at about 20,000 points ("Show 45 flights"). The test now drags the thumb past the left end.
  - From the screenshots (PLAN 8.4):
    - The search placeholder was truncated, so it now reads "From" / "To".
    - "0 flights" showed above the error screen, so the count is now hidden while there's no data.
- **02:58 Second iOS run green** ([41792526988](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41792526988)): 23/23 UI tests, the demo walkthrough, and the offline relaunch with the API stopped. The demo video is 27 MB.
  - Screenshot review (PLAN 8.4):
    - SFO → NRT draws as one arc over the North Pacific, framed above the card (M8).
    - Tokyo shows HND and NRT as separate markers.
    - Fixed: a single-airport result zoomed to street level. Markers within 2° now get a regional view.
    - Left as is: "Premium Economy" truncates in the segmented cabin picker.
- **03:26 Step 8 green** ([ios 41796168173](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41796168173)).
  - 23/23 UI tests passed, plus the demo walkthrough and the offline relaunch.
  - The screenshots confirm the regional zoom for one airport and HND/NRT apart for Tokyo.
  - Per-item iOS QA results are in `docs/QA_IOS.md`.
  - Cleanup: stopped the local `roame-smoke` emulator. The AVD itself is still there.
- **03:56 Draft PR [#1](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/1) opened.**
  - Its iOS check ([41799697121](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41799697121)) failed 1 of 23 on unchanged code: V2's "No flight card".
  - Diagnosis from the failure's UI hierarchy:
    - The list already showed the 3 JFK → HND flights at the updated prices, but nothing was selected.
    - The test had tapped the first row right after typing "hnd". The store filters asynchronously, and here the price refresh was landing at the same moment, so the tap hit a row from the JFK-only results. The store then correctly cleared a selection that was no longer in the results.
  - Fix (test synchronization, no assertion loosened): every select after a search now waits for that search's count first.
- **04:30 Green on `e3e9c4b`, twice.**
  - The push runs ([ci](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41803655191), [ios](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41803655158)) and the PR runs ([ci](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41803662023), [ios](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41803662017)) all passed.
  - Both iOS runs passed all 23 UI tests, the demo and the offline relaunch.

## Daytime: SHOULD items brought forward (from 09:31)

Your order: (a) Android camera fit, (b) `--mode=slow`, (c) polish (iOS cabin truncation, marker overlap on both apps), (d) README and NOTES §3/§4. There's also one iOS XCUITest for F1, timeboxed to 45 minutes. One branch and one PR per item, each merged into `main` once it's green. Everything else in §13 waits for the Android QA pass 1 fixes.

| Item | PR | State |
|---|---|---|
| (a) Android camera fits the selected route | `feat/android-camera-fit` | in review |
| (b) `--mode=slow` | — | pending |
| (c) Polish | — | pending |
| F1 XCUITest | — | pending |
| (d) README, NOTES §3/§4 | — | pending |

- **(a) Android camera fit.**
  - New shared `fitCamera(points, viewWidth, viewHeight, bottomInset, padding)`: Web Mercator math on the route's unwrapped longitudes, so trans-Pacific routes are framed over the Pacific. It returns a centre and a zoom for the area above the card.
  - 6 new tests, written first. One checks that every one of the 192 fixture routes lands inside the padding above a 300 dp card on a 400 × 800 dp map.
  - Android measures the card and calls `fitCamera` when the route changes, then animates over 600 ms. iOS keeps MapKit's own fit.
  - Not checked visually on Android: the local API stays off, per your call. Your QA pass covers it (QA M9).

## Decisions

- **CI triggers:** added `overnight` to the `push` branches of `ci.yml` and `ios.yml` instead of opening the PR early, so every push gets CI. Reverted before merging (Nov 4, 08:00); PRs and pushes to `main` still run both workflows.
- **First push carries three commits:** setup, version catalog and server. The mandated pre-push check (`:server:test :shared:testAndroidHostTest :androidApp:assembleDebug`) can't pass until `:server` and the shared host-test task exist.
- **Version catalog in step 4, not 5.0:** `gradle/libs.versions.toml` now holds every §3.1 version, and all build files use it. The server was the first module to need new versions.
- **Host tests enabled in step 4:** `withHostTest {}` went in early so `:shared:testAndroidHostTest` exists for the pre-push check. With no test sources it reports NO-SOURCE.
- **Tier skips (`:server`):** `--mode=slow` (SHOULD) and the Python fallback server (COULD) aren't built. `CLAUDE.md` still lists `slow`, but the server rejects it.
- **Server bytecode targets JVM 17**, so it also runs on Android Studio's bundled JDK on Windows. `slf4j-simple` is there only so Ktor's startup lines print instead of an SLF4J warning.
- **Working ahead:** while a step's CI runs, I draft the next step in the working tree. It's only committed and pushed once the current step is green.
- **Android tier skips:**
  - Built without these SHOULD/COULD items: camera fit, the offline fallback map style, individually removable chips (chips open the filter sheet instead), −/+ points buttons, skeleton rows, haptics, StrictMode, the Compose stability config, `testTag`s and the "Flight Explorer" app label.
  - Polish items P1 (consistent spacing via one `Spacing` scale) and P2 (pressed and progress states on Save/Remove) are MUST and are built.
- **Android pull-to-refresh is on the list only.** On the map, a vertical drag would fight panning. The top bar's refresh button works on every screen.
- **Search fields own their text.** Typing never waits on the store, which derives asynchronously. Reset clears both the fields and the store.
- **The iOS demo video is recorded on every iOS CI run**, rather than by a manual `demo.yml`. A `workflow_dispatch` workflow can't be run until it exists on `main`, which this run doesn't touch.
  - CI records the simulator while `DemoWalkthrough` runs (slow-paced: search → filters → map marker → select → save → Saved tab) and while `OfflineRelaunchTests` runs after the API is stopped.
  - The walkthrough also seeds the offline run. The mp4 is in the `ios-ui-tests` artifact.
- **iOS tier choices:** fitting the camera to the selected route (SHOULD) is built, because QA M8 is judged from the screenshot. Not built: re-rendering only the changed slices, haptics, the "Flight Explorer" display name, dark mode and Dynamic Type passes.

## Step 6 review findings (skeptical re-read of BRIEF.md and the step-6 diff)

Fixed, each with a test:

1. **Silent failure: storage errors could be invisible.** The state waited until all three database reads had succeeded, so a "Couldn't read…" message never reached the UI while a read kept failing. Each read now starts as "not read yet", and the state derives immediately (`aBrokenStorageReadBecomesAMessageAndRecovers`).
2. **Race: `close()` versus in-flight writes.** `close()` closed the SQLite driver at once, while a cancelled coroutine could still be inside a blocking transaction on the IO thread. That could throw "database closed" on Android when `onCleared` runs. Resources now close only after the store's job completes (`closeReleasesResourcesOnlyAfterWorkInFlightFinishes`).
3. **Race: lost messages.** `messageShown()` cleared whatever message was current, so a second failure that arrived while the first was on screen was dropped. It's now `messageShown(messageId)` (`dismissingAnOlderMessageKeepsANewerOne`).
4. **Silent failure: later storage errors.** Only the first-ever failure posted a message; a failure after storage had recovered posted none. Now each new streak of failures posts one (`eachNewStreakOfStorageErrorsPostsAMessage`).
5. **Silent failure: debug reset.** A failed `resetData` was ignored; it now posts a message.
6. **Untested boundaries:** direct tests for every `syncStateOf` combination, the saved-flights overlay (pending save on top, pending removal hidden, re-save shows the new snapshot once) and `saveStatusOf` (`StoreRulesTest`).

Checked and left alone:

- **Main thread.** DB reads (`mapToList(io)`) and writes (`withContext(io)`) never run on the main thread. Derivation runs on `Dispatchers.Default`; only small map assignments happen on main. Both platform drivers open the database lazily on first use, which is on IO.
- **Refresh races.** `refresh()` ignores calls while one is running; the store is called from one thread.
- **Selection.** Selection, focus and Saved-tab selection are cleared only after every read has arrived. Corrections reach a fixed point (one extra derive).
- **Remove right after save.** A remove right after a save is accepted once the save has finished, even before the database flow re-emits. Only operations still in flight are ignored.

## Failures and fixes

- **Store tests never advanced** (01:10). 21 of 23 store tests saw only the initial state. Diagnosis from a probe test: the harness scope inherited `backgroundScope`'s context, and `advanceUntilIdle()` doesn't run background work. Fixed in the harness (first attempt).
