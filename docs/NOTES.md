# Submission notes (working draft)

Started 2026-11-03 during the planning pass. Sections 1, 3, 4, 5 and 6 are filled in as the work lands (PLAN §10, §12). Every assumption gets a date and a reason when it's made.

## 1. Approach and architecture
To be written at step 10. Planned:
- One shared `FlightExplorerStore` in KMP is the single source of truth. It does networking, persistence, search, filtering, map aggregation, selection, sync state, route geometry and display formatting.
- The SwiftUI and Compose apps only render `state` and send intents.
- Library choices and the reasons for them: PLAN §3.1.

## 2. Assumptions

### 2.1 Search rule (decided 2026-11-03)
The brief asks for origin and destination fields that "match airport codes or city names" and "ignore capitalization and surrounding whitespace". It doesn't say how partial input matches. The rule:
1. Normalize the text: trim, lowercase, and collapse runs of inner whitespace to a single space. Empty text means no constraint.
2. **Exact code first.** If the normalized text equals an airport code that appears in the data (32 codes), match only flights whose code for that field equals it.
3. **Otherwise, prefix match.** The airport code starts with the text, OR the city starts with it from any word boundary.

Why: in this fixture, three-letter codes appear inside other cities' names. Plain substring matching would silently mix airports.
- Destination `dub`: Dublin's code is DUB, but "Dubai" also starts with "dub". Substring or prefix matching returns 69 flights (DUB + DXB). This rule returns **29** (Dublin only).
- Origin `fra`: FRA is Frankfurt, which is only a destination in this data, but "San Francisco" contains "fra". Substring or word-prefix matching returns 123 SFO flights. This rule returns **0**. Typing on to `fran` returns San Francisco (123).
- Prefix matching also stops mid-word hits that substring matching allows: `ork` would match New York, `ao` Sao Paulo, and `an` LAX, SFO and YVR (374 flights). With this rule `ork` and `ao` match nothing, and `an` matches Los Angeles only (125).
- Unchanged: `"  jfk "` → 113, `new york` → 113, `new y` → 113, `york` → 113, `TOKYO` → 110 (HND + NRT), `tok` → 110.

Alternatives considered:
- Exact-code precedence + substring: keeps `ork` → JFK and `ao` → GRU.
- Code-prefix OR city word-prefix only: keeps `dub` → Dubai and `fra` → San Francisco.

13 codes appear inside a city name, forming 15 code/city pairs. Only DUB/Dubai and FRA/San Francisco point at a different airport; the rest are self-matches, such as MEL/Melbourne.

### 2.2 Other assumptions (2026-11-03 unless noted)
- **Cabin is single-select**, with an "any" option. The brief says programs allow "multiple selections" but doesn't say that about cabin.
- **Programs combine with OR**, and selecting none means all programs.
- **Cabin and program values match exactly.** "Economy" must not match "Premium Economy" (467 vs 656 flights), and "Mileage Plan" is a different program from "MileagePlus" (203 vs 199).
- **Flight IDs are opaque.** Six records (`f101`–`f106`) don't follow the `fx-ORIG-DEST-YYYYMMDD` pattern, so the code never parses ids.
- **Dates are calendar dates, with no time zones.** iOS converts `DatePicker` values through calendar components; Android converts `DateRangePicker`'s UTC-midnight milliseconds through shared helpers.
- **Default date range = the data's bounds** (Feb 1 – Apr 1, 2027). While the user hasn't changed it, it follows new data. A range equal to the bounds shows no chip.
- **Max points defaults to no limit.**
  - The slider moves in steps of 500, because every fixture price is a multiple of 500.
  - A slider at the data maximum means "no limit" and shows no chip.
