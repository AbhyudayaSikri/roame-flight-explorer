# AI facts

Source material for NOTES §6, which you write. Facts only, each with its PR, run or commit. Times are EST (UTC−5). "The agent" is Cursor's agent on the work Mac. Counts are as of [#46]'s merge on Nov 5.

## Machines, models and tools

- **Planning, before Nov 3:** a separate Claude chat you ran (the planning agent) wrote `PLAN.md`. Model: _fill in_. It was imported with the starter, `BRIEF.md`, `CLAUDE.md` and `QA_CHECKLIST.md` on Nov 3, 21:23 ([ed2d6a7]).
- **Work Mac, all code, commits and PRs:**
  - Cursor's agent, Claude Opus 5.5, in one chat from Nov 3 to now. `PLAN.md` (line 3) names Claude Code (Opus 5.5) for this machine.
  - The chat's context was summarized several times. On Nov 5 at 01:15, the agent started a turn without the earlier context and rebuilt it from its own transcript and the repo.
  - One subagent, the independent review (PLAN P3): Nov 4, 22:51–23:15, on a read-only checkout of `857090e`. Its only context was `BRIEF.md`, `CLAUDE.md` and the diff since the starter (106 files, about 7,700 added lines). Model: not recorded in its transcript.
  - Tools the agent ran:
    - git, and the GitHub CLI. The token's scopes are `gist`, `read:org` and `repo`, with no `workflow`.
    - Gradle, JVM and Android tasks only.
    - XcodeGen. The project file is committed; CI regenerates it and fails on any difference.
    - The Android emulator, `adb`, `uiautomator dump` and `screenrecord`, on the AVD `roame-smoke` (API 35, arm64), which the agent created on Nov 4.
    - ffmpeg, for frames from screen recordings.
    - Python scripts, for the fixture analysis and byte comparisons.
    - curl.
  - Never run on the Mac (CLAUDE.md): Xcode, `xcodebuild`, iOS Gradle tasks. iOS was built and tested only in GitHub Actions.
  - Recorded blocks by Cursor's command auto-review:
    - Starting the API overnight on Nov 4 (step 7). No Android check with data ran on the Mac that night.
    - A local emulator run on Nov 5, which then ran after your approval.
