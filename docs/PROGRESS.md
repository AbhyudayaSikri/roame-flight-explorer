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
| (d) NOTES §1 and §5 | [#13](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/13) | merged 23:05 |
| (e) §13.1 Independent review (prompt P3) | fixes in #22–#27 and [#32](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/32) | review done 23:15; summary in NOTES §5 |
| (e) §13.2 Android offline map style (QA O8) | [#14](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/14) | merged 23:07 |
| (e) §13.5 StrictMode in debug builds (QA T1) | [#15](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/15) | merged 23:10 |
| (e) §13.6a Compose stability config | [#16](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/16) | merged 23:17 |
| (e) §13.6b iOS sliced observation | [#17](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/17) | merged 00:15 |
| (e) §13.7 Polish: "Flight Explorer" display names (QA P6) | [#18](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/18) | merged 23:57 |
| (e) §13.8 `.gitattributes` LF for the fixture | [#20](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/20) | merged 23:30 |
| (e) §13.8 Search-rule QA (S6) as an XCUITest | [#19](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/19) | merged 00:37 |
| (e) §13.1 Review fix: decode off the main thread | [#22](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/22) | merged 00:41 |
| (e) §13.1 Review fixes: sync state | [#23](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/23) | merged 00:54 |
| (e) §13.1 Review fixes: store tests that checked too little | [#24](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/24) | merged 00:56 |
| (e) §13.8 Android `testTag`s (QA T4) | [#21](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/21) | merged 00:56 |
| (e) §13.1 Review fixes: Android Explore state (QA O9, M10) | [#25](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/25) | merged 00:56 |
| (e) §13.1 Review fixes: iOS UI-test assertions | [#26](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/26) | merged 01:23 |
| (e) §13.1 Review fix: the points slider during a drag | [#27](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/27) | merged 01:23 |
| (e) §13.7 Polish: individually removable chips (QA F11) | [#28](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/28) | merged 01:23 |
| (e) §13.7 Polish: −/+ points buttons (QA F12) | [#29](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/29) | merged 01:24 |
| (e) §13.7 Polish: skeleton rows (QA L4) | [#30](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/30) | merged 01:24 |
| (e) §13.7 Polish: haptics (QA P7) | [#31](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/31) | merged 01:24 |
| (e) §13.1 Review fixes: debug reset race, iOS pull to refresh; review summary | [#32](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/32) | merged 01:25 |
| (e) §13.9 COULD: dark mode (QA P3) | [#33](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/33) | merged 01:25 |
| (e) §13.9 COULD: large text (QA P4) | [#34](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/34) | merged 01:25 |
| (e) §13.9 COULD: screen-reader labels (QA P5) | [#35](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/35) | merged 01:26 |
| (e) §13.9 COULD: LeakCanary in debug builds (QA T2) | [#36](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/36) | merged 01:26 |

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
- **(e) §13.6b iOS sliced observation.**
  - `ExplorerModel` now mirrors each state field the views read as its own observed property, and assigns it only when its value changed. Each view reads the fields it needs from the model, not the whole `ExplorerState`.
  - The behaviour is meant to be identical. The full XCUITest suite ran on the branch before merging.
  - There's no iOS unit-test target, so the compare-then-assign rule has no unit test of its own. There's no new QA item.
- **(e) §13.7 Polish, part 1: display names.**
  - The launcher (Android) and home screen (iOS) now say "Flight Explorer". The bundle and package names don't change.
  - New QA item P6. The rest of §13.7 (chips, −/+ points buttons, skeleton rows, haptics) follows in separate PRs.
- **(e) §13.8 `.gitattributes` LF for the fixture.**
  - `fixtures/*.json text eol=lf` keeps the fixture byte-identical on every clone. A Windows checkout with `core.autocrlf=true` would otherwise get CRLF, and the server would serve those bytes.
  - `git add --renormalize .` changed nothing, because the committed fixture was already LF.
  - Cloned with `core.autocrlf=true`, the fixture was 447,745 bytes with 15,004 CRs before the rule, and exactly 432,741 bytes with none after.
- **(e) §13.8 Search-rule QA (S6).**
  - The shared `SearchTest` already covered the rule. Now `SearchTests.testAnExactCodeWinsOverCityPrefixes` checks it on iOS too: `dub` gives 29 and `du` 69, then `fra` gives 0 with the no-matches state and `fran` 123.
  - That makes 30 XCUITests. The new test runs in the full suite, not the PR smoke set, so it ran on the branch by dispatch before merging.
- **(e) §13.1 Review fix: decode off the main thread** (review bug 1).
  - The flights response was read and decoded on the store's main dispatcher. On every launch and refresh, the UI thread turned 432 KB into 1,000 `Flight`s.
  - `FlightsApi` now does the whole fetch on `Dispatchers.Default`.
  - The new test `theBodyIsDecodedOnTheWorkDispatcherNotTheCallers` fails if the decode moves back out; I checked that by moving it.
- **(e) §13.1 Review fixes: sync state** (review risks 4 and 6).
  - Loading now lasts until the cache has been read. Before, a refresh that failed first showed "Couldn't load flights" over a valid cache.
  - The cache and its timestamp come from one transaction (`CachedFlights`), so a first launch can't flash "no flights".
  - A retry keeps the stale banner up (`RefreshStatus.Running.previousFailure`).
  - New tests: `aRefreshThatFailsBeforeTheCacheIsReadDoesNotClaimThereIsNoCache`, `aRetryKeepsTheCacheMarkedStaleUntilItSucceeds`, `everyCacheSnapshotPairsTheFlightsWithTheirOwnTimestamp`, and two more `syncStateOf` cases. With the old code, the two store tests fail; I checked by restoring it.
- **(e) §13.1 Review fixes: tests that checked less than their names said** (review findings 7 to 9).
  - `anOutdatedQueryStillBeingComputedNeverReachesTheState` holds the derivation for "jfk" on a new `ManualDispatcher` while "sea" arrives. It fails if `mapLatest` becomes `map`; the old rapid-typing test didn't.
  - `aBadResponseOverACacheKeepsTheCacheAndMarksItStale` covers a malformed response and HTTP 500 over a real cache. The old malformed test is renamed to what it checks, an error with nothing cached.
  - `FileDatabaseRestartTest` closes and reopens an SQLite file between two stores, instead of sharing one in-memory database.
- **(e) §13.8 Android `testTag`s (QA T4).**
  - Android controls have the shared `TestIds` as `testTag`s, matching iOS: search fields and their clear buttons, the count, chips, Reset, Filters, List/Map, rows, the card and its buttons, the sheets, every filter control, the state views, the Saved rows and Remove buttons, tabs, the snackbar and the debug menu.
  - `testTagsAsResourceIds()` is set on the `Scaffold` and on each sheet, dialog and menu window, so UiAutomator lists the tags as resource ids.
  - New shared ids: `filters.dates` (Android's single date-range button) and `saved.empty`. `debug.open` moved from the Server URL menu item to the ⋮ button that opens the menu, as on iOS.
  - New QA item T4.
  - Checked on the `roame-smoke` emulator, with no API running, through `uiautomator dump`:
    - The Explore error screen lists 10 tags as resource ids: the search fields, `results.count`, List/Map, `filters.open`, `error.retry`, both tabs and `debug.open`.
    - The Server URL dialog, a separate window, lists its 4 tags, plus `debug.serverUrl.problem` once `ftp://x` is typed.
    - The Saved tab shows `saved.empty`.
    - The filter sheet, card and destination sheet need data, so QA T4 on Windows covers them.
  - Same session, StrictMode (#15): `adb logcat -s StrictMode` had 10 violations, all from Google's search and Messaging apps, none from ours. That session had no network and no map, so T1 is still the real check.
- **(e) §13.1 Review fixes: Android Explore state** (review bug 2 and a nit).
  - The search fields now start from the store's query with plain `remember`, instead of restoring old text with `rememberSaveable`. After the system destroyed the activity, a new store and the restored text disagreed: the field said "jfk", but all 1,000 flights showed with no chip.
  - List/Map mode now lives in `FlightExplorerApp`, so a visit to the Saved tab no longer resets it.
  - These are UI-only, and there are no Android UI tests. New QA items O9 and M10.
  - Emulator check, `roame-smoke` with no API: type `jfk`, press Home, `am kill` the process, relaunch (a cold start that restores saved state).
    - `main`'s build came back with "jfk" in the field but no "From: jfk" chip, which is the bug.
    - This branch came back with an empty field and no chip, and still in Map mode.
- **(e) §13.1 Review fixes: iOS UI-test assertions** (review finding 10).
  - `SavedTests` now waits for JFK → Paris (8 flights) before checking that the Saved tab ignores the filters.
  - `MapSelectionTests.testSelectionIsSharedBetweenListAndMap` compares every text on the card before and after each switch. All six results are SFO → NRT, so "a card exists" didn't show it was the same flight.
  - `OfflineRelaunchTests` is unchanged. With the API stopped, Refresh fails within milliseconds, so the end state (cache kept, marked stale) is the only stable thing to check, and it's what QA O2 asks for.
- **(e) §13.1 Review fix: the points slider during a drag** (review risk 5).
  - Each drag step updates the store, but the slider read its value back from the store, which arrives a moment later. A value from an earlier step could pull the thumb back mid-drag.
  - Both sliders now follow a local value during the drag (iOS `draggedPoints`, Android `dragging`), then follow the store again afterwards, including Reset.
  - QA F9 now includes a fast drag. This is UI-only. The iOS slider test (`testLowestPointsLimitLeavesTheSingleCheapestFlight`) ran in the full suite on the branch.
- **(e) §13.7 Polish: individually removable chips (QA F11).**
  - Each chip has a × that removes just its filter, through the store's existing `clearFilter`. The label still opens the filter sheet. Removing From or To also empties the field, because the fields own their text.
  - New shared test `removingOneChipClearsOnlyThatFilter`: with all six filters set (2 flights), removing each one in turn leaves exactly the other five chips, with the count the fixture gives without it.
  - New iOS UI test `FilterTests.testEachChipRemovesOnlyItsOwnFilter`, the F11 steps. New id `filterChipRemove(kind)`.
  - Emulator check, no API: typing `jfk` shows the "From: jfk" chip with a × read as "Remove From: jfk". Tapping the × removes the chip and empties the field.
- **(e) §13.7 Polish: −/+ points buttons (QA F12).**
  - iOS uses a `Stepper` beside the value, and Android two tonal buttons. Each calls the new `stepMaxPoints(±1)`, one 500-point slider step. Reaching the highest price means no limit, and each end disables its button.
  - `stepMaxPoints` updates the query directly instead of reading the derived state, so quick taps each count. `quickTapsOnMinusEachCount` fails with the naive version; I checked by trying it.
  - Shared tests also cover one step each way, stopping at both ends, and doing nothing before there's data. New iOS UI test `testMinusAndPlusStepThePointsLimit`, the F12 steps.
- **(e) §13.7 Polish: skeleton rows (QA L4).**
  - The first load shows eight grey rows in `FlightRow`'s shape instead of a spinner: iOS redacts sample rows, Android draws bars. Both keep the `state.loading` id and read as "Loading flights".
  - There's no pulse animation, because XCUITest waits for animations to finish before it acts.
  - Android check: a debug Server URL that never answers (`http://10.255.255.1:8080`) held the loading state for the 5 s connect timeout. The dump showed `state.loading` with the placeholder rows, then the error screen.
  - iOS: `LaunchTests.testASlowAPIShowsTheLoadingStateUntilTheFlightsArrive` still finds `state.loading` and saves its "O6 loading" screenshot.
- **(e) §13.7 Polish: haptics (QA P7).**
  - Save and Update saved give a firm tap, and Remove a light one, at the tap itself (iOS `UIImpactFeedbackGenerator`, Android `Confirm` / `ContextClick`). A failure message gives an error buzz (iOS `sensoryFeedback(.error)`, Android `Reject`).
  - I first tried triggering on changes to the saved ids. It buzzed at launch, when saved flights load from the database, and watching the save status can miss a fast write's in-progress step. So the feedback is tied to the taps.
  - Simulators and emulators have no haptics, so only P7 on a phone can check it. iOS CI shows it compiles and changes nothing else.
- **(e) §13.1 Review fixes, last part: debug reset race and iOS pull to refresh.**
  - With `-resetData`, the store started reading the database before the reset cleared it, so the previous run's cache could flash in UI tests. Reads now wait for the reset. `aDebugResetNeverShowsThePreviousRunsCache` holds the reset open with `SlowResetRepository`, and fails without the fix; I checked by removing it.
  - iOS `.refreshable` returned at once, so the system spinner went away mid-request. It now awaits the new `refreshAndWait()`, tested by `refreshAndWaitReturnsOnlyOnceTheRefreshHasFinished`.
  - NOTES §5 now has the review summary: each finding, the PR that fixed it, and why the rest weren't changed. Also updated: test counts (133 shared plus 1 JVM-only, 32 XCUITests) and the Android emulator checks. NOTES §2.2 records the route-connector reading.
- **(e) §13.9 COULD: dark mode (QA P3).** §13's SHOULD items were all in PRs by then, and no `qa` issues were open.
  - Android: a dark colour scheme, from `isSystemInDarkTheme()`. Emulator in night mode: dark surfaces, light text, readable status bar, Retry button and Server URL dialog.
  - iOS already used system colours. Debug builds take `-forceDarkMode` (`preferredColorScheme`), and the new `AppearanceTests.testDarkModeScreenshots` saves "P3 dark" screenshots of the list, card, map, filters and Saved in the full run.
- **(e) §13.9 COULD: large text (QA P4).**
  - At accessibility text sizes, iOS rows put the points under the route, and the card's detail rows stack their label over the value (`AnyLayout`). The points label is `fixedSize`, so it never truncates.
  - New `AppearanceTests.testLargestTextScreenshots` launches at the largest standard size (`XXXL`) and the largest accessibility size, and saves screenshots of the list and card in the full run.
  - Android isn't checked here: the emulator has no data for rows. That's QA P4.
- **(e) §13.9 COULD: screen-reader labels (QA P5).**
  - New shared `spokenFlight` and `spokenRoute`, with words instead of "→" and "pts". iOS row buttons and Android `ListItem`s use them as their accessibility label or `contentDescription`, and so do the card titles.
  - `FormattingTest.screenReadersHearWordsNotSymbols` checks the cheapest flight's label, saved and not.
  - iOS UI tests now read the new labels, including one assertion in the smoke test `testOriginAndDestinationCombine`.
  - Android needs data to show rows, so TalkBack is checked by QA P5.
- **(e) §13.9 COULD: LeakCanary (QA T2).** This is the last §13 item; the Python fallback (also §13.9) shipped with the release packaging.
  - `debugImplementation` LeakCanary 2.14, the latest stable (3.0 is alpha). The merged manifest has 15 LeakCanary entries in debug and none in release.
  - Emulator, no API: four rotations plus tab switches. LeakCanary watched 8 destroyed objects (4 activities, 4 `ReportFragment`s) and found none retained.
- **01:23–01:26: the last 11 PRs (#26–#36) merged on green Linux CI, at your call (01:15).**
  - They were stacked in merge order, so #36's branch held all of them. Its full iOS run passed at 01:27 on the tree `main` now has: every XCUITest, the demo and the offline relaunch ([41976342016](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41976342016)).
  - The three per-PR smoke runs still going tested parts of the same code, so I cancelled them.
  - `main`'s own full run on `1545741` ([41980418799](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41980418799)) failed at 01:38: one shared test raced on Kotlin/Native (below).
- **01:33: your answers.** The merge gate in Decisions, no new features, an Android emulator smoke job in Linux CI, and a release rehearsal with `v0.9.0-rc.1`. Then `qa` issues as you file them.
- **01:38: `main` red: `FlightsApiTest` raced on Kotlin/Native.**
  - `theBodyIsDecodedOnTheWorkDispatcherNotTheCallers` (from #22) failed on `assertFalse(work.isRunning)`, after the fetch had returned. It had passed on #22's PR and on #36's full run of the same tree.
  - Cause: its recording dispatcher ran each block on whichever thread dispatched it. On Native, Ktor's engine resumes from its own thread, so the test thread could see the result while that thread was still inside the block. The app code was right.
  - Fix ([#39](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/39)): the dispatcher queues every block on the test's scheduler, so they all run on the test thread, one at a time. Moving the decode out of `withContext(workDispatcher)` still fails the test; I checked by doing it.

## Night: your third list (from 01:33)

Merge gate as in Decisions (Nov 5). `qa` issues come first; then the release rehearsal and the Android emulator job.

| Item | PR | State |
|---|---|---|
| Docs: merge gate, tonight's log in merge order | [#37](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/37) | merged 01:39 |
| Release rehearsal: `v0.9.0-rc.1` as a pre-release | [#38](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/38) (the `release.yml` fix), then the tag | rehearsed 07:05–07:12, then deleted; #38 merged 09:39 |
| Android emulator smoke job in Linux CI | [#40](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/40) | merged 09:39 |

- **Release rehearsal, part 1: `release.yml` would have published `v0.9.0-rc.1` as a full release.** `gh release create` had no `--prerelease`, so the rc would also have become the repo's "Latest" release. A tag with a suffix (anything after a `-`) now publishes as a pre-release; `v1.0.0` still publishes as a normal release.
- **Release rehearsal, part 2: `v0.9.0-rc.1`, 07:05–07:12.**
  - The token still lacks the `workflow` scope, so [#38](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/38) can't merge yet. To rehearse now, I tagged its head (`5bf629c`): `main` plus the fix, with the same app code. `v1.0.0` will be tagged on `main`.
  - The [release run](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/42023928470) was green: both builds, then publish. The release was a pre-release, and the repo had no "Latest" release.
  - All six files downloaded anonymously from their release URLs. `flights.json`, `fallback.py` and `QUICKSTART.md` matched the tagged files byte for byte.
  - The downloaded jar, run from an empty directory with `--mode=normal`: `/flights` returned 200 `application/json`, byte-identical to the fixture (1,000 flights). The downloaded `fallback.py` did the same.
  - The downloaded APK installed on the `roame-smoke` emulator, cold-started in 1.8 s and showed the jar's 1,000 flights through `10.0.2.2:8080`, with no stale banner.
  - The iOS zip holds `RoameStarter.app`: an arm64 Debug build for the iPhone Simulator (SDK 26.4), named "Flight Explorer", with QUICKSTART's bundle id. It wasn't run, because this Mac doesn't run Xcode.
  - The only problem found was the missing pre-release flag (part 1). Then I deleted the release and the tag (`gh release delete --cleanup-tag`); no releases or tags remain.
- **Android emulator smoke job (`android-ui.yml`, [#40](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/40)).**
  - Five Compose UI tests in `androidApp/src/androidTest`, which find controls by the shared `TestIds`. `SmokeTest`: launch loads 1,000; "tokyo" finds 110; a map pill's flight opens its card; a saved flight appears in Saved. Then the API is stopped and `OfflineRelaunchTest` relaunches: the cache is marked stale and the saved flight is still there.
  - The map pills are MapLibre bitmaps, not Compose nodes. The test asks MapLibre for the rendered pill and its projected screen point, then taps there through the input system.
  - `.github/scripts/android-ui-tests.sh` runs `am instrument` twice on one install, so the second run relaunches onto the first run's database; `connectedAndroidTest` would uninstall the app in between.
  - Locally on `roame-smoke` (API 35), against the server jar: all five passed. With the API left running, the offline test failed as it should, and saved its failure screenshot.
  - CI: an API 36 `google_apis` x86_64 image, Pixel 7 profile, android-emulator-runner v2.38.0. MapLibre already used its OpenGL artifact.
  - Getting it green on the runner:
    - Fix 1: a 6 GB data partition. The 57 MB debug APK didn't fit ("Requested internal only, but not enough space").
    - Fix 2: API 37 crashed the system. SurfaceFlinger aborted in the emulator's graphics mapper (`Assertion failed: !rcEnc->featureInfo()->hasReadColorBufferDma`) within seconds of boot, in both runs. I tried turning that host feature off, but the emulator has no feature by that name ("Bad feature name").
    - Fix 3: API 36, the nearest image, as your answer allowed. NOTES §2.2 records why.
- **07:34: `main` green again** on `5d89852` ([#39](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/39)): the full iOS suite ([42023837394](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/42023837394)) and Linux CI.
- **09:39: #38, then #40, merged through the API.**
  - The token still had no `workflow` scope, and GitHub allowed both merges anyway. It refused #10 on Nov 4, which merged a workflow file that `main` had also changed. Probably the difference: these merges only brought in workflow files exactly as the PRs had pushed them.
  - On `31ffcac`, the emulator job's first run on `main` ([42047127064](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/42047127064)) and Linux CI passed. The full iOS run was still going when this was written.

## Day: your fourth list (from 10:55)

A second independent review runs in another chat; its findings arrive as `review` issues, handled like `qa` ones. Meanwhile the Android UI suite grows towards parity with iOS, and the emulator job records an Android demo.

| Item | PR | State |
|---|---|---|
| Android UI tests: filters and search (F1–F8, F10–F12, S1–S6) | [#42](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/42) | merged 11:40 |
| Android UI tests: selection, saved, server states (M5, M6, O1, O3–O5, O7, V2, V4–V6) | this PR | in review |
| Android demo video (`android-demo.mp4`) | | not started |

- **The `qa` and `review` labels didn't exist**, so filing an issue with either label would have failed. I created both. The repo had no issues at all, so none were missed.
- **Token scopes (`gh auth status`, 11:00):** `gist`, `read:org`, `repo`. Still no `workflow`.
- **Android UI tests, part 1: filters and search ([#42](https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/42)).**
  - Debug builds take boolean intent extras, like iOS's launch arguments: `resetData`, `failSaves` and `failRemoves`. `ExplorerViewModel` reads them as `DebugOptions`, and drops `resetData` once used, so an activity recreated after process death doesn't wipe the data again.
  - Every test launches through `ExplorerApp` with `resetData`, so each starts from an empty database and the order doesn't matter.
  - New: `FilterTests` (7 tests) and `SearchTests` (3). QA_CHECKLIST.md now marks each automated item by platform.
  - The date picker is Material's `DateRangePicker`. Its month list tells test tools it can't scroll, so the test scrolls by month and taps the day once it's on screen. One tap and Apply give a single-day range.
  - `OfflineRelaunchTest` now seeds its own cache and saved flight in a run before the API stops (`-e offline before`), because every other test resets the database.
  - Locally on `roame-smoke` (API 35) against the jar: all 16 online tests passed in 67 s, and both offline runs passed. Before the last fix, the date test timed out waiting for 21: it had tapped April 1 while that day was still below the visible part of the list.
  - CI: green, 16 online tests in 117 s; the whole job took 6 minutes.
- **Android UI tests, part 2: selection, saved and server states (this PR).**
  - New: `SelectionTests` (4 tests), `SavedTests` (5) and `ServerStateTests` (3); `OfflineRelaunchTest`'s offline run now also pulls to refresh (O1) and searches and removes offline (O3).
  - The API in CI now runs with `--allow-control`, and `ServerControl.setMode` switches it from the test, as iOS's `ServerControl` does. `ServerModeReset` puts it back after each test.
  - Beyond your list: V5 (a saved flight survives an empty response), O3, and O7 (recreating the activity and backgrounding keep the search and selection), because each was one short test with the new helpers.
  - Locally on `roame-smoke` against the jar: all 28 online tests passed in 103 s, and both offline runs passed.

## Decisions

- **Lighter iOS job on pull requests (Nov 4, evening, your call).**
  - PRs run the build, the shared tests on the simulator and 5 smoke XCUITests: launch, search, map selection, save, error state.
  - Pushes to `main` and workflow_dispatch run the full suite, the demo and the offline relaunch. A red full run on `main` is fixed first.
- **Merge gate (Nov 5, 01:15 and 01:33, your call).**
  - PRs that only touch `androidApp/`, `server/`, `fixtures/` or docs merge on green Linux CI.
  - PRs touching `shared/` or `iosApp/` also need the PR iOS job green. I count the Gradle build files (`gradle/`, `*.gradle.kts`) with them, because they can break the iOS build.
  - Once it exists, the Android emulator job is also required for PRs touching `androidApp/` or `shared/`.
  - A green full iOS run on `main` is required before "final".
  - Everything merges through PRs: no more local merge commits pushed to `main`. CLAUDE.md has the same rules.
- **CI triggers:** added `overnight` to the `push` branches of `ci.yml` and `ios.yml` instead of opening the PR early, so every push gets CI. Reverted before merging (Nov 4, 08:00); PRs and pushes to `main` still run both workflows.
- **First push carries three commits:** setup, version catalog and server. The mandated pre-push check (`:server:test :shared:testAndroidHostTest :androidApp:assembleDebug`) can't pass until `:server` and the shared host-test task exist.
- **Version catalog in step 4, not 5.0:** `gradle/libs.versions.toml` now holds every §3.1 version, and all build files use it. The server was the first module to need new versions.
- **Host tests enabled in step 4:** `withHostTest {}` went in early so `:shared:testAndroidHostTest` exists for the pre-push check. With no test sources it reports NO-SOURCE.
- **Tier skips (`:server`):** the Python fallback server (COULD) isn't built. `--mode=slow` (SHOULD) was built on Nov 4 as item (b). The fallback was built later, with the release packaging (#11).
- **Server bytecode targets JVM 17**, so it also runs on Android Studio's bundled JDK on Windows. `slf4j-simple` is there only so Ktor's startup lines print instead of an SLF4J warning.
- **Working ahead:** while a step's CI runs, I draft the next step in the working tree. It's only committed and pushed once the current step is green.
- **Android tier skips:**
  - Built without these SHOULD/COULD items: camera fit, the offline fallback map style, individually removable chips (chips open the filter sheet instead), −/+ points buttons, skeleton rows, haptics, StrictMode, the Compose stability config, `testTag`s and the "Flight Explorer" app label.
  - Polish items P1 (consistent spacing via one `Spacing` scale) and P2 (pressed and progress states on Save/Remove) are MUST and are built.
  - All of the skipped items were built later, on Nov 4 and 5 (Daytime and Evening sections).
- **Android pull-to-refresh is on the list only.** On the map, a vertical drag would fight panning. The top bar's refresh button works on every screen.
- **Search fields own their text.** Typing never waits on the store, which derives asynchronously. Reset clears both the fields and the store.
- **The iOS demo video is recorded on every iOS CI run**, rather than by a manual `demo.yml`. A `workflow_dispatch` workflow can't be run until it exists on `main`, which this run doesn't touch.
  - CI records the simulator while `DemoWalkthrough` runs (slow-paced: search → filters → map marker → select → save → Saved tab) and while `OfflineRelaunchTests` runs after the API is stopped.
  - The walkthrough also seeds the offline run. The mp4 is in the `ios-ui-tests` artifact.
- **iOS tier choices:** fitting the camera to the selected route (SHOULD) is built, because QA M8 is judged from the screenshot. Not built: re-rendering only the changed slices, haptics, the "Flight Explorer" display name, dark mode and Dynamic Type passes. All of them were built later (Evening section).

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
