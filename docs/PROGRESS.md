# Overnight run log (Nov 4, 2026)

Branch `overnight`, cut from `main` at `6f91c42`. Scope: `docs/PLAN.md` steps 4–8, MUST-tier items only, one step at a time. Times are local (UTC-5).

## Status

| Step | State | Last green CI |
|---|---|---|
| 4. Local API (`:server`) | done 00:56 | [ci](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41781065144) · [ios](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41781065124) |
| 5. Shared domain | done 01:10 | [ci](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41782370174) · [ios](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41782370193) |
| 6. Data layer + store | in progress | — |
| 7. Android UI | pending | — |
| 8. iOS UI | pending | — |

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

## Decisions

- **CI triggers:** added `overnight` to the `push` branches of `ci.yml` and `ios.yml` instead of opening the PR early, so every push gets CI. Generalize or revert this at merge time.
- **First push carries three commits:** setup, version catalog and server. The mandated pre-push check (`:server:test :shared:testAndroidHostTest :androidApp:assembleDebug`) can't pass until `:server` and the shared host-test task exist.
- **Version catalog in step 4, not 5.0:** `gradle/libs.versions.toml` now holds every §3.1 version, and all build files use it. The server was the first module to need new versions.
- **Host tests enabled in step 4:** `withHostTest {}` went in early so `:shared:testAndroidHostTest` exists for the pre-push check. With no test sources it reports NO-SOURCE.
- **Tier skips (`:server`):** `--mode=slow` (SHOULD) and the Python fallback server (COULD) aren't built. `CLAUDE.md` still lists `slow`, but the server rejects it.
- **Server bytecode targets JVM 17**, so it also runs on Android Studio's bundled JDK on Windows. `slf4j-simple` is there only so Ktor's startup lines print instead of an SLF4J warning.
- **Working ahead:** while a step's CI runs, I draft the next step in the working tree. It's only committed and pushed once the current step is green.

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
