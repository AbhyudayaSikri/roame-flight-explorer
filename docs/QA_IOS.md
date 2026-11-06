# iOS QA results (CI simulator)

All results come from GitHub Actions on an iPhone 17 simulator (iOS 26.4, Xcode 26.4.1); iOS is never run locally (PLAN 8.4). The latest green run's `ios-ui-tests` artifact holds the `.xcresult` bundles, the exported screenshots and `demo.mp4`.

How each item was checked:
- **test:** an XCUITest asserts it. The class and method are named.
- **screenshot:** I looked at the named screenshot.
- **shared:** the shared Kotlin tests cover it on the simulator. The iOS UI doesn't drive it.
- **needs eyes:** only a person watching `demo.mp4` or using the app can judge it.

Last reviewed: 2026-11-04, against [run 41796168173](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41796168173) (commit `f3fe6ff`). The walkthrough recording from a later run is reviewed at the end: "Shown in the CI walkthrough recording".

## Launch, search, filters

| QA | How | Result |
|---|---|---|
| L1 | tests `LaunchTests.testFreshInstallLoadsEveryFlightWithNoFiltersActive` and, for the loading state first, `testASlowAPIShowsTheLoadingStateUntilTheFlightsArrive`; screenshot "L1 launch" | pass |
| L2 | same test (no chips, "Show 1,000 flights"); screenshot "L2 default filters" shows Feb 1, 2027 – Apr 1, 2027 | pass |
| L3 | needs eyes (scrolling in `demo.mp4`) | not judged |
| S1–S5 | test `SearchTests` (113, 113, 110 with both Tokyo markers, 7, no-matches state with Reset) | pass |
| S6 | test `SearchTests.testAnExactCodeWinsOverCityPrefixes` (`dub` 29, `du` 69, `fra` 0 with the no-matches state, `fran` 123). Shared `SearchTest` covers the same rule. | pass |
| F1, F2 | Shared `FilterTest`, `PickerDateTest` (the picker's date conversion) and the store's single-day test. A dropped XCUITest's recording shows the picker giving "Show 17 flights" for Feb 1 before the test's own tap changed the cabin (NOTES §4). | covered by shared tests |
| F3 | test `FilterTests.testLowestPointsLimitLeavesTheSingleCheapestFlight` (slider at the minimum leaves LAX → MEX, 9,000 pts). The UI can't go below 9,000. | pass |
| F4 | test `FilterTests.testCabinCountsUpdateLiveAndShowAChip` (344, 467) | pass |
| F5, F10 | test `FilterTests.testProgramsCombineWithOrAndDeselectingRestoresAll` (386, back to 1,000; "Mileage Plan" and "MileagePlus" are both options) | pass |
| F6 | shared `FilterTest`; the slider can't be set to exactly 60,000 from a UI test | needs manual check |
| F7, F8 | tests `FilterTests.testCabinCountsUpdateLiveAndShowAChip` and `testResetClearsSearchAndEveryFilter` | pass |
| F9 | needs eyes | not judged |

## Map, selection, saved

| QA | How | Result |
|---|---|---|
| M1, M7 | test `MapSelectionTests.testMarkersShowCountAndLowestPointsPerAirport` ("HND 55 · 16.5k", "NRT 55 · 19.5k", 2 markers for 110 results); screenshot "M1 map, Tokyo" | pass |
| M2 | needs eyes | not judged |
| M3, M4 | test `MapSelectionTests.testMarkerSheetPicksAFlightAndShowsTheCard`; screenshots "M3 destination sheet", "M3 route and card" | pass |
| M5 | test `MapSelectionTests.testSelectionIsSharedBetweenListAndMap` | pass |
| M6 | test `MapSelectionTests.testFilteringOutTheSelectionClearsIt` | pass |
| M8 | screenshot "M5 M8 SFO to NRT route on the map": one arc over the North Pacific, framed above the card | pass |
| V1 | test `SavedTests.testSavedFlightShowsEverywhereIgnoresFiltersAndRemovesFromTheCard` (Saved tab and the list row's saved mark). The orange map marker for a saved destination isn't asserted. | pass (list) / map not checked |
| V2 | test `SavedTests.testSavingAgainAfterAPriceChangeUpdatesWithoutDuplicates` (`--mode=updated`); screenshot "V2 price changed" shows "Update saved" | pass |
| V3, V4 | tests `SavedTests.testSavedFlightShowsEverywhere…` and `testRemoveFromTheSavedTab` | pass |
| V5 | test `SavedTests.testSavedFlightSurvivesAnEmptyResponse` (`--mode=empty`, then refresh) | pass |
| V6 | tests `SavedTests.testFailedSaveRollsBackWithAMessage` and `testFailedRemoveRollsBackWithAMessage` (`-failSaves`, `-failRemoves`) | pass |

## Offline, errors, polish

| QA | How | Result |
|---|---|---|
| O1–O3 | test `OfflineRelaunchTests` with the API process stopped; screenshots "O2 relaunch offline" (stale banner, Saved badge), "O3 removed offline" | pass |
| O4 | test `ErrorStateTests.testNoCacheErrorOffersRetryThatLoadsOnceTheServerRecovers` (`--mode=error`, then `normal` and Retry); screenshot "O4 error with no cache" | pass |
| O5 | test `ErrorStateTests.testEmptyResponseOnAFreshInstallSaysNoFlightsAvailable`; screenshot "O5 empty response" | pass |
| O6 (SHOULD) | test `LaunchTests.testASlowAPIShowsTheLoadingStateUntilTheFlightsArrive` (`--mode=slow`); screenshot "O6 loading". The recorded demo also starts with a 3 s load. | pass |
| O7 | test `LaunchTests.testRotatingAndBackgroundingKeepsSearchAndSelection`. XCUITest's landscape screenshot comes out rotated and clipped, so the landscape layout itself isn't judged. | pass (state) |
| P1 | screenshots: the same row typography in the list, the destination sheet and Saved; 16 pt margins throughout | pass |
| P2 | Buttons use system styles with pressed states. The store ignores Save/Remove while one is in flight (shared tests). The card's progress state wasn't caught on screen, because local writes finish within a frame. | partly checked |

## Polish notes
- Fixed 2026-11-04: "Premium Economy" no longer truncates, because the cabin filter is a list of rows instead of a segmented control.
- Fixed 2026-11-04: pills no longer overlap at world zoom; nearby airports merge into one pill until you zoom in. Test: `MapSelectionTests.testTappingAMergedPillZoomsInUntilItsAirportsSeparate`, with screenshots "Map, merged pills at world zoom" and "Map, zoomed into a merged pill".

## Shown in the CI walkthrough recording

`2-ios-walkthrough-ci.mp4` (1:11) is `demo.mp4` from the full iOS run on `main` [42078549498](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/42078549498) (commit `b31cdae`), recorded on the CI simulator while `DemoWalkthrough` and then `OfflineRelaunchTests` ran.
- CI stops the API between the two tests.
- The home screen at the start and between the tests is cut, and two static pauses are shortened.
- It was re-encoded as H.264. Timestamps refer to this file.

What its frames show:

| QA | Shown in the CI walkthrough recording |
|---|---|
| L1, O6 | Placeholder rows during the slow first load (0:00–0:05), then 1,000 flights (0:06) |
| L2 | The filter sheet's dates, Feb 1, 2027 to Apr 1, 2027 (0:18) |
| S2 | From "new york" gives 113 (0:12) |
| S4 | From "new york" plus To "tokyo" gives 7 (0:28) |
| F7 | The sheet's "Show N flights" counts live: 7, then 2 with Business (0:24), then 7 again (0:26) |
| M1 | With the search's 3 flights, the HND pill reads "3 · 26.5k" (0:36–0:38). The default-filter values aren't shown. |
| M3 | The HND pill's sheet, "Tokyo (HND) · 3 flights" (0:40), then the JFK → HND route and card (0:44) |
| M4 | The card shows JFK → HND, "New York to Tokyo", Wed, Feb 10, 2027, Business, 70,500 pts, MileagePlus and Save (0:44) |
| M8, M9 | JFK → HND draws one continuous arc, framed above the card (0:44–0:52) |
| V1 | Save gives "Saved · Remove" and a Saved badge (0:50), and the Saved tab lists the flight (0:53) |
| O2 | Relaunched with the API stopped: the cached 1,000 flights under "Offline · showing results from 6:19 PM", with the Saved badge (0:54–0:56) |
| O3 | Offline, "jfk" gives 113 (1:06), and the Saved tab still lists the flight (1:08). Remove is tapped as the recording ends (1:10), so the removal itself isn't shown. |