- **Reset** clears both search fields and every filter. QA F8 expects 1,000 results afterwards.
- **Results sort** by departure date, then points, then id.
- **Filter options order (2026-11-04):** cabins run from cheapest to most premium (Economy, Premium Economy, Business, then First if it ever appears), with any unknown cabin names after them alphabetically. Programs are alphabetical.
- **Result count wording (2026-11-04):** "1 flight", otherwise a grouped plural such as "1,000 flights". Both apps use the shared `formatFlightCount`.
- **Pills that would overlap are merged (2026-11-04).** At world zoom, nearby destinations such as LHR and CDG, or HND and NRT (0.6° apart), would sit on top of each other.
  - The shared `clusterMarkers` merges any pills that would overlap at the current zoom into one, such as "6 airports  280 · 13k" (airports, then flights · lowest points). Tapping a merged pill zooms in until its airports separate.
  - The busiest destination in a group names it, so the pills are the same each time at a given zoom.
  - Each airport, HND and NRT included, still has its own marker once the map is zoomed in, as QA M1 and R15 require.
  - Pill sizes are fixed estimates: 104 × 24 pt on iOS and 130 × 28 dp on Android.
- **iOS UI-test plumbing (2026-11-04):**
  - The test target compiles in Swift 5 mode, and its test methods and helpers are `@MainActor`, matching `XCUIApplication`'s isolation.
  - Tests switch the server's mode with a raw TCP request (Network framework). That avoids App Transport Security in the test runner, which a plain-HTTP `URLSession` call would hit.
- **Camera fit on a selected route (2026-11-04):**
  - Android frames the route with the shared `fitCamera`, which uses Web Mercator and MapLibre's 512-dp world at zoom 0. It leaves out the card covering the bottom of the map. iOS gets the same result from MapKit (`MKGeodesicPolyline.boundingMapRect` plus a bottom safe-area inset).
  - Android re-frames only when the selected flight changes. iOS also re-frames the markers when the filters change and nothing is selected.
  - iOS also frames again when the map is resized (the keyboard closing, a rotation), because a frame chosen in a short map stays too zoomed out once the map is tall again.
- **Long routes near the pole (2026-11-04):** six routes, such as Toronto to Bangkok, pass within 4° of the North Pole. Their great-circle paths swing quickly in longitude there, which is real geometry rather than an antimeridian wrap. On a Web Mercator map they run along the top edge.
- **An empty successful response replaces the cache.** It is "the last successful result". Saved flights are unaffected.
- **Release packaging (2026-11-04).**
  - A `v*` tag publishes a GitHub Release (`release.yml`):
    - The Android **debug** APK and a **Debug** iOS simulator build, so testers get the debug menu's Server URL.
    - The server as one jar (fixture embedded, Java 17+).
    - `fallback.py` with its `flights.json`, and `QUICKSTART.md`.
  - The simulator `.app` is arm64 only, so it runs in simulators on Apple silicon Macs, not Intel ones.
  - The jar is a plain Gradle `Jar` with dependencies merged. No dependency shares a `META-INF/services` file, so keeping the first copy of duplicates is safe (checked), and no Shadow plugin is needed. CI runs the jar from an empty directory and compares `/flights` with the fixture.
- **The Python fallback server is built (2026-11-04)**, a COULD item pulled in because the release ships it. It uses only the standard library, with the same flags and endpoints as the Kotlin server, and 12 `unittest` tests that mirror `FlightsModuleTest`.
- **`--mode=slow` (2026-11-04)** serves the normal fixture after 3 s, on `/flights` only; `/health` still answers at once. That's long enough to see the loading state, and well inside the apps' 10 s request timeout.
- **Saved flights are snapshots**, updated only by an explicit save. When the cached flight differs from its saved snapshot, the card offers "Update saved", which upserts the snapshot with no duplicate.
  - The server's `--mode=updated` adds 500 points to every flight, so this can be exercised (QA V2).
