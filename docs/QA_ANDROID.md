# Android QA results

Each item in `QA_CHECKLIST.md` has one of these statuses:
- **Verified in the owner's recorded walkthrough:** the frames of `1-android-walkthrough.mp4` show the check. Timestamps refer to that file.
- **Partly shown in the owner's recorded walkthrough:** the frames show part of the check; the note says which part, and what covers the rest.
- **Automated: Android UI test `Class.method`:** a Compose UI test in `androidApp/src/androidTest` asserts it on every pull request, on an API 36 emulator in CI (`android-ui.yml`, NOTES §5).
- **Not manually verified:** neither of the above.

**The recording:** `1-android-walkthrough.mp4` (5:31), recorded by hand on 2026-11-05 on an Android Studio emulator ("Medium Phone API 37.0").
- The app reached the local API through `adb reverse` (`apiBaseUrl=http://localhost:8080`, NOTES §2.2).
- The build includes #46: the map frames a search's results.
- Its frames were read every 5 s, and every 1–2 s where the screen changes. An item is marked from what the frames show, not from the narration.

## API

| QA | Status | Note |
|---|---|---|
| A1 | automated: server tests in Linux CI (`:server:test`) | — |
| A2 | not manually verified | The recording doesn't show the API being started. |

## Launch and loading

| QA | Status | Note |
|---|---|---|
| L1 | verified in the owner's recorded walkthrough | The app opens from the home screen (1:00). Grey placeholder rows show (1:15–1:20), then "1,000 flights" (1:25). The recording doesn't show whether it was a fresh install. Also automated: `SmokeTest.launchLoadsAllFlightsWithNoFiltersActive`. |
| L2 | verified in the owner's recorded walkthrough | No chips at 1:25; the filter sheet's dates read "Feb 1 – Apr 1" (3:04). Also automated: `SmokeTest.launchLoadsAllFlightsWithNoFiltersActive`. |
| L3 | not manually verified | The list is scrolled a little (1:35–1:40), not flung end to end. |
| L4 | partly shown in the owner's recorded walkthrough | Grey placeholder rows in the list's shape (1:15–1:20), then the real rows (1:25). The API's mode isn't shown, and frames can't show a layout jump. |

## Search

| QA | Status | Note |
|---|---|---|
| S1 | automated: Android UI test `SearchTests.originIgnoresCaseAndSpacesMatchesTheCityAndCombinesWithTheDestination` | — |
| S2 | verified in the owner's recorded walkthrough | From "new" gives 113 flights (2:30), and "new york" 113 (2:40). Also automated: `SearchTests.originIgnoresCaseAndSpacesMatchesTheCityAndCombinesWithTheDestination`. |
| S3 | automated: Android UI test `SmokeTest.searchingTokyoFindsBothAirports` | The recording searches "tokyo" only together with From "new york". |
| S4 | verified in the owner's recorded walkthrough | From "new york" plus To "tokyo" gives 7 flights, to HND and NRT (2:45). The checklist uses "jfk"; "new york" matches the same 113 JFK flights. Also automated: `SearchTests.originIgnoresCaseAndSpacesMatchesTheCityAndCombinesWithTheDestination`. |
| S5 | automated: Android UI test `SearchTests.noMatchesIsAnEmptyStateWithResetNotAnError` | — |
| S6 | automated: Android UI test `SearchTests.anExactCodeWinsOverCityPrefixes` | — |

## Filters