- **Windows laptop:** Android Studio and its emulator for hand QA (PLAN §9), with Claude Pro (Sonnet) for QA triage (PLAN P6). No results filed yet: no `qa` issues, and no `docs/QA_ANDROID.md`.
- **GitHub Actions:**
  - `ci.yml` (`ubuntu-24.04`): server tests, shared JVM tests, the Android build.
  - `ios.yml` (`macos-26`, Xcode 26.4.1, iPhone 17 simulator on iOS 26.4): shared tests on the simulator, then XCUITests.
  - `android-ui.yml` (`ubuntu-24.04`, from [#40]): Compose UI tests against the real API. API 36 `google_apis` x86_64 emulator, Pixel 7 profile, android-emulator-runner v2.38.0.
  - `release.yml` (both runners, from [#11]): publishes a release when a `v*` tag is pushed.
- **Second independent review:** run by you, in another chat with a different model, over `857090e..main`. Findings arrive as `review` issues. None filed yet.
- **Relay:** on Nov 5 the agent wrote status updates for the planning agent at 01:15, 10:52 and 12:14. Your "Answers" messages followed at 01:33, 10:55 and 12:22.

## Workflow, in order

- **Nov 3, 21:23:** starter and planning docs imported ([ed2d6a7]).
- **Nov 3, to 22:50: plan verification by the agent** ([382e711]; NOTES appendices A and B):
  - Every library version was checked against Maven Central and Google Maven metadata, release notes, AAR metadata and the runner images.
  - Task names came from `./gradlew :shared:tasks --all`.
  - The fixture was analysed by script.
  - Your decisions after reading it: MUST/SHOULD/COULD tiers with a MUST gate; XcodeGen; a public repo during development; the search rule (NOTES §2.1).
- **Nov 3, 23:42 to Nov 4, 00:34: CI skeleton** ([a008759] to [6f91c42]).
  - Both starters built as provided.
  - XcodeGen, the SKIE spike and the persistence probe were each accepted after a green run.
- **Nov 4, 00:40–04:30: overnight run.** PLAN steps 4–8 (MUST tier) on branch `overnight`, with a self-review after step 6. [#1] was opened as a draft at 03:27 and merged at 08:26.
- **Nov 4, 09:31–21:26:** your daytime list, [#2]–[#6].
- **Nov 4, 21:36 to Nov 5, 01:26:** your evening list, [#7]–[#36]. The review subagent ran 22:51–23:15. Its findings were fixed in [#22]–[#27] and [#32].
- **Nov 5, 01:33–09:46:** your night list, [#37]–[#41].
- **Nov 5, 10:55–12:07:** your fourth list, [#42]–[#45].
- **Nov 5, from 12:22:** your fifth list, [#46] and this file.
- **Each item:**
  1. Its own branch, some stacked on the previous item's.
  2. The pre-push check: `:server:test :shared:testAndroidHostTest :androidApp:assembleDebug`, plus `:androidApp:assembleDebugAndroidTest` from [#40].
  3. A PR, then CI.
  4. A merge through the GitHub API, except [#10] and [#11], which were merged by git.
- **Merge gate:**
  - Nov 4, daytime: each PR's own Linux run and full iOS run.
  - Nov 4, evening ([#8], your call): PRs run 5 smoke XCUITests. Pushes to `main` and manual runs run the full suite.
  - Nov 5, 01:15 and 01:33 (your call):
    - Linux CI is enough for PRs that only touch `androidApp/`, `server/`, `fixtures/` or docs.
    - PRs touching `shared/` or `iosApp/` also need the PR iOS job. The agent added the Gradle build files to this group.
    - PRs touching `androidApp/` or `shared/` also need the emulator job, from [#40].
    - A green full iOS run on `main` is required before "final".
- **Evidence CI keeps:** each full iOS run uploads `ios-ui-tests` (screenshots, the `.xcresult` bundle and `demo.mp4`). Each emulator run uploads `android-demo.mp4`, from [#44].

## Counts

- **PRs:** [#1]–[#46], all merged. 44 went through the GitHub API, and 2 were merged by git and pushed to `main` ([#10], [#11]).
- **Commits on `main`:** 174.
  - The first is [ed2d6a7] (Nov 3, 21:23).
  - 130 are local commits under the git identity `Abhyudaya.Sikri <abhyudaya.sikri@textnow.com>`.
  - 44 are GitHub merge commits.
- **Tests:**
  - Shared (`shared/src/commonTest`): 137. They run on the JVM in Linux CI and on the iOS simulator in `ios.yml`. One more, `FileDatabaseRestartTest`, runs on the JVM only.
  - Server: 19 Kotlin tests (`server/src/test`), and 12 Python tests for the fallback server (`server/test_fallback.py`).
  - iOS XCUITest: 34 in 10 classes, including `DemoWalkthrough` and `OfflineRelaunchTests`. PRs run 5 of them.
  - Android Compose UI: 29 in 9 classes. 27 run in the online pass; `DemoWalkthrough` and `OfflineRelaunchTest` each run in their own pass.
  - Earlier totals:
    - Shared: 46 after step 5, 87 after step 6, 133 at [#32].
    - XCUITest: 23 overnight, 30 at [#19], 32 at [#32].
    - Android UI: 5 at [#40], 16 at [#42], 28 at [#43].
- **QA checklist** (61 items): 30 are automated on both apps, 6 on iOS only, 4 on Android only and 1 in Linux CI (A1). 20 are automated on neither.
- **CI runs, all time, at #46's merge (12:50):**
  - `ci.yml`: 120 (101 passed, 18 cancelled, none failed, 1 running on `main`).
  - `ios.yml`: 116 (56 passed, 16 failed, 43 cancelled, 1 running on `main`). 5 of the 16 failures were GitHub not assigning a runner.
  - `android-ui.yml`: 14 (10 passed, 3 failed, 1 running on `main`).
  - `release.yml`: 6, all passed.
- **Run times** (last 30 green runs, queued to finished): `ci.yml` 1–3 min, `ios.yml` 13–53 min, `android-ui.yml` 4–7 min, `release.yml` 2–13 min.

## Bugs and failures caught, and how each was confirmed

### By CI

- **Map markers off-screen, and a slider test stopping short (overnight).** In [run 41788560793], 3 map XCUITests and 1 slider test failed.
  - Confirmed from the failure's UI hierarchy: the markers were there, with the right ids. The fixed starting region covered only Europe and Africa, so Tokyo was off-screen.
  - `adjust(toNormalizedSliderPosition: 0)` stopped at about 20,000 points.
  - Fixed on `overnight` ([#1]): the map frames its markers and the selected route, and the slider test drags past the end. Green in [run 41792526988].
- **V2's "No flight card" on [#1]** ([run 41799697121]).
  - Confirmed from the UI hierarchy: the 3 JFK → HND rows were showing at the new prices, with nothing selected. The test had tapped a row before the "hnd" search finished, while a refresh landed.
  - Fixed in the test: each select waits for its search's count. No assertion changed. Green in [run 41803662017].
- **The keyboard-shortened Tokyo map** (`testMarkersShowCountAndLowestPointsPerAirport`). After the search narrowed to Tokyo, HND and NRT stayed one merged pill.
  - Failed in [run 41881579267], [run 41924340854], [run 41924418166] and [run 41947873526], then on `main` in [run 41955053011], and on [#7] in [run 41957059552].
  - Confirmed:
    - Every failure's UI hierarchy had the merged pill at the same screen position.
    - The XCTest screen recording (frames pulled with ffmpeg) showed the camera framing Tokyo while the keyboard shortened the map, and keeping that camera when the map grew.
  - App behaviour, not only the test's: searching from the map left it zoomed out.
  - [#6] made three attempts: a zoom estimate on camera moves, then re-framing on resize, then re-fitting through the region. The third passed on its PRs ([run 41951444932], [run 41951417332]), then failed on `main`.
  - [#9] lets the map run under the keyboard. Two full runs with it passed before merging ([run 41957685895], [run 41958388873]). The test hasn't failed since.
- **The F1 test artifact.** In [run 41852391748], `testASingleDayRangeIncludesThatDay` expected "Show 17 flights" and found "Show 10 flights".
  - Dropped at your 45-minute timebox, then investigated in the evening ([#7]).
  - Confirmed from recording frames:
    - The picker set Feb 1 → Feb 1, and the sheet read 17.
    - The calendar then closed by itself, and the test's "dismiss" tap landed on the Economy row. Feb 1 in Economy is 10 flights in the fixture.
  - [#7] moved the conversion into shared code, with `PickerDateTest` in ten time zones. iOS has no XCUITest for F1 or F2; Android's tests for both are in [#42].
- **Screenshot timeouts.** [run 41957059552] ([#7]) failed with "Failed to get screenshot: Timed out while requesting screenshot", while five macOS jobs were running. [#12] makes evidence screenshots non-fatal; assertions are unchanged.
- **The demo walkthrough lost keyboard focus** on [#9]'s dispatched run ([run 41957681937]).
  - "Neither element nor any descendant has keyboard focus" while typing into `search.destination`. The offline run then failed for lack of its seed. Every other UI test in that run passed.
  - Not investigated or logged at the time. It hasn't recurred in any later run.
- **`FlightsApiTest` raced on Kotlin/Native.** In `main`'s full run [run 41980418799] (Nov 5, 01:38), `theBodyIsDecodedOnTheWorkDispatcherNotTheCallers` (from [#22]) failed `assertFalse(work.isRunning)`.
  - It had passed on [#22]'s PR, and in [#36]'s full run of the same tree ([run 41976342016]).
  - Cause (PROGRESS): the test's dispatcher ran each block on the thread that dispatched it, and on Native, Ktor's engine resumes from its own thread.
  - Fix, test code only: [#39] queues every block on the test scheduler. Moving the decode out of `withContext(workDispatcher)` still fails the test (checked).
  - Green in [run 42023837394] (07:34). `main` was red from 01:38 to 07:34. No commits landed on any branch from 01:41 to 06:27; the cause wasn't recorded.
- **The emulator job on API 37** ([#40]):
  - "Requested internal only, but not enough space" while installing the 57 MB debug APK ([run 42023738212]). Fix: a 6 GB data partition.
  - [run 42024450163] and [run 42026005109]: SurfaceFlinger aborted seconds after boot (`Assertion failed: !rcEnc->featureInfo()->hasReadColorBufferDma`).
  - The second run carried the agent's `-feature -ReadColorBufferDma`, which the emulator rejected: "Bad feature name: 'ReadColorBufferDma'".
  - Switched to API 36, the nearest image, as your rule allowed. Green since.
- **GitHub runner outage** (Nov 4, about 10:00–17:20). "The job was not acquired by Runner of type hosted even after multiple attempts" in [run 41850754688], [run 41853828307], [run 41853912750], [run 41869605322] and [run 41875688814]. githubstatus.com posted no incident.
- **Merge refused for lack of the `workflow` scope.** GitHub refused to merge [#10] through the API: the token has no `workflow` scope, and #10 changed a workflow file that `main` had also changed. At your call, [#10] and [#11] were merged by git. [#38] and [#40] later merged through the API without the scope.

### By the independent review subagent (Nov 4, on `857090e`)

It reported 2 bugs, 4 risks, 4 sets of tests that checked less than their names said, and nits (NOTES §5).

- **Bug: the flights response was decoded on the main thread.** [#22] runs the fetch on `Dispatchers.Default`. The new test fails when the decode moves back (checked).
- **Bug: after process death, the Android search fields could show a search the store wasn't running.** Fixed in [#25].
  - Confirmed on the emulator: type `jfk`, press Home, `am kill` the process, relaunch.
  - `main`'s build came back with "jfk" in the field, no chip, and all flights. The branch came back with an empty field.
- **Risk: "no cache" was shown before the cache was read, and a first launch could flash "no flights".** Fixed in [#23]. Its two new store tests fail on the old code (checked).
- **Nit: the stale banner disappeared during a retry.** Fixed in [#23].
- **Risk: the points slider could pull back during a drag.** Fixed in [#27]. The iOS slider test ran in the full suite on the branch.
- **Tests that checked less than their names:**
  - Fixed in [#24]: latest-query-wins (it fails if `mapLatest` becomes `map`), a bad response over a cache, and a restart with a real database file.
  - Fixed in [#26]: two XCUITests that passed before their action.
- **Nits:**
  - List/Map mode reset after a visit to Saved ([#25]).
  - A debug reset could flash the previous run's cache ([#32]). Its test fails without the fix (checked).
  - iOS pull to refresh let go of its spinner early ([#32]).
- **Not changed** (reasons in NOTES §5):
  - The blank Android map without a style, already fixed by [#14].
  - `OfflineRelaunchTests` checking an end state.
  - A debug-only server-switch race.
  - Two drifts from CLAUDE.md.
- The agent's step-6 self-review had checked "Main thread" and left it alone (PROGRESS, "Step 6 review findings").

### By the agent's review of CI screenshots and recordings (PLAN 8.4)

- In [run 41788560793]:
  - The search placeholder was truncated. It now reads "From" / "To".
  - "0 flights" showed above the error screen. The count is now hidden while there's no data.
- In [run 41792526988], a single-airport result zoomed to street level. Fixed with a regional view, confirmed in [run 41796168173]'s screenshots.
- "Premium Economy" truncated in the segmented cabin picker. Left as is overnight; [#4] replaced the picker with cabin rows.
- The Tokyo map and F1 diagnoses above came from recording frames.

### By the agent's step-6 self-review (overnight, in [#1])

Each was fixed with a named test (PROGRESS, "Step 6 review findings"):
- Storage errors stayed invisible while a read kept failing.
- `close()` raced in-flight writes.
- A second failure message was dropped while the first was showing.
- Later storage failures posted no message.
- A failed debug reset was ignored.
- Untested boundaries: `syncStateOf`, the saved-flights overlay and `saveStatusOf`.

The first local test run also caught the store test harness running inside `backgroundScope`: 21 of 23 store tests saw only the initial state. A probe test confirmed the cause.

### By the release rehearsal (Nov 5, 07:05–07:12)

- **`release.yml` had no `--prerelease`** (written in [#11]), so `v0.9.0-rc.1` would have become a full "Latest" release. Found while preparing the rehearsal. Fixed in [#38].
- The rehearsal ([run 42023928470]) used a tag on [#38]'s head, `5bf629c`:
  - It published a pre-release, and the repo had no "Latest" release.
  - All 6 assets downloaded anonymously. The fixture, `fallback.py` and `QUICKSTART.md` matched the tagged files byte for byte.
  - The jar, run from an empty directory, served the fixture byte for byte. So did `fallback.py`.
  - The APK showed the jar's 1,000 flights on `roame-smoke`.
  - The iOS zip was inspected (an arm64 simulator build named "Flight Explorer") but not run.
  - Then the release and the tag were deleted.

### By the agent's local emulator runs

- [#42], before merge: the date test tapped April 1 while that day was below the visible part of the list, and timed out waiting for 21.
- [#44], before merge: the demo's HND pill was never drawn, because Android's fixed world view left Japan off-screen. The demo used Paris instead; [#46] then added the framing.
- In [#46], with the zoom cap set to 6, both new marker tests fail (checked). In the local walkthrough video, the frame at 28 s shows the HND and NRT pills over Tokyo.

## Agent errors, your overrides, agent choices

### Agent errors

- Overnight, caught by CI or the agent's screenshot review (details above):
  - The fixed map region.
  - The slider test.
  - The truncated placeholder.
  - "0 flights" above the error screen.
  - Street-level zoom for one airport.
  - V2's unsynchronized tap.
- [#6] tried three fixes: the first two didn't hold, and the third passed on its PRs but failed on `main`. [#9] fixed it.
- The F1 XCUITest's dismiss tap changed the cabin ([run 41852391748]).
- The step-6 self-review passed "Main thread"; the review subagent found the decode on the main thread ([#22]).
- [#22]'s test raced on Kotlin/Native, and `main` stayed red from 01:38 to 07:34 on Nov 5 ([#39]).
- `release.yml` ([#11]) published every tag as a full release ([#38]).
- The emulator flag `-feature -ReadColorBufferDma` doesn't exist ([run 42026005109]).
- [#42]'s date test and [#44]'s demo, both fixed before merge (above).
- The first haptics design fired on changes to the saved ids, so it buzzed at launch. It now fires on the taps ([#31]).
- Docs:
  - `PROGRESS.md` merge times were corrected in [#17] ("docs(progress): actual merge times").
  - [#1]'s opening time and the merge times of [#2], [#3], [#4] and [#6] were still 5–29 minutes off GitHub's. Corrected with this file.
  - From [#42] to [#46], `PROGRESS.md` gave totals as "online tests", but each included the 2 tests that skip themselves in that pass. Corrected with this file.

### Your overrides and calls

- **Nov 3, after plan verification:** tiers with a MUST gate, XcodeGen, a public repo, and the search rule.
- **Nov 4, daytime:**
  - The order (a)–(d).
  - A 45-minute timebox for the F1 XCUITest, which was dropped.
  - The local API stays off on the Mac, so Android's route camera fit wasn't checked visually.
  - [#2] and [#4] merged on a dispatched run's evidence ([run 41852391748]) plus their Linux checks.
- **Nov 4, evening:** the lighter iOS job on PRs ([#8]); [#10] and [#11] merged by git.
- **Nov 5, 01:15:** [#26]–[#36] merged on Linux CI, with [#36]'s full run ([run 41976342016]) as the evidence.
- **Nov 5, 01:33:**
  - The merge gate, and everything through PRs.
  - No new features.
  - The emulator job: API 37 or the nearest image, advisory after 3 genuine fixes.
  - The rc rehearsal.
- **Nov 5, 10:55:**
  - The second review runs in your chat with a different model; the agent had offered to rerun P3 itself.
  - The Android UI parity list.
  - You record the Android video by hand.
- **Nov 5, 12:22:**
  - Android map framing fixed for parity ([#46]). The agent had listed it as a known limitation in NOTES §4 since [a997530] (Nov 4, in [#5]).
  - This file, instead of the agent writing §6.
- **Standing rules:** no Xcode, `xcodebuild` or iOS Gradle tasks on the Mac; no force-push; no repo settings changes; no loosened, deleted or skipped tests; §6 is yours.

### Agent choices made without an instruction (recorded in PROGRESS or NOTES)

- Tagged `v0.9.0-rc.1` on [#38]'s head, not on `main`, because #38 couldn't merge yet without the `workflow` scope.
- API 36 for the emulator job after the API 37 runs, within your "nearest image" rule.
- Counted the Gradle build files with `shared/` and `iosApp/` in the iOS gate.
- Android tests for V5, O3 and O7, beyond your list ([#43]).
- The iOS demo video is recorded on every full CI run, not by a manual `demo.yml`.
- Overnight: added `overnight` to the CI push triggers instead of opening the PR early. Reverted before merging.

## Checked only by automation or by the agent

- **iOS:** never run by a person. It ran only on the CI simulator, never on a device (NOTES §4). `docs/QA_IOS.md` was last reviewed against [run 41796168173] (Nov 4, `f3fe6ff`), before [#2]–[#46].
- **Android:** no hand QA results yet. Everything so far comes from the emulator tests (API 35 locally, API 36 in CI) and the agent's own emulator checks.
- **QA items automated on both apps (30):** L1, L2, S1–S6, F3, F4, F5, F7, F8, F10, F11, F12, M3, M5, M6, V1, V2, V4, V5, V6, O1–O5, O7.
- **Automated on iOS only (6):** M1, M4, M7, V3, O6, T3. On Android, no test asserts them, and no hand QA has been filed.
- **Automated on Android only (4):** F1, F2, F6, M11 (the Tokyo part). iOS has no XCUITest for them; F1 and F2 are covered there by the shared `PickerDateTest`.
- **Automated nowhere (20):** A2, L3, L4, F9, M2, M8, M9, M10, O8, O9, P1–P7, T1, T2, T4. Of these:
  - Checked by the agent only:
    - iOS, from CI screenshots: M8 and M9 ([run 41792526988], [run 41796168173]).
    - Android, on the emulator without data: O9 (process death), T4 (`uiautomator dump`), P3 (night mode), T1 (the StrictMode log), T2 (LeakCanary), L4 (skeleton rows behind an unreachable URL).
  - Not checked by anyone yet:
    - P7 haptics, which needs a phone.
    - A2 on Windows.
    - O8, P4 and P5 on Android.
    - Smoothness (L3, M2, F9) on both apps.
- **Release:** checked only by the agent's rc rehearsal. `v1.0.0` isn't tagged, and the iOS zip has never been run.
- **Tests checked to fail on the old code**, by the agent: [#22], [#23], [#24], [#29], [#32], [#39], [#46].
- **The fixture on Windows:** LF line endings were verified on a Mac clone with `core.autocrlf=true` ([#20]), not on Windows.
- **Pending:** the second review and the Windows QA pass.

[#1]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/1
[#2]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/2
[#3]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/3
[#4]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/4
[#5]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/5
[#6]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/6
[#7]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/7
[#8]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/8
[#9]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/9
[#10]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/10
[#11]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/11
[#12]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/12
[#14]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/14
[#17]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/17
[#19]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/19
[#20]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/20
[#22]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/22
[#23]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/23
[#24]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/24
[#25]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/25
[#26]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/26
[#27]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/27
[#29]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/29
[#31]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/31
[#32]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/32
[#36]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/36
[#37]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/37
[#38]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/38
[#39]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/39
[#40]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/40
[#41]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/41
[#42]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/42
[#43]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/43
[#44]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/44
[#45]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/45
[#46]: https://github.com/AbhyudayaSikri/roame-flight-explorer/pull/46
[ed2d6a7]: https://github.com/AbhyudayaSikri/roame-flight-explorer/commit/ed2d6a7
[382e711]: https://github.com/AbhyudayaSikri/roame-flight-explorer/commit/382e711
[a008759]: https://github.com/AbhyudayaSikri/roame-flight-explorer/commit/a008759
[6f91c42]: https://github.com/AbhyudayaSikri/roame-flight-explorer/commit/6f91c42
[a997530]: https://github.com/AbhyudayaSikri/roame-flight-explorer/commit/a997530
[run 41788560793]: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41788560793
[run 41792526988]: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41792526988
[run 41796168173]: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41796168173
[run 41799697121]: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41799697121
[run 41803662017]: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41803662017
[run 41850754688]: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41850754688
[run 41852391748]: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41852391748
[run 41853828307]: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41853828307
[run 41853912750]: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41853912750
[run 41869605322]: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41869605322
[run 41875688814]: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41875688814
[run 41881579267]: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41881579267
[run 41924340854]: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41924340854
[run 41924418166]: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41924418166
[run 41947873526]: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41947873526
[run 41951417332]: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41951417332
[run 41951444932]: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41951444932
[run 41955053011]: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41955053011
[run 41957059552]: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41957059552
[run 41957681937]: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41957681937
[run 41957685895]: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41957685895
[run 41958388873]: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41958388873
[run 41976342016]: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41976342016
[run 41980418799]: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41980418799
[run 42023738212]: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/42023738212
[run 42023837394]: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/42023837394
[run 42023928470]: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/42023928470
[run 42024450163]: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/42024450163
[run 42026005109]: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/42026005109
