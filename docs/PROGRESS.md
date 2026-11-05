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
| (a) Android camera fits the selected route | [#2](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/2) | merged 11:15 (iOS evidence from a dispatched run, see below) |
| (b) `--mode=slow` | [#3](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/3) | merged 21:29, its own Linux and iOS checks green |
| (c) Polish | [#4](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/4) | merged 11:16 (same evidence) |
| (c) follow-up: iOS map re-frames and re-clusters reliably | [#6](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/6) | merged 21:28, its own Linux and iOS checks green |
| F1 XCUITest | — (branch `test/ios-date-range-f1`) | dropped at the 45-minute timebox |
| (d) README, NOTES §3/§4 | [#5](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/5) | this PR, docs only |

- **(a) Android camera fit.**
  - New shared `fitCamera(points, viewWidth, viewHeight, bottomInset, padding)`: Web Mercator math on the route's unwrapped longitudes, so trans-Pacific routes are framed over the Pacific. It returns a centre and a zoom for the area above the card.
  - 6 new tests, written first. One checks that every one of the 192 fixture routes lands inside the padding above a 300 dp card on a 400 × 800 dp map.
  - Android measures the card and calls `fitCamera` when the route changes, then animates over 600 ms. iOS keeps MapKit's own fit.
  - Not checked visually on Android: the local API stays off, per your call. Your QA pass covers it (QA M9).
- **(b) `--mode=slow`.**
  - The server serves the normal fixture 3 s late on `/flights` only. 3 new tests, written first: the module takes a `pause` function, so tests record the pause instead of sleeping.
  - iOS: a new test checks the loading state against the slow API (QA O6 and the "loading first" part of L1), and the recorded demo now starts with a 3 s load.
- **(c) Polish.**
  - Shared `clusterMarkers` merges destination pills that would overlap at the current zoom. 9 tests, written first; one checks that no two pills overlap at 21 zoom levels.
  - Both maps draw merged pills, for example "6 airports  280 · 13k". Tapping one zooms in until its airports separate; on Android that zoom uses `fitCamera` from (a). A new XCUITest taps a merged pill and checks that one of its airports gets its own pill.
  - iOS cabin filter: a list of rows with a checkmark instead of the segmented control that truncated "Premium Economy".
- **F1 XCUITest: dropped.**
  - The test opened the "To" calendar, went back to February and tapped the 1st.
  - The sheet then read "Show 10 flights" instead of 17. No single day has 10 flights, so the "tap outside to close the calendar" fallback most likely changed another filter too.
  - A fix would need another 30-minute iOS run, past the timebox. F1 and F2 are documented as covered by the shared tests only (NOTES §4).
- **macOS runner outage (10:00–11:15).**
  - GitHub dropped every PR iOS job after about 15 minutes in the queue: "The job was not acquired by Runner of type hosted even after multiple attempts" (macOS arm64 capacity). That happened 3 times for #2, and twice each for #3 and #4. No incident was posted on githubstatus.com.
  - One manually dispatched run did get a runner: [41852391748](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41852391748), on `test/ios-date-range-f1`, which contains (a), (c) and the F1 test. It passed 24 of 25 UI tests (the one failure was F1), plus the demo walkthrough and the offline relaunch.
  - At your call, #2 and #4 were merged on that run's evidence plus their green Linux checks. Each PR has a comment linking it. #3 waits for its own iOS run.
- **12:20 `main` green on iOS after the merges** ([41869501689](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41869501689)): every UI test, the demo and the offline relaunch.
  - Screenshot review: at world zoom no pills overlap (for example "5 airports 211 · 13k", "2 airports 83 · 22k"), and tapping one zooms into Europe, where CDG, FCO and ATH separate.
- **13:20–21:20 A flaky map test, three fixes (#6).**
  - `MapSelectionTests.testMarkersShowCountAndLowestPointsPerAirport` then failed in 4 of 5 runs. In every failure's UI hierarchy, HND and NRT were still one merged pill after the search narrowed to Tokyo, at the same screen position each time.
  - Fix 1 set the zoom estimate as soon as the app moves the camera. Not enough: it failed in both runs.
  - Diagnosis from that fixed position: the test types with the keyboard up, so the map was short when it framed Tokyo. MapKit fits a short view by its height (about 4–5° of longitude), which merges HND and NRT, and it kept that zoom when the keyboard closed.
  - Fix 2 frames again whenever the map is resized. That passed once and failed once, because asking for the rect it already had is an equal camera position, which doesn't move the camera.
  - Fix 3 re-fits through the equivalent region. It passed in both runs (#3: [41951444932](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41951444932), #6: [41951417332](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41951417332)).
  - It was a real user-facing bug, not a test-only problem: searching from the map left it zoomed out. No test was loosened.
  - #6 also gives the cabin rows primary text instead of the accent tint.
- **Runner shortage continued until about 17:20**: two more dropped jobs for #3, and one dispatched run that never got a runner. After that every job got a runner within minutes.

## Evening: your second list (from 21:36)

Order: (a) F1 investigation; (b) Server URL, then release packaging; (c) `qa` issues as you file them, ahead of everything; (d) NOTES §1 and §5; (e) the remaining §13 items. Also the lighter iOS job on pull requests (your answer 2).

| Item | PR | State |
|---|---|---|
| (a) F1 investigation | [#7](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/7) | merged 22:38 |
| Lighter iOS job on PRs | [#8](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/8) | merged 22:15 |
| `main` was red: map test, keyboard | [#9](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/9) | merged 22:14 |
| (b1) Server URL in both debug menus | [#10](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/10) | merged 22:47, by git (see below) |
| (b2) Release packaging | [#11](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/11) | merged 23:04, by git |
| Screenshot timeouts don't fail tests | [#12](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/12) | merged 22:39 |
| (d) NOTES §1 and §5 | [#13](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/13) | merged after #11 |
| (e) §13.1 Independent review (prompt P3) | (fixes to follow) | review running |
| (e) §13.2 Android offline map style (QA O8) | [#14](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/14) | merged 23:15 |
| (e) §13.5 StrictMode in debug builds (QA T1) | [#15](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/15) | merged 23:25 |
| (e) §13.6a Compose stability config | this PR | in review |
| (e) §13.6b iOS sliced observation | next PR | to do |
| (e) §13.7 Polish: "Flight Explorer" display names (QA P6) | [#18](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/18) | merged after #20 |
| (e) §13.8 `.gitattributes` LF for the fixture | [#20](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/20) | merged 23:30 |

- **(a) F1: not a conversion bug.**
  - Frames from the test's screen recording show the picker setting Feb 1 → Feb 1 and the sheet reading "Show 17 flights".
  - The calendar then closed by itself, and the test's "dismiss" tap landed on the Economy row. Feb 1 in Economy is 10 flights.
  - The conversion now lives in shared code, with `PickerDateTest` in ten time zones and two store tests.
- **`main` was red after the daytime merges** (full run [41955053011](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41955053011)): the map test again.
  - The recording showed the camera framing Tokyo while the keyboard shortened the map; when the keyboard closed, the map grew but kept that camera.
  - #9 lets the map run under the keyboard. It passed two full runs before merging.
- **PR #7's first run** also failed on "Timed out while requesting screenshot" with five macOS jobs running. #12 makes capturing evidence screenshots non-fatal; assertions are unchanged.
- **(b1)** `DebugMenuTests` passed in CI over plain HTTP to the runner's LAN IP (`192.168.64.14`), which confirms `NSAllowsLocalNetworking` covers IP addresses.
- **Merging #10 and #11:** the gh token lacks the `workflow` scope, which GitHub requires to merge PRs that change `.github/workflows/`. At your call they're merged by git: the same merge commit, pushed to `main` over SSH, and GitHub marks the PR merged.
- No `qa` issues were filed as of 22:45.
- **(e) §13.2 Android offline map style (QA O8).**
  - Offline, MapLibre couldn't load the OpenFreeMap style, and with no style nothing was drawn: no pills, no route.
  - On `OnDidFailLoadingMap` the map now switches to a background-only style built into the app. The route and pill layers need no downloads, so they still render.
  - There's no logic to unit-test and no Android UI tests, so QA O8 covers it in your Windows pass.
- **(e) §13.5 StrictMode (QA T1).**
  - Debug builds turn on StrictMode in a new `FlightExplorerApplication`. It logs, and never crashes on, main-thread disk or network access and leaked closeables, SQLite objects or activities.
  - Release builds are unchanged. T1 now says how to watch the log: `adb logcat -s StrictMode`.
  - §13 items 3 (iOS camera fit) and 4 (`ci.yml` on PRs) were already done.
- **(e) §13.6a Compose stability config.**
  - `androidApp/compose-stability.conf` marks the shared value types, and read-only lists and sets of them, as stable. Compose then compares them with `equals`, and skips recomposition when the store rebuilds an equal value.
  - In the compiler report, unstable arguments went from 35 to 17 and unknown-stability ones from 14 to 6. Only the store is left.
  - `-PcomposeReports` regenerates the report.
  - There's no logic to test and no visible change, so there's no new QA item.
- **(e) §13.1 Review fixes: Android Explore state** (review bug 2 and a nit).
  - The search fields now start from the store's query with plain `remember`, instead of restoring old text with `rememberSaveable`. After the system destroyed the activity, a new store and the restored text disagreed: the field said "jfk", but all 1,000 flights showed with no chip.
  - List/Map mode now lives in `FlightExplorerApp`, so a visit to the Saved tab no longer resets it.
  - These are UI-only, and there are no Android UI tests. New QA items O9 and M10; checked on the emulator (below).
- **(e) §13.7 Polish, part 1: display names.**
  - The launcher (Android) and home screen (iOS) now say "Flight Explorer". The bundle and package names don't change.
  - New QA item P6. The rest of §13.7 (chips, −/+ points buttons, skeleton rows, haptics) follows in separate PRs.
- **(e) §13.8 `.gitattributes` LF for the fixture.**
  - `fixtures/*.json text eol=lf` keeps the fixture byte-identical on every clone. A Windows checkout with `core.autocrlf=true` would otherwise get CRLF, and the server would serve those bytes.
  - `git add --renormalize .` changed nothing, because the committed fixture was already LF.
  - Cloned with `core.autocrlf=true`, the fixture was 447,745 bytes with 15,004 CRs before the rule, and exactly 432,741 bytes with none after.

## Decisions

- **Lighter iOS job on pull requests (Nov 4, evening, your call).**
  - PRs run the build, the shared tests on the simulator and 5 smoke XCUITests: launch, search, map selection, save, error state.
  - Pushes to `main` and workflow_dispatch run the full suite, the demo and the offline relaunch. A red full run on `main` is fixed first.
- **CI triggers:** added `overnight` to the `push` branches of `ci.yml` and `ios.yml` instead of opening the PR early, so every push gets CI. Reverted before merging (Nov 4, 08:00); PRs and pushes to `main` still run both workflows.
- **First push carries three commits:** setup, version catalog and server. The mandated pre-push check (`:server:test :shared:testAndroidHostTest :androidApp:assembleDebug`) can't pass until `:server` and the shared host-test task exist.
- **Version catalog in step 4, not 5.0:** `gradle/libs.versions.toml` now holds every §3.1 version, and all build files use it. The server was the first module to need new versions.
- **Host tests enabled in step 4:** `withHostTest {}` went in early so `:shared:testAndroidHostTest` exists for the pre-push check. With no test sources it reports NO-SOURCE.
- **Tier skips (`:server`):** the Python fallback server (COULD) isn't built. `--mode=slow` (SHOULD) was built on Nov 4 as item (b).
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