- **Saved-tab selection** is independent of the discovery filters.
- **Decoding is strict.** One malformed record makes the whole response `Malformed`, and the cache is kept. Unknown JSON keys are ignored.
- **Map tiles need the network**, as the brief allows. Markers and the selected route should still render offline; the Android fallback style is a SHOULD.
- **Shared display formatting is English-only:** "16.5k", "16,500 pts", "Mon, Feb 1, 2027".
- **Android minSdk goes from 23 (starter) to 26.** kotlinx-datetime's JVM implementation uses `java.time`; the alternative would have been core-library desugaring.
- **The starter's `iosX64` target is kept.** It's Tier 3 in Kotlin 2.4 but not deprecated, and every chosen library still publishes it.
- **Each platform keeps its own local data.** There's no cross-device sync, per the brief.
- **iOS is verified only on the CI simulator** (iPhone 17, iOS 26.4, Xcode 26.4.1) by XCUITest and screenshots. It's never run interactively or on a device.
- **The repo is public during development**, so macOS CI minutes are free. Visibility is confirmed at submission (PLAN §12).

## 3. Tradeoffs
- **MapLibre with OpenFreeMap tiles on Android, not Google Maps.**
  - Gain: a reviewer needs no API key or billing account.
  - Cost: tiles come from a free community server, and the pills are drawn as bitmaps so they don't depend on the style's fonts.
- **SQLDelight, not Room.**
  - Gain: Room's bundled SQLite has no `iosX64` artifact, while SQLDelight's drivers are mature on both platforms.
  - Cost: the SQL is hand-written (`Flights.sq`), and Android's Room tooling isn't available.
- **SKIE, not a hand-written Swift bridge.**
  - Gain: Swift gets `for await` over the store's `StateFlow`, exhaustive `switch` over sealed types, and Swift enums, with no glue code.
  - Cost: a young plugin tied to the Kotlin version (0.10.16 for Kotlin 2.4.21), plus SKIE #208's stale generated files.
  - The fallback, not needed, was a small hand-written wrapper.
- **One shared store with one `StateFlow<ExplorerState>`, not a view model per screen in each app.**
  - Gain: selection, filters and saved state can't drift between the list, the map and the two platforms, and every rule is tested once.
  - Cost: every change re-derives the whole state. Filtering 1,000 flights takes far less than a frame, and it runs off the main thread.
- **Marker clustering and camera fitting in shared code, not each map SDK's own.**
  - Gain: both apps merge the same pills at the same zoom and frame routes the same way, and the rules have tests.
  - Cost: simple greedy clustering with fixed pill sizes, instead of the SDKs' animated clustering.
- **XcodeGen, not a hand-edited `project.pbxproj`.**
  - Gain: the project is reviewable YAML, and CI fails if the committed project drifts from it.
  - Cost: one more tool, needed only when files are added or removed.
- **Ktor CIO for the local API.**
  - Gain: one Gradle command starts it on any OS, it shares the client's HTTP stack, and its failure modes are tested.
  - Cost: it needs a JDK; the planned Python fallback wasn't built.
- **iOS verified on the CI simulator with XCUITest, not interactively.**
  - Gain: every iOS change is tested the same way, with screenshots and a demo video as evidence.
  - Cost: nothing was tried on a device, and smoothness is judged from recordings.
- **Strict decoding.**
  - Gain: one malformed record rejects the whole response and the cache is kept, so flights never silently disappear.
  - Cost: a single bad record blocks the whole refresh.

## 4. Known limitations
- **iOS has never run on a device or interactively.** It's checked only on the CI simulator (iPhone 17, iOS 26.4) by XCUITest, screenshots and the demo video.
- **Android was never run against the real API on the Mac.** It's checked by the shared tests, compilation and the Windows QA pass (issues labelled `qa`).
- **Map tiles need the network**, which the brief allows. The Android offline fallback style (SHOULD) isn't built.
- **Some QA items aren't covered by iOS UI tests:** F1 and F2 (single-day date ranges) and F6 (an exact 60,000-point limit). The shared `FilterTest` covers all three rules, and the iOS store uses it unchanged.
  - An F1 XCUITest through the date picker was tried on 2026-11-04 and dropped at its 45-minute timebox.
  - The calendar opened and February 1 was tapped, but the sheet then read "Show 10 flights" instead of 17. No single day has 10 flights, so the "tap outside to close the calendar" step most likely also changed another filter.
  - Finding the cause needs another 30-minute iOS CI run. The test is on the unmerged branch `test/ios-date-range-f1`.
