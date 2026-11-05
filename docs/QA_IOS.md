# iOS QA results (CI simulator)

All results come from GitHub Actions on an iPhone 17 simulator (iOS 26.4, Xcode 26.4.1); iOS is never run locally (PLAN 8.4). The latest green run's `ios-ui-tests` artifact holds the `.xcresult` bundles, the exported screenshots and `demo.mp4`.

How each item was checked:
- **test:** an XCUITest asserts it. The class and method are named.
- **screenshot:** I looked at the named screenshot.
- **shared:** the shared Kotlin tests cover it on the simulator. The iOS UI doesn't drive it.
- **needs eyes:** only a person watching `demo.mp4` or using the app can judge it.

Last reviewed: 2026-11-04, against [run 41796168173](https://github.com/AbhyudayaSikri/roame-flight-explorer/actions/runs/41796168173) (commit `f3fe6ff`).

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