| QA | Status | Note |
|---|---|---|
| F1, F2 | automated: Android UI test `FilterTests.aSingleDayRangeIncludesThatDay` | The dates aren't changed in the recording. |
| F3 | automated: Android UI test `FilterTests.theLowestPointsLimitLeavesTheSingleCheapestFlight` | The lowest limit in the recording is 30,500 (3:30). |
| F4 | partly shown in the owner's recorded walkthrough | Business alone gives 344 (3:12). Economy isn't shown. Automated: `FilterTests.cabinCountsUpdateLiveAndCombineWithAProgramAndAnExactLimit`. |
| F5 | partly shown in the owner's recorded walkthrough | With Business and a 30,500 limit, Aeroplan plus Flying Blue gives 5 of the 12 flights, under "Flights from any selected program" (3:30–3:35). Avios plus Aeroplan (386) isn't shown. Automated: `FilterTests.programsCombineWithOrAndDeselectingRestoresAll`. |
| F6 | automated: Android UI test `FilterTests.cabinCountsUpdateLiveAndCombineWithAProgramAndAnExactLimit` | The recording combines Business with a 60,000 limit (171, 3:25), without Avios. |
| F7 | verified in the owner's recorded walkthrough | Chips: "From: new york" and "To: tokyo" (2:45), "Business" (3:12), "Business" and "≤ 30,500 pts" (3:40). The sheet's button counts live: 344 (3:16), 338 (3:21), 171 (3:25), 12 (3:30), 5 (3:35). Also automated: `FilterTests.cabinCountsUpdateLiveAndCombineWithAProgramAndAnExactLimit`. |
| F8 | automated: Android UI test `FilterTests.resetClearsSearchAndEveryFilter` | Reset isn't tapped in the recording. |
| F9 | not manually verified | The slider is dragged (3:19–3:30), but frames can't show whether it follows the finger. |
| F10 | partly shown in the owner's recorded walkthrough | "Mileage Plan" and "MileagePlus" are separate options (3:04). Selecting and deselecting a program isn't shown. Automated: `FilterTests.programsCombineWithOrAndDeselectingRestoresAll`. |
| F11 | verified in the owner's recorded walkthrough | From "new york", To "tokyo" and Business give 2 (3:09). × on the From chip empties the From field and keeps the other two (38, 3:11); × on the To chip keeps Business (344, 3:12). Also automated: `FilterTests.eachChipRemovesOnlyItsOwnFilter`. |
| F12 | automated: Android UI test `FilterTests.minusAndPlusStepThePointsLimit` | The − and + buttons are visible (3:16–3:21), but not used. |

## List, map, selection

| QA | Status | Note |
|---|---|---|
| M1 | verified in the owner's recorded walkthrough | Pills show each airport's count and lowest points, and nearby airports merge, such as "3 airports 150 · 16.5k" (1:45). Tokyo shows two pills, HND "55 · 16.5k" and NRT "55 · 19.5k" (5:00). The 24 markers at default filters aren't counted. |
| M2 | not manually verified | The map is panned and zoomed (1:45–1:55, 4:45–5:00), but frames can't show smoothness. |
| M3 | verified in the owner's recorded walkthrough | The Lisbon pill opens its sheet, 33 flights (2:00), and picking YYZ → LIS draws the route and shows the card (2:05). The Tokyo (NRT) sheet appears at 4:55. Also automated: `SmokeTest.aFlightPickedFromAMapPillShowsItsCard`. |
| M4 | verified in the owner's recorded walkthrough | The card shows YYZ → LIS, "Toronto to Lisbon", Mon, Feb 8, 2027, Economy, 14,500 pts, MileagePlus and Save (2:05), then "Saved · Remove" (2:10). |
| M5 | automated: Android UI tests `SelectionTests.aFlightSelectedOnTheListStaysSelectedOnTheMapAndBack` and `SelectionTests.aFlightPickedOnTheMapStaysSelectedOnTheList` | The card is closed before the recording switches views (2:59). |
| M6 | automated: Android UI test `SelectionTests.filteringOutTheSelectionClearsItOnBothViews` | — |
| M7 | verified in the owner's recorded walkthrough | With 7 results, the pills read HND "3 · 26.5k" and NRT "4 · 25k" (2:50): 3 + 4 = 7. |
| M8 | verified in the owner's recorded walkthrough | JFK → NRT draws one continuous arc from Japan over the North Pacific to New York (2:57). |
| M9 | verified in the owner's recorded walkthrough | After selecting, the whole route is framed above the card: YYZ → LIS (2:05) and JFK → NRT (2:57). |
| M10 | verified in the owner's recorded walkthrough | On the Map (2:10), then the Saved tab (2:15), then back to Explore, still on the Map (2:20). |
| M11 | partly shown in the owner's recorded walkthrough | After From "new york" and To "tokyo" on the List (2:45), switching to Map shows Japan with HND and NRT as separate pills (2:50). The search isn't typed on the Map tab, and the gesture, route and no-match rules aren't shown. Automated: `MapTests.searchingTokyoFramesBothAirportsAsSeparatePills`. |