- **Memory leaks aren't checked** on either platform. LeakCanary is a COULD.
- **Clustering uses one pill size**, so a merged pill, which is wider than a single airport's, can still touch a neighbour.
- **In portrait, the world map can't show every destination at once.** iOS frames the markers it can fit; Android starts at a fixed world view.
- **Android re-frames the camera only for a selected route.** iOS also re-frames the markers after a filter change.
- **English only:** shared formatting gives "16,500 pts" and "Mon, Feb 1, 2027" whatever the device language.
- **Not built (PLAN §13 SHOULD/COULD, cut under §0):**
  - Individually removable chips, −/+ points buttons, skeleton rows, haptics, "Flight Explorer" display names.
  - StrictMode, the Compose stability config, iOS sliced observation, Android `testTag`s, the Android offline map style.
  - Dark mode, Dynamic Type and screen-reader passes, LeakCanary, the Python fallback server.

## 5. Checks actually performed
Dates are local (UTC-5).

### Starter builds before implementation
- **2026-11-03, Mac: task list.** `./gradlew :shared:tasks --all` on the starter. Verification tasks: `allTests`, `check`, `iosSimulatorArm64Test`, `iosX64Test` (disabled on Apple Silicon), `androidConnectedCheck`, `androidDeviceCheck`. There's no host-test task until `withHostTest {}` is added.
- **2026-11-03, Mac: Android build.** After installing `platforms;android-37.0` and `build-tools;37.0.0`, `DEVELOPER_DIR=/var/empty ./gradlew :androidApp:assembleDebug` built the unmodified Android starter in 52 s. An empty `DEVELOPER_DIR` makes any Xcode tool call fail, so this also shows that the local Android build never uses Xcode.
- **2026-11-03, Linux CI** (`ubuntu-24.04`): `:androidApp:assembleDebug` on the unmodified starter succeeded. https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41774805083
- **2026-11-03, iOS CI** (`macos-26` image 20261026.0455, Xcode 26.4.1 build 17E202): `xcodebuild build` of the unmodified starter for an iPhone 17 simulator (iOS 26.4.1) succeeded. https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41774805004
  - Xcode ignores the starter's `RoameStarter.xcscheme.xml`, but `xcodebuild -list` auto-created a `RoameStarter` scheme, so `-scheme RoameStarter` worked.
- **Pending:** the Android starter on the Windows emulator (PLAN §1a).

### CI setup (PLAN §2b, 2026-11-03)
- **XcodeGen migration.** CI regenerated the project with XcodeGen 2.46.0 and found no drift, and the first UI test (`LaunchTests`) passed on the simulator.
  - ios: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41776284367
  - ci: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41776284332
- **SKIE spike: green on the first attempt, so no fallback was needed.** Stack: SKIE 0.10.16, Kotlin 2.4.21, Xcode 26.4.1, Swift 6.
  - `SkieSpike` exposes a `StateFlow<String>` and a `StateFlow` of the sealed `SpikeStatus`. SwiftUI consumes them with `for await` and `onEnum(of:)`.
  - The UI test checked the rendered values before and after a tap: "Hello world!" / "idle", then "Hello world! (ticks: 1)" / "ticked 1".
  - The build log has no SKIE errors and no Swift 6 concurrency warnings.
  - ios: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41777187615
  - ci: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41777187418
