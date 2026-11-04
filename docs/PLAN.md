# Flight Explorer — implementation plan

Owner tags: **[MAC]** = work Mac (Claude Code, Opus 5.5, writes all code) · **[WIN]** = Windows laptop (Android Studio, Claude Pro, QA) · **[CI]** = GitHub Actions.

Rule of the project: the Mac never opens Xcode. iOS is built, tested and recorded only in CI.

---

## 0. Timeline (2 days)

| Block | Steps | Done when |
|---|---|---|
| Day 1 AM | 1–3 | Repo exists, starter builds on Android (WIN) and iOS (CI), plan verified |
| Day 1 PM | 4–6 | Server running, all shared logic + persistence + store green in tests |
| Day 2 AM | 7–8 (parallel) | Android UI and iOS UI feature-complete; first QA pass on Android |
| Day 2 PM | 9–12 | QA fixes, iOS UI tests + screenshots green, notes, recording, submit |

---

## 1. Setup the machines

### 1a. [WIN] Windows laptop (~1 h, start immediately; downloads are slow)
1. Install: Git for Windows, GitHub CLI (`winget install GitHub.cli`), Android Studio **Quail 3 or newer**, Claude Code.
2. Android Studio → SDK Manager: Android SDK Platform **37**, Build-Tools **37.0.0**, an emulator system image for API 37 (Pixel, Google APIs, x86_64). Create the AVD.
3. `git config --global core.autocrlf input` (the repo's `.gitattributes` also enforces this).
4. `gh auth login`.
5. Unzip the starter somewhere scratch, open it in Android Studio, run `androidApp` on the emulator → "Hello world!". This is the brief's "both apps build before implementation" check for Android. Note the result for `docs/NOTES.md`.

### 1b. [MAC] Work Mac (~20 min)
1. Check you have: `git`, `gh` (`gh auth login`), a JDK on PATH (`java -version`; Gradle then auto-provisions JDK 25 for the daemon).
2. **Android SDK without Android Studio** — the `:shared` module has an Android target, so Gradle needs the SDK just to configure the project:
   ```bash
   brew install --cask android-commandlinetools
   sdkmanager "platforms;android-37" "build-tools;37.0.0" "platform-tools"
   echo "sdk.dir=$(brew --prefix)/share/android-commandlinetools" > local.properties
   ```
   If IT blocks this, the Mac can still write code, but every compile and test then runs in Linux CI. Expect a slower loop; tell Claude in the kickoff prompt.
3. Do not install or open Xcode.

---

## 2. [MAC] Create the repo (~15 min)

```bash
mkdir ~/dev && cd ~/dev
unzip ~/Downloads/roame-mobile-take-home-starter*.zip
mv roame-mobile-take-home-starter roame-flight-explorer && cd roame-flight-explorer
find . -name .DS_Store -delete
# copy in the handoff files: CLAUDE.md (root) and docs/{PLAN,BRIEF,QA_CHECKLIST}.md
printf '* text=auto\ngradlew text eol=lf\n*.bat text eol=crlf\n*.jar binary\n' > .gitattributes
git init && git add -A && git update-index --chmod=+x gradlew
git commit -m "chore: import starter and planning docs"
gh repo create roame-flight-explorer --private --source=. --push
```

**Public or private repo:** CI on macOS is free for public repos. On private repos it uses up your plan's minutes quickly, because macOS minutes are billed at a multiple of Linux minutes. Pick one:
- Private (safer for a take-home): run the iOS job only on changes to `iosApp/**` or `shared/**`, plus manual dispatch. Each run takes roughly 10–20 min.
- Public: no minute worries. Some companies don't mind; check if unsure.

[WIN] `gh repo clone <you>/roame-flight-explorer` and open it in Android Studio.

---

## 3. [MAC] Plan mode: verify and refine this plan (~45 min)

Open Claude Code in the repo, switch to **plan mode**, and paste prompt **P1** (see §Prompts). Its outputs:
- Verified library versions (§3.1)
- Exact Gradle task names for tests
- Answers to open questions
- Any changes to this plan, written back into `docs/PLAN.md` and committed

Review the plan it produces before approving it.

### 3.1 Libraries to verify (latest version compatible with Kotlin 2.4.21 / AGP 9.3.1 / compileSdk 37)
| Purpose | First choice | Fallback |
|---|---|---|
| Serialization | kotlinx-serialization-json | — |
| Dates | kotlinx-datetime (`LocalDate`) | — |
| HTTP client | Ktor client (OkHttp engine on Android, Darwin on iOS) | — |
| Server | Ktor server (Netty), JVM module `:server` | Python stdlib `server/fallback.py` |
| DB | SQLDelight (Android + native drivers) | Room KMP |
| Coroutines | kotlinx-coroutines | — |
| Swift interop | SKIE (Flow → AsyncSequence, sealed → enum) | KMP-NativeCoroutines |
| Android UI | Compose BOM 2026.09.00 + Material 3, lifecycle-viewmodel-compose | — |
| Android map | MapLibre Android (no API key) + OpenFreeMap style URL | Google Maps Compose (needs key) |
| iOS map | MapKit (SwiftUI `Map`, `MKGeodesicPolyline`) | — |
| Tests | kotlin-test, kotlinx-coroutines-test, Turbine, Ktor MockEngine, SQLDelight JDBC/in-memory driver for JVM tests | — |

---

## 4. [MAC] Local API (`:server`) — ~1 h

**Spec**
- `GET /flights` → 200, body = `fixtures/flights.json` byte-for-byte, `Content-Type: application/json`.
- `GET /health` → 200 `ok`.
- Flags: `--port=8080` (default), `--mode=normal|empty|error|slow`.
  - `empty` → 200 `{"flights":[]}`
  - `error` → 500
  - `slow` → 3 s delay, then normal
- Binds `0.0.0.0` so the emulator (`10.0.2.2`) and the simulator (`localhost`) both reach it.
- Resolves the fixture from the repo root (set `workingDir = rootDir` on the `run` task), with an absolute-path override flag.
- Logs each request: method, path, mode, status.

**One command:** `./gradlew :server:run`. On Windows: `gradlew.bat :server:run`.
**Fallback:** `python3 server/fallback.py [--mode=…]`, zero dependencies, same behaviour.

**Tests:** a Ktor `testApplication` test for each mode, plus one asserting the body equals the fixture.

**Who runs it:** every machine runs its own copy. The Windows laptop runs it for the emulator. CI starts it in the background before iOS tests. A reviewer runs it the same way. The app never bundles a server or the fixture.

Commit: `feat(server): local flights API with failure modes`.

---

## 5. [MAC] Shared domain logic (pure, test-first) — ~2 h

Package `travel.roame.takehome` under `shared/src/commonMain`. Delete `Greeting` after both UIs stop using it.

### 5.1 Models
```kotlin
@Serializable data class FlightsEnvelope(val flights: List<FlightDto>)
@Serializable data class FlightDto(/* exactly the JSON fields */)

data class Flight(
  val id: String, val origin: String, val destination: String,
  val originCity: String, val destinationCity: String,
  val departureDate: LocalDate, val program: String, val points: Int, val cabin: String,
  val originPoint: GeoPoint, val destinationPoint: GeoPoint,
)
```
Cabin and program stay `String`s, with known values derived from data. This tolerates new values from the API. Document the assumption.

### 5.2 Query and filter (pure function)
```kotlin
data class FlightQuery(
  val originText: String = "", val destinationText: String = "",
  val dateRange: ClosedRange<LocalDate>?,   // null = use data bounds
  val cabin: String? = null,                // single-select, null = any
  val maxPoints: Int? = null,               // null = no limit; inclusive
  val programs: Set<String> = emptySet(),   // OR within; empty = all
)
fun filterFlights(flights: List<Flight>, q: FlightQuery): List<Flight>
fun activeFilters(q: FlightQuery, bounds: DataBounds): List<ActiveFilter>  // for chips + reset
```
- Text matching: `trim()`, `lowercase()`, and `contains` against the airport code or the city. An empty string means no constraint.
- Sort results by departure date, then points, then id, so the order is stable.
- `DataBounds` (min/max date, min/max points, cabins, programs) is derived from the cached data and drives the default query and the filter UI.

### 5.3 Map aggregation (pure function)
```kotlin
data class DestinationMarker(val airport: String, val city: String, val point: GeoPoint,
                             val count: Int, val lowestPoints: Int, val flightIds: List<String>)
fun aggregateByDestination(results: List<Flight>): List<DestinationMarker>
```
Group by the **airport code**, not the city.

### 5.4 Tests (`commonTest`). Write these first.
- Search: trim, case, code match, city match, origin+destination AND, Tokyo returns HND+NRT, no match returns empty.
- Dates: start-boundary and end-boundary inclusive, a single-day range, a range outside the data.
- Points: exactly at the limit included, one below excluded, null means everything.
- Cabin filter; programs: one, many (OR), empty (all).
- Combination: AND across types.
- Defaults: the default query returns all 1,000 (load the real fixture in tests).
- Aggregation: counts, lowest points, HND and NRT separate, an empty input, and that it respects the filtered input.
- `activeFilters`: empty at defaults, one chip per active filter.

Commit: `feat(shared): flight model, filtering, search, map aggregation + tests`.

---

## 6. [MAC] Shared data layer and store — ~3 h

### 6.1 Persistence (SQLDelight)
```sql
CREATE TABLE cached_flight (id TEXT PRIMARY KEY, …all fields…);
CREATE TABLE saved_flight  (id TEXT PRIMARY KEY, …all fields…, saved_at INTEGER NOT NULL);
CREATE TABLE sync_meta     (key TEXT PRIMARY KEY, value TEXT);  -- last_success_at
```
- Successful fetch: in **one transaction**, replace all `cached_flight` rows and set `last_success_at`. An empty success also replaces the cache, since it is the latest successful result. Document this.
- Save: `INSERT OR REPLACE` into `saved_flight`. That gives upsert, so there are no duplicates and the details get updated.
- Remove: `DELETE … WHERE id = ?`.
- A refresh **never** touches `saved_flight`. Saved snapshots are refreshed only by an explicit save. Document this.
- All DB calls run on `Dispatchers.IO` (Android) or `Dispatchers.Default` (native). Expose queries as `Flow`.
- Drivers: `AndroidSqliteDriver`, `NativeSqliteDriver`, and a JVM in-memory driver for tests.

### 6.2 Network
- `FlightsApi.fetch(): FlightsEnvelope` uses Ktor with a ~10 s timeout. The base URL is injected.
- Errors map to `RefreshError.Offline | Http(code) | Malformed`.

### 6.3 Repository
```kotlin
interface FlightRepository {
  val cachedFlights: Flow<List<Flight>>
  val savedFlights: Flow<List<SavedFlight>>
  val lastSuccessAt: Flow<Instant?>
  suspend fun refresh(): Result<Int>          // count fetched
  suspend fun save(f: Flight): Result<Unit>
  suspend fun remove(id: String): Result<Unit>
}
```

### 6.4 SyncState
```kotlin
sealed interface SyncState {
  data object Loading : SyncState                         // first load, no cache
  data class Fresh(val at: Instant) : SyncState
  data class Stale(val at: Instant, val error: RefreshError) : SyncState   // has cache, refresh failed
  data class EmptySuccess(val at: Instant) : SyncState    // 200 with zero flights
  data class FailedNoCache(val error: RefreshError) : SyncState            // show error + Retry
}
```
On launch: emit cached data immediately if there is any, then refresh in the background. While refreshing with a cache, keep showing the cache with a small "refreshing" indicator.

### 6.5 FlightExplorerStore (single source of truth for both UIs)
```kotlin
data class ExplorerState(
  val sync: SyncState, val refreshing: Boolean,
  val query: FlightQuery, val bounds: DataBounds?,
  val results: List<Flight>, val resultCount: Int, val activeFilters: List<ActiveFilter>,
  val markers: List<DestinationMarker>, val focusedDestination: String?,   // marker tapped
  val selected: Flight?, val savedIds: Set<String>, val saved: List<SavedFlight>,
  val message: UserMessage?,   // one-shot: "Couldn't save flight", etc.
)
class FlightExplorerStore(repo, scope) {
  val state: StateFlow<ExplorerState>
  fun setOrigin(t: String); fun setDestination(t: String)
  fun setDateRange(r: ClosedRange<LocalDate>); fun setCabin(c: String?)
  fun setMaxPoints(p: Int?); fun toggleProgram(p: String); fun resetFilters()
  fun focusDestination(code: String?); fun select(id: String?)
  fun save(id: String); fun remove(id: String); fun refresh(); fun messageShown()
  fun close()
}
```
- Pipeline: `combine(cachedFlights, query)` → `mapLatest { withContext(Default) { filter + aggregate } }`. Latest query wins.
- Selection rule: store the `selectedId`; derive `selected = results.find { it.id == selectedId }`. If the flight drops out of results, clear the id. Saved-tab selection is separate, so a saved flight opens even when it's filtered out of discovery.
- Save/remove: update `savedIds` optimistically. On `Result.failure`, roll back and emit `message`.
- Debug-only fault hook: a repository decorator that fails saves, toggled from a hidden debug menu. Used for QA item V6 and the demo.
- Factory: `ExplorerFactory.create(baseUrl, driverFactory)` for Swift and Android.

### 6.6 Tests (`commonTest`, using a fake API, Ktor `MockEngine`, an in-memory DB and Turbine)
Persistence round-trip; upsert means no duplicates and updated details; saved flights survive a refresh that doesn't include them; cache survives "restart" (a new store on the same DB file); a failed refresh keeps the cache and gives `Stale`; empty success gives `EmptySuccess`, not a failure; no cache plus a failure gives `FailedNoCache`, then retry succeeds; save failure rolls back and emits a message; selection clears when filtered out and persists otherwise; rapid query changes give the last query's results; a malformed JSON response is handled.

**Before moving on:** spawn a fresh reviewer subagent (prompt **P3**).

Commits: `feat(shared): persistence`, `feat(shared): network + repository`, `feat(shared): explorer store + sync state`.

---

## 7. [MAC] Android UI — ~4 h (parallel with step 8, separate git worktree)

- Manifest: `INTERNET` permission. A network security config allowing cleartext **only** for `10.0.2.2` and `localhost`. Base URL from `BuildConfig`, overridable with a Gradle property.
- `ExplorerViewModel` owns the store; `onCleared()` calls `store.close()`.
- Structure: bottom navigation **Explore | Saved**. Explore has a List/Map segmented toggle sharing one state.
- **Top:** origin and destination fields, a result count, active-filter chips (each removable) and a Reset button. A filter button opens a bottom sheet with a date range picker, cabin chips, a max-points slider (stepped, showing the value), and program multi-select chips.
- **List:** `LazyColumn` with stable `key = id`. A row shows route codes and cities, date, cabin, points and program, plus a saved indicator. Tapping a row selects it.
- **Map:** MapLibre `MapView` in `AndroidView` with proper lifecycle forwarding (avoids leaks). One marker per destination labelled "12 · 25k". Tap a marker to open a sheet listing that destination's flights. Picking a flight selects it, draws the geodesic route (split at the antimeridian) and fits the camera.
- **Flight card:** origin and destination, date, cabin, points, program, and a Save/Remove button with pressed and progress feedback.
- **States:** a skeleton while loading; "No matches, reset filters"; "No flights available" (empty success); full-screen error with Retry (no cache); a stale banner "Offline · showing results from 2:14 PM"; a snackbar for `message`; pull to refresh.
- **Saved tab:** a list of snapshots sorted by `saved_at`, with remove and its own detail card. Works offline.
- `testTag` on every control.

Commit in slices: navigation shell → list + search → filters → map → card + saved → states and polish.

Then **[WIN] QA pass 1** (§9).

---

## 8. [MAC + CI] iOS UI — ~4 h (parallel with step 7)

### 8.1 CI first (`.github/workflows/ios.yml`, macOS runner)
1. Check out the code, set up a JDK (the Xcode build phase calls `./gradlew` and needs `JAVA_HOME`), cache Gradle, and select Xcode 26.4 or newer.
2. `./gradlew :shared:iosSimulatorArm64Test` (shared tests on iOS).
3. Start the server in the background, then wait for `/health`.
4. `xcodebuild build-for-testing` for an iPhone simulator, then `test-without-building` with the UI test target.
5. Offline cases with a real server stop:
   - Run the online UI tests, kill the server, then run `-only-testing:…/OfflineRelaunchTests` on the same simulator. The app data container persists.
   - Restart the server with `--mode=empty` or `--mode=error` and run the matching tests.
6. Upload the `.xcresult`, step screenshots (`XCTAttachment`, lifetime `.keepAlways`) and the server log as artifacts.
7. Trigger on `push` to `iosApp/**`, `shared/**` or `.github/**`, and on `workflow_dispatch`.

### 8.2 App
- Info.plist: `NSAppTransportSecurity → NSAllowsLocalNetworking = YES`; the base URL comes from an Info.plist key set per build config.
- `@main` app creates the store via `ExplorerFactory`. An `@Observable` `ExplorerModel` wrapper consumes `store.state` with `for await` in a `.task`, which cancels automatically, so there's no leak.
- `TabView`: **Explore | Saved**. Explore uses a `Picker(.segmented)` for List/Map sharing one state.
- Top bar: two `TextField`s, the count, filter chips in a horizontal `ScrollView`, and Reset. The filter sheet has `DatePicker` ×2, a cabin `Picker`, a points `Slider` and program toggles.
- List: `List`/`LazyVStack` keyed by id.
- Map: SwiftUI `Map` with an `Annotation` per destination showing count and lowest points. Tapping one opens a sheet of flights. The selected flight draws `MapPolyline(MKGeodesicPolyline)` and adjusts the camera.
- The flight card and all states mirror Android. Show errors as an overlay or banner.
- `accessibilityIdentifier` on every control.

### 8.3 UI tests (XCUITest)
- One test per QA checklist group: Launch, Search, Filters, Map/Selection, Saved, Offline. Each takes screenshots at key steps.
- `DemoWalkthrough`: a slow-paced test (1.5–2 s pause per step) covering search → filters → map marker → select → save → saved tab. The demo workflow (`demo.yml`, `workflow_dispatch`) wraps it with `xcrun simctl io booted recordVideo --codec=h264 ios-demo.mp4` and a server stop and relaunch at the end. It uploads `ios-demo.mp4`.

### 8.4 Review loop on the Mac
Claude downloads the screenshot artifacts (`gh run download`), looks at each one, fixes layout and spacing problems, and pushes. It writes results to `docs/QA_IOS.md`.

---

## 9. [WIN] Android QA passes

The Windows laptop does this for each QA pass (after step 7, then after fixes):
1. `git pull`
2. Terminal 1: `gradlew.bat :server:run`. Allow Java through Windows Firewall (private networks) if prompted.
3. Run `androidApp` from Android Studio on the emulator.
4. Work through `docs/QA_CHECKLIST.md` with prompt **P6**. Use `--mode=empty`, `--mode=error` and `--mode=slow` restarts where the checklist says to.
5. For each failure, use `gh issue create --label qa` with steps, expected vs actual, a screenshot and a logcat excerpt.
6. Commit `docs/QA_ANDROID.md` with the results and push. This file is the "checks you actually performed" evidence.

[MAC] picks up the issues with prompt **P5**.

---

## 10. [MAC] Hardening and docs — ~2 h
- Performance: no allocations in hot composables, stable keys, `derivedStateOf` where needed; check for recompositions. iOS: avoid re-creating view models; check that filtering runs off the main thread.
- Memory: store closed in `onCleared` and the iOS task cancelled. Run a LeakCanary debug build once, via WIN.
- Write `README.md`: prerequisites, the single server command, Android run steps, iOS run steps (for a reviewer with a Mac), test commands, base-URL overrides, a troubleshooting section (emulator networking, `NSAllowsLocalNetworking`, first-build "No such module 'Shared'").
- Write `docs/NOTES.md` (template in §12).
- Final reviewer subagent pass against `docs/BRIEF.md` (prompt **P3**, scoped to the whole repo).

---

## 11. Screen recording

The brief requires showing both apps: search, filters, map selection, saving flights, and reopening the app with the API stopped.

- **Android [WIN]:** use the emulator's built-in recorder (Extended controls → Record and playback), or OBS for longer takes. Script:
  1. Launch, showing loading then 1,000 results.
  2. Origin "new york", destination "tokyo" (HND and NRT).
  3. Filters: dates, cabin, points, two programs. Show the chips, then Reset.
  4. Map: pan and zoom, tap a marker, pick a flight, show the route and card.
  5. Save. Switch to the list to show the selection kept. Open the Saved tab.
  6. Stop the server (Ctrl+C), force-quit the app, reopen: cached data, stale banner, search works, saved flights visible, remove one.
  7. Optional: clear app data with the server stopped to show the error and Retry.
- **iOS [CI]:** run the `demo` workflow, then download `ios-demo.mp4`.
- **Combine:** edit both clips together in Clipchamp (built into Windows). Add short title cards: "Android (Compose)" and "iOS (SwiftUI), recorded on CI simulator". Keep it to 5 min or less.

---

## 12. Submission
- `docs/NOTES.md` sections:
  1. **Approach and architecture**: shared store as single source of truth; what lives in shared vs native; library choices and why.
  2. **Assumptions** (all of these get logged as you go):
     - cabin is single-select
     - search uses substring match on code or city
     - an empty success replaces the cache
     - saved snapshots update only on explicit save
     - dates are local calendar dates, no timezones
     - "max points" defaults to no limit
     - saved-tab selection is independent of discovery filters
     - map tiles need network
  3. **Tradeoffs**: MapLibre vs Google Maps (no key); SQLDelight vs Room; iOS verified via CI simulator plus XCUITest rather than interactively.
  4. **Known limitations.**
  5. **Checks actually performed**: link `QA_ANDROID.md`, `QA_IOS.md`, the CI runs, and the test list.
  6. **How AI was used and verified**: Claude Code (Opus 5.5) on the Mac planned and implemented the work, with an independent reviewer subagent at each phase. Claude Pro on Windows assisted QA triage. Everything was checked by requirement-to-test mapping, CI, manual Android QA and screenshot review on iOS. List concrete things you rejected or corrected.
- Final checks: a clean clone on WIN, `gradlew.bat :server:run`, then the app runs; the CI badge is green.
- Submit the repo link (add reviewers if private) or a ZIP made with `git archive`, plus the video.
- **Before submitting, read every diff yourself** and be ready to change any part live.

---

## Prompts

### P1 — [MAC] plan mode kickoff (Opus 5.5, max effort)
```
Read CLAUDE.md, docs/BRIEF.md, docs/PLAN.md, docs/QA_CHECKLIST.md, the entire starter, and analyse
fixtures/flights.json (value sets, ranges, edge cases).

Verify and refine docs/PLAN.md. Specifically:
1. For every library in §3.1, find the latest version compatible with Kotlin 2.4.21, AGP 9.3.1
   (com.android.kotlin.multiplatform.library plugin), Gradle 9.7.0, compileSdk 37, Swift 6/Xcode 26.4.
   Say how you verified each (release notes, compatibility matrix). Pick the fallback where needed.
2. Find the exact Gradle task names for shared JVM/Android-host tests and iOS simulator tests with
   this plugin setup, and write them into CLAUDE.md.
3. Confirm the shared public API in §5–6 is clean to consume from Swift via SKIE; adjust signatures if not.
4. Check every bullet in docs/BRIEF.md maps to a step, a test, and a QA checklist item. List any gaps.
5. Challenge the assumptions in §12; flag any you'd change.
6. Note: I will not run Xcode locally. [Android SDK cmdline tools: installed / NOT available — edit this.]
Output the revised plan. Don't write code yet.
```
After approval: "Write the revised plan into docs/PLAN.md and CLAUDE.md, commit, push."

### P2 — [MAC] implement a step
```
Implement step <N> of docs/PLAN.md. Tests first where the step lists them. Run the relevant Gradle
tests until green and make sure ./gradlew :androidApp:assembleDebug still builds. Log new assumptions
in docs/NOTES.md. Commit in the logical slices the step lists, push, and run `gh run watch` until CI is
green; fix failures from `gh run view --log-failed`. Then run P3.
```

### P3 — [MAC] independent review (subagent)
```
Spawn a fresh subagent that has not seen this work. Give it only docs/BRIEF.md, CLAUDE.md and
`git diff <base>..HEAD`. Ask it to report: requirements missed or half-done, tests that don't actually
test the boundary they claim, main-thread blocking, races (stale query results, selection), leaks,
and error paths that could fail silently. It must cite file:line. Then fix every valid finding, add tests
for the ones that were bugs, and summarise what you changed and what you rejected and why.
```

### P4 — [MAC] Android and iOS UI in parallel
```
Create two git worktrees: ui-android and ui-ios. Run step 7 in one subagent and step 8 in another,
both consuming the shared store unchanged. If either needs a shared API change, stop, make it on main
with tests, and rebase both. iOS: never run Xcode locally; CI is the only iOS build machine
(push → gh run watch → gh run view --log-failed → fix). When both are done, merge to main,
make sure all CI is green, then download iOS screenshot artifacts, review them against the brief, fix, and
write docs/QA_IOS.md.
```

### P5 — [MAC] fix QA issues
```
gh issue list --label qa --state open. For each: reproduce with a failing shared test if it's a logic bug
(UI-only bugs: explain the cause), fix, commit with "fixes #N", push, and comment on the issue with what
changed and what to re-test. Make sure CI is green at the end.
```

### P6 — [WIN] QA assistant (Claude Pro: use Sonnet, keep sessions short)
```
You are the QA assistant for this repo. You do NOT write or edit app code. I'm on Windows: use gradlew.bat
and PowerShell syntax. Read CLAUDE.md and docs/QA_CHECKLIST.md.
Walk me through the checklist one item at a time on the Android emulator. Tell me exactly what to do,
I'll report what I see. Record each result in docs/QA_ANDROID.md as "ID | pass/fail | commit | note".
For each failure, draft and create a GitHub issue with gh issue create --label qa: title, steps, expected
vs actual, and the logcat lines I paste. When done, commit and push docs/QA_ANDROID.md only.
```

### P7 — [WIN] something won't build or run
```
<paste error>. Diagnose in at most 5 bullet points. If it's an environment issue on this machine
(SDK, emulator, firewall, networking to 10.0.2.2:8080), give me the fix. If it's a code bug, draft a
GitHub issue instead of editing code.
```