## Saved

| QA | Status | Note |
|---|---|---|
| V1 | verified in the owner's recorded walkthrough | Saving YYZ → LIS turns the button into "Saved · Remove", adds a Saved badge and turns its map pill orange (2:10). The Saved tab lists it (2:15). JFK → NRT, saved at 2:57, shows a heart on its list row (3:01). Also automated: `SmokeTest.aSavedFlightAppearsInSaved`. |
| V2 | automated: Android UI test `SavedTests.savingAgainAfterAPriceChangeUpdatesWithoutDuplicates` | — |
| V3 | not manually verified | The Saved tab isn't opened while a filter is active. |
| V4 | automated: Android UI test `SavedTests.removeWorksFromTheCardAndFromTheSavedTab` | The recording shows only a failed remove (5:23). |
| V5 | automated: Android UI test `SavedTests.aSavedFlightSurvivesAnEmptyResponse` | — |
| V6 | verified in the owner's recorded walkthrough | In ⋮, Fail saves is turned on (5:06–5:08). Saving YYZ → BCN shows "Couldn't save YYZ → BCN. Please try again.", and the button is Save again (5:13–5:20). A remove shows "Couldn't remove YYZ → LIS. Please try again.", and both flights stay in Saved (5:23–5:30). Also automated: `SavedTests.aFailedSaveRollsBackWithAMessage`, `SavedTests.aFailedRemoveRollsBackWithAMessage`. |

## Offline and errors

| QA | Status | Note |
|---|---|---|
| O1 | automated: Android UI tests `ServerStateTests.aFailedRefreshKeepsTheCacheAndMarksItStale` and `OfflineRelaunchTest.aRelaunchWithTheApiStoppedShowsTheCacheMarkedStaleAndTheSavedFlight` | The API isn't stopped in the recording. |
| O2 | automated: Android UI test `OfflineRelaunchTest.aRelaunchWithTheApiStoppedShowsTheCacheMarkedStaleAndTheSavedFlight` | The recording turns off the emulator's Wi-Fi and mobile data (3:56–4:00), force-quits the app from Recents (4:04) and reopens it (4:08). The flights come back (4:20), and both saved flights are still in Saved (4:36). But `adb reverse` doesn't use the emulator's network, so the app still reached the API: a refresh succeeded, and no offline banner appears (4:20–4:26). This isn't O2's API-stopped case. |
| O3 | automated: Android UI test `OfflineRelaunchTest.aRelaunchWithTheApiStoppedShowsTheCacheMarkedStaleAndTheSavedFlight` | — |
| O4 | automated: Android UI test `ServerStateTests.withNoCacheAnErrorOffersRetryThatLoadsOnceTheServerRecovers` | — |
| O5 | automated: Android UI test `ServerStateTests.anEmptyResponseOnAFreshInstallSaysNoFlightsAvailable` | — |
| O6 | not manually verified | — |
| O7 | automated: Android UI test `SelectionTests.rotatingAndBackgroundingKeepTheSearchAndTheSelection` | The recording doesn't rotate, or return to the app from Home. |
| O8 | partly shown in the owner's recorded walkthrough | With the emulator offline (no Wi-Fi or mobile data in the status bar, 4:26–5:00), the Map tab still draws its pills, over cached tiles (4:45–5:00). A selected route isn't shown offline. |
| O9 | not manually verified | — |

## Polish and tooling

| QA | Status | Note |
|---|---|---|
| P1, P2 | not manually verified | The list, card, sheets and Saved tab are all on screen, but the recording doesn't record a judgement on spacing or tap feedback. |
| P3, P4, P5 | not manually verified | The recording uses the light theme and the default font size, without TalkBack. |
| P6 | not manually verified | The app's icon is in the home screen's dock, which shows no labels. |
| P7 | not manually verified | Emulators have no haptics. |
| T1, T2, T4 | not manually verified | — |
| T3 | not manually verified | The ⋮ menu shows "Server URL…" (5:06), but the dialog isn't opened. |

## Seen in the recording, not a checklist item
- While the flights load, "0 flights" shows above the placeholder rows (1:15–1:20, 4:18). iOS hides the count until there's data.