- **Does app data survive separate `xcodebuild test-without-building` runs on the same simulator? Yes.**
  - Run 1 reset a counter file in the app's Application Support directory and launched the app once.
  - Run 2, a separate xcodebuild invocation, read `launchCount=2`.
  - So the offline-relaunch tests run as their own invocation after CI stops the server (PLAN §8.1).
  - ios: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41778604099
  - ci: https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41778604081

## 6. How AI was used and verified
To be completed. So far:
- **Plan verification (2026-11-03):** an AI agent in Cursor (Claude Opus 5.5) did the following:
  - Checked every library version against Maven Central and Google Maven metadata, release notes, AAR metadata and the GitHub runner images (Appendix B).
  - Ran `./gradlew :shared:tasks --all`.
  - Analysed the fixture with scripts (Appendix A).
  - Proposed plan changes.
- **Decisions taken after reviewing that output:** MUST/SHOULD/COULD tiers with a MUST gate; XcodeGen; a public repo during development; the search rule in 2.1.
- **CI setup (2026-11-03):** the same agent wrote the two workflows, the XcodeGen spec, the SKIE spike and the persistence probe.
  - Each was accepted only after a green CI run.
  - The UI-test screenshots were reviewed before moving on (section 5).

## Appendix A. Fixture facts (`fixtures/flights.json`, analysed 2026-11-03)
- **Shape:**
  - The envelope is `{"flights": [...]}`, with 1,000 records.
  - Every record has the same 13 fields, with no nulls or blanks: `id`, `origin`, `destination`, `departureDate`, `program`, `points` (int), `cabin`, `originCity`, `destinationCity`, and origin/destination latitude/longitude (double).
- **Airports:**
  - 32 in total: 8 origins (BOS, JFK, LAX, ORD, SEA, SFO, YVR, YYZ) and 24 destinations, with no overlap between the two sets.
  - Each airport has one consistent city and coordinate pair.
  - Tokyo is the only multi-airport city: HND has 55 flights and NRT has 55.
  - City names are ASCII ("Sao Paulo").
- **Dates:**
  - 2027-02-01 to 2027-04-01, 60 days, with flights on every day (8–25 per day).
  - Feb 1 has 17 flights; Apr 1 has 21.
  - The array is sorted by date.
- **Points:**
  - 9,000–133,000, always a multiple of 500, with 183 distinct values.
  - The minimum is unique: `fx-LAX-MEX-20270218`. The maximum is `fx-BOS-MEL-20270401`, on the last day.
  - 491 prices aren't whole thousands, so marker labels need "16.5k".
- **Cabins:** Economy 467, Business 344, Premium Economy 189.
- **Programs:** Flying Blue 212, Mileage Plan 203, MileagePlus 199, Avios 196, Aeroplan 190.
- **Edge cases:**
  - Ids `f101`–`f106` break the `fx-` pattern, and list their fields in a different order. They are:
    - SFO→NRT Feb 12, Aeroplan, Business, 75,000.
    - JFK→CDG Feb 14, Flying Blue, Business, 60,000.
    - LAX→HND Feb 18, Mileage Plan, Business, 70,000.
    - SFO→LHR Mar 2, MileagePlus, Economy, 35,000.
    - YYZ→LIS Mar 6, Aeroplan, Economy, 35,000.
    - BOS→MAD Mar 10, Avios, Business, 40,500.
  - No duplicate ids, and no duplicate (origin, destination, date).
  - 68 of the 192 distinct routes have great circles that cross the antimeridian.
- **Expected counts used by the tests and QA:**
  - Origin JFK → 113; destination Tokyo → 110; JFK + Tokyo → 7.
  - Avios + Aeroplan → 386; Business + Avios + ≤ 60,000 → 32.
  - ≤ 132,999 points → 999.
  - Markers at defaults: 24, with HND "55 · 16.5k" and NRT "55 · 19.5k".
- **File:** 432,741 bytes, ASCII, LF line endings with a final newline, no `$` or `"""`. In git it has only the `text=auto` attribute.

## Appendix B. Verification log (2026-11-03)
- **Latest versions:**
  - Read from `maven-metadata.xml` on `repo1.maven.org` and `dl.google.com`.
  - Pre-releases were excluded by hand. For example, MapLibre's metadata `<release>` points at a `-globe` pre-release.
- **Target coverage:** artifact requests for every KMP dependency's android/JVM, `iosArm64`, `iosSimulatorArm64` and `iosX64` artifacts. All exist, except `androidx.sqlite:sqlite-bundled` for `iosX64` (that's the Room fallback).
- **Built-with versions:** read from `.module` metadata.
  - stdlib: serialization 2.4.10, Ktor 2.3.21, SQLDelight 2.3.10, coroutines 2.2.20, datetime 2.1.20.
  - The SQLDelight Gradle plugin depends on KGP 2.3.10 at runtime.
- **AAR metadata** (`aar-metadata.properties`): compose-ui 1.12.1 and lifecycle 2.11.0 need compileSdk 37 and AGP ≥ 9.1.0; MapLibre 13.7.1 needs minSdk 23.
- **Release notes and sources:**
  - GitHub releases for SKIE, SQLDelight, Ktor, kotlinx-serialization, kotlinx-coroutines, kotlinx-datetime, KSP, MapLibre Native, KMP-NativeCoroutines and XcodeGen.
  - SKIE commit `25bb2b9148` ("Add support for Kotlin 2.4.21 (#211)") for 2.4.21 support.
  - SKIE issues #168, #177, #179 and #208.
- **Compatibility:**
  - kotlinlang.org KMP compatibility guide: Kotlin 2.4.20–2.4.21 → Gradle 7.6.3–9.7.0, AGP 8.5.2–9.3.1, Xcode 26.4.
  - Kotlin/Native target tiers: `iosX64` is Tier 3 and not deprecated.
- **Task names:**
  - The Android KMP plugin docs give `withHostTest {}`, the `androidHostTest` source set and the `hostTest` compilation. androidx's `run_tests` skill and an AGP 9 migration commit give `testAndroidHostTest`.
  - The KGP v2.4.21 source (`KotlinNativeTest.kt`) shows the `--device` option on simulator tests.
- **Environment:**
  - `sdkmanager --list`: the valid package IDs are `platforms;android-37.0` and `build-tools;37.0.0`.
  - runner-images READMEs: `macos-26` (Xcode 26.4.1, iPhone 17 on iOS 26.4, JDK 25, Android 37.0); `macos-15` tops out at Xcode 26.3; `ubuntu-24.04`.
  - The OpenFreeMap liberty style URL returns 200.
  - SQLDelight's Gradle docs: a static framework needs `-lsqlite3`.
  - GitHub billing docs: macOS runners cost $0.062/min against a $12–18 monthly allowance on private repos, and are free on public repos.

## Appendix C. Known risks
- **SKIE 0.10.16 is days old**, with no changelog page. Resolved 2026-11-03: the §2b spike works at runtime in CI (section 5). The hand-written bridge remains the fallback if a later SKIE problem appears.
- **SKIE #208:** stale macro-expansion files under `shared/build/skie` can break iOS links until that directory is deleted.
- **App data across `xcodebuild test-without-building` runs.** Resolved 2026-11-03: the data container survives between runs on the same simulator (section 5).
- **MapLibre `LatLngBounds`** may not frame antimeridian-crossing routes. Fallback: centre on the path midpoint and pick the zoom from the distance.
- **Turbine 1.2.1** was built against coroutines 1.10.2. Fallback: `first { }` + `withTimeout`.
- **The toolchain is at the top of Kotlin's tested range.** A runner-image update could move Xcode 26.4.1, so CI selects it by path and prints `xcodebuild -version`.
- **iOS is checked only through CI**, so smoothness is judged from recordings.
- **Schedule:** about 22 h of work in two days. The cut order is in PLAN §0.
